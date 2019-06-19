(ns simple-static.components.pagebuilder
  (:require [simple-static.components.watch-and-run :as watch-and-run]
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
  {"index.html" {:template 'top/template
                 :data {:body-class "top"}}
   "hello" {"index.html" {:template 'hello/template
                          :data {:body-class "hello"}}}})

(defn sym->ns-sym [sym]
  (-> (ns-resolve 'simple-static.components.pagebuilder sym)
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
         (if (:template node)
           (let [ns-sym (sym->ns-sym (:template node))]
             (vswap! acc assoc (str base-path (str/join "/" (:path node)))
                     {:path (str base-path (str/join "/" (:path node)))
                      :ns ns-sym
                      :data (:data node)
                      :template (:template node)})
             false)
           (map? node)))
       (fn parse-out-map-children [node]
         (for [[p n] node
               :when (not= :path p)]
           (assoc n :path ((fnil conj []) (:path node) p))))
       out-map))
     @acc)))

(mount/defstate
  simple-pages
  :start
  (let [out-map (parse-out-map out-map)]
    (swap!
     (:watched watch-and-run/watch-and-run)
     into out-map)
    out-map)
  :stop
  (doseq [k (keys simple-pages)]
    (swap! (:watched watch-and-run/watch-and-run)
           dissoc k)))

(comment
 (watch-and-run/start-watcher
    :simple-pages
    {:namespaces namespaces
     :handler
     (fn watching-simple [ctx e]
       (println (ns-tracker/tracker))
       (when (= (:kind e) :modify)
         (when-let* [ns-path (watch-and-run/select-ns-path namespaces (str (:file e)))
                     ns (watch-and-run/file->ns namespaces ns-path)]
           (timbre/info (prn-str ns))
           (when (namespaces ns)
             (require ns :reload-all))
           ))
       ctx)}))
