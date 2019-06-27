(ns simple-static.components.helper.file-map
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]))

(defn spit-txt [base-path {:keys [template path data]}]
  (let [out-file
        (apply fs/file
               base-path
               path)]
    (timbre/infof "BUILD [%s]->[%s]"
                  template
                  (str/join "/" path))
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

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch java.io.IOException e
      (timbre/errorf "Couldn't open '%s': %s" source (.getMessage e)))
    (catch RuntimeException e
      (timbre/errorf "Error parsing edn file '%s': %s" source (.getMessage e)))))

(defn load-file-map
  ([file-map-source]
   (load-file-map file-map-name {:extra-namespaces #{} :base-path ""}))
  ([file-map-source {:keys [base-path]}]
   (try
     (let [file-map (read-edn file-map-source)
           acc (volatile! #{})]
       (doall
        (tree-seq
         (fn file-map->jobs-br? [node]
           (if (:template node)
             (let [ns-sym (sym->ns-sym (:template node))]
               (vswap! acc conj
                       {:base-path base-path
                        :node node
                        :ns-sym ns-sym
                        :build-fn (fn [] (spit-txt base-path node))})
               false)
             (map? node)))
         (fn file-map->jobs-children [node]
           (for [[p n] node
                 :when (and (not= :path p))]
             (assoc n :path ((fnil conj []) (:path node) p))))
         file-map))
       @acc)
     (catch Exception e
       (timbre/errorf "Error loading file map [%s]" file-map-source)))))
