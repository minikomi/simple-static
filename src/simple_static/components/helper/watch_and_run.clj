(ns simple-static.components.helper.watch-and-run
  (:require [clojure.java.io :as io]
            [clojure.java.classpath :as cp]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [taoensso.timbre :as timbre]
            )
  (:import java.io.File
           java.nio.file.Paths
           java.nio.file.Files)
  )

(defonce watchers (atom {}))

;;
;;  namespaces -> css is easy
;;  hmm.
;;  templates change -> rebuild page
;;  data changes -> rebuild change.
;;    pages can be based on data, more than one page based on same template..
;;
;;  [files / ns] -> handler

(defn- ns-file-name
  "Copied from clojure.tools.namespace.move because it's private there."
  [sym]
  (str (-> (name sym)
           (str/replace "-" "_")
           (str/replace "." File/separator))
       ".clj"))

(defn- file-on-classpath
  "Given a relative path to a source file, find it on the classpath, returning a
fully qualified java.io.File "
  [path]
  (->> (cp/classpath)
       (map #(io/file % path))
       (filter #(.exists %))
       first))

(defn- select-ns-path
  "Given a list of namespace names (symbols) and a path (string), transforms the
path so it's relative to the classpath"
  [namespaces file]
  (let [ns-paths (map ns-file-name namespaces)]
    (first (filter #(.endsWith file %) ns-paths))))

(defn- file->ns
  "Given a list of namespace names (symbols) and a path (string), return the
namespace name that corresponds with the path name"
  [namespaces path]
  (first (filter #(.endsWith path (ns-file-name %)) namespaces)))

(defn stop-watcher [watcher-id]
  (if-let [w (get @watchers watcher-id)]
    (do
      (hawk/stop! w)
      (swap! watchers dissoc watcher-id)
      (timbre/info "Watcher stopped: " (name watcher-id)))
      (timbre/info "Not running: " (name watcher-id))))

(defn start-watcher [watcher-id
                     {:keys [namespaces
                             resources
                             handler]}]

  (stop-watcher watcher-id)
  (let [ns-paths  (map (comp file-on-classpath ns-file-name) namespaces)
        res-paths (map (comp (fn [f] (.path f)) io/resource) resources)
        paths     (concat ns-paths res-paths)]
    (timbre/info "Watcher started: " (name watcher-id))
    (swap! watchers assoc watcher-id
           (hawk/watch! [{:paths   paths
                          :handler handler}]))))

(comment
  (start-watcher :a
                 {:namespaces #{'simple-static.pages.top}
                  :handler (fn watching-top [ctx e]
                             (println "event: " e)
                             (println "context: " ctx)
                             ctx)
                  })
  (stop-watcher :a)

  )
