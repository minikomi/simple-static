(ns simple-static.components.pagebuilder
  (:require [simple-static.components.helper.watch-and-run :as watch]
            [mount.core :as mount]
            [simple-static.pages.top :as top]
            [simple-static.pages.hello :as hello]
            [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]))

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

(defn parse-out-map
  ([out-map]
   (parse-out-map out-map {:extra-namespaces #{} :base-path ""}))
  ([out-map {:keys [extra-namespaces base-path]}]
   (let [acc (volatile! {:namespaces (or extra-namespaces #{})
                         :path->data {}})]
     (doall
      (tree-seq
       (fn [node]
         (cond
           (:template node)
           (do
             (vswap! acc update :namespaces
                     conj (name (ns-name (:ns (meta (resolve (:template node)))))))
             (vswap! acc update :path->data
                     assoc (str base-path
                            (str/join "/" (:path node))) (dissoc node :path))
             false)
           (map? node) true
           :else false))
       (fn [node]
         (for [[p n] node
               :when (not= :path p)]
           (assoc n :path ((fnil conj []) (:path node) p))))
       out-map))
     @acc)))

(let [{:keys [namespaces]} (parse-out-map out-map {:extra-namespaces #{'simple-static.pages.layout}})]
 (mount/defstate
   simple-pages
   :start
   (watch/start-watcher
    :simple-pages
    {:namespaces namespaces
     :handler (fn watching-simple [ctx e]
                (when (= (:kind e) :modify)
                  (when-let* [ns-path (watch/select-ns-path namespaces (str (:file e)))
                              ns (watch/file->ns namespaces ns-path)]
                    (timbre/info (prn-str ns))
                    (require (symbol ns) :reload-all)
                    ))
                ctx)})
   :stop
   (when simple-pages
     (watch/stop-watcher :simple-pages))))
