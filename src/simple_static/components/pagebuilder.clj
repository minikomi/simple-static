(ns simple-static.components.pagebuilder
  (:require [simple-static.components.watch-and-run :refer [watch-and-run]]
            [simple-static.components.ns-tracker :as ns-tracker]
            [mount.core :as mount]
            [simple-static.pages.top :as top]
            [simple-static.pages.hello :as hello]
            [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]
            [clojure.tools.namespace.file :as ns-file]))

(defmacro when-let*
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(def simple-namespaces
  #{'simple-static.pages.top
    'simple-static.pages.hello
    'simple-static.pages.layout})

(def out-map
  {"index.html" {:builder 'simple-static.pages.top/template
                 :data {:body-class "top"}}
   "hello" {"index.html" {:builder 'simple-static.pages.hello/template
                          :data {:body-class "hello"}}}})

(defn sym->ns-sym [sym]
  (-> (resolve sym)
      meta
      :ns
      ns-name
      name
      symbol))

(defn parse-out-map
  ([out-map]
   (parse-out-map out-map {:extra-namespaces #{} :base-path ""}))
  ([out-map {:keys [base-path]}]
   (let [acc (volatile! {})]
     (doall
      (tree-seq
       (fn parse-out-map-br? [node]
         (if (:builder node)
           (let [ns-sym (sym->ns-sym (:builder node))]
             (vswap! acc assoc (str base-path (str/join "/" (:path node)))
                     {:path (str base-path (str/join "/" (:path node)))
                      :ns-sym ns-sym
                      :data (:data node)
                      :builder (:builder node)})
             false)
           (map? node)))
       (fn parse-out-map-children [node]
         (for [[p n] node
               :when (and
                      (not= :path p))]
           (assoc n :path ((fnil conj []) (:path node) p))))
       out-map))
     @acc)))

(mount/defstate
  simple-pages
  :start
  (let [out-map (parse-out-map out-map)]
    (println "adding jobs")
    ((:add-jobs watch-and-run) out-map)
    out-map)
  :stop
  ((:remove-jobs watch-and-run) simple-pages))
