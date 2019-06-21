(ns simple-static.components.watch-and-run
  (:require [clojure.java.io :as io]
            [clojure.java.classpath :as cp]
            [clojure.string :as str]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.file :as ns-file]
            [clojure.tools.namespace.track :as ns-track]
            [clojure.tools.namespace.dependency :as ns-dep]
            [clojure.set :as set]
            [hawk.core :as hawk]
            [taoensso.timbre :as timbre]
            [simple-static.components.config :refer [env]]
            [simple-static.components.ns-tracker :refer [tracker]]
            [clojure.pprint :as pprint]
            [mount.core :as mount])
  (:import java.io.File
           java.nio.file.Paths
           java.nio.file.Files))

(defn get-dep-graph [src-paths]
  (let [src-files
        (apply set/union
               (map (comp #(ns-find/find-clojure-sources-in-dir %)
                          io/file)
                    src-paths))
        tracker (ns-file/add-files {} src-files)
        dep-graph (tracker ::ns-track/deps)]
    dep-graph))

(defn all-nested-deps [watched ns-sym]
  (let [tracked-src (:tracked-src env ["src" "data"])
        all-dependencies (:dependencies (get-dep-graph tracked-src))
        watched-ns-syms (set (map :ns (vals @watched)))
        ns-names (set (ns-find/find-namespaces (map io/file tracked-src)))
       part-of-project? (partial contains? ns-names)]
    (set (tree-seq identity
               #(filter
                 part-of-project?
                 (get all-dependencies %))
               ns-sym))))

(defn ns-file-name
  "Copied from clojure.tools.namespace.move because it's private there."
  [sym]
  (str (-> (name sym)
           (str/replace "-" "_")
           (str/replace "." File/separator))
       ".clj"))

(defn file-on-classpath
  "Given a relative path to a source file, find it on the classpath, returning a
fully qualified java.io.File "
  [path]
  (->> (cp/classpath)
       (map #(io/file % path))
       (filter #(.exists %))
       first))

(defn select-ns-path
  "Given a list of namespace names (symbols) and a path (string), transforms the
path so it's relative to the classpath"
  [namespaces file]
  (let [ns-paths (map ns-file-name namespaces)]
    (first (filter #(.endsWith file %) ns-paths))))

(defn file->ns
  "Given a list of namespace names (symbols) and a path (string), return the
namespace name that corresponds with the path name"
  [namespaces path]
  (first (filter #(.endsWith path (ns-file-name %)) namespaces)))

(defn rebuild [target [out-file {:keys [data builder ns-sym]}]]
  (let [out-file (io/file target out-file)]
    (timbre/info "BUILD [" (.getPath out-file) "] with" ns-sym)
    (io/make-parents out-file)
    (spit
     out-file
     ((resolve builder) data))))

(defn watch-handler [{:keys [watched target]}]
  (let [changed (set (tracker))
        reloaded (volatile! #{})]
    (doseq [job @watched
            :let [ns-sym (:ns-sym (second job))
                  nested-deps (all-nested-deps watched ns-sym)
                  intersection (set/intersection nested-deps changed)]
            :when (not-empty intersection)]
      ;; reload changed namespaces
      (doseq [reload-ns-sym intersection
              :when (not (contains? @reloaded reload-ns-sym))]
        (timbre/info "Reloading:" reload-ns-sym)
        (require reload-ns-sym :reload)
        (vswap! reloaded conj reload-ns-sym))
      ;; run job
      (rebuild target job))))

(mount/defstate watch-and-run
  :start
  (let [watched (atom {})
        target (:target env "target")]
    {:watcher (hawk/watch!
               [{:paths (:tracked-src env ["src" "data"])
                 :handler (fn [ctx ev]
                            (watch-handler {:watched watched
                                            :target target}))}])
     :target target
     :watched watched
     :add-jobs
     (fn add-jobs [jobmap]
       (swap! watched into jobmap)
       (doseq [job jobmap]
         (rebuild target job)))
     :remove-jobs
     (fn remove-jobs [jobmap]
       (doseq [v (keys jobmap)]
         (swap! watched dissoc v)))})
  :stop
  (hawk/stop! (:watcher watch-and-run)))
