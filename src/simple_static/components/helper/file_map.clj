(ns simple-static.components.helper.file-map
  (:require [me.raynes.fs :as fs]
            [taoensso.timbre :as timbre]
            [simple-static.components.config :refer [env]]
            [clojure.java.io :as io]))

(defn spit-txt [base-path {:keys [template path data]}]
  (let [out-file
        (apply fs/file
               (:target env "target/public")
               base-path
               path)]
    (timbre/info "\nBUILD [" (.getPath out-file) "] with" template)
    (io/make-parents out-file)
    (spit
     out-file
     ((resolve template) data))))

(defn sym->ns-sym [sym]
  (-> (resolve sym)
      meta
      :ns
      ns-name
      name
      symbol))

(defn file-map->jobs
  ([file-map]
   (file-map->jobs file-map {:extra-namespaces #{} :base-path ""}))
  ([file-map {:keys [base-path]}]
   (let [acc (volatile! #{})]
     (doall
      (tree-seq
       (fn file-map->jobs-br? [node]
         (if (:template node)
           (let [ns-sym (sym->ns-sym (:template node))]
             (vswap! acc conj
                     {:ns-sym ns-sym
                      :build-fn (fn [] (spit-txt base-path node))})
             false)
           (map? node)))
       (fn file-map->jobs-children [node]
         (for [[p n] node
               :when (and
                      (not= :path p))]
           (assoc n :path ((fnil conj []) (:path node) p))))
       file-map))
     @acc)))
