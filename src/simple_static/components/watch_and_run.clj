(ns simple-static.components.watch-and-run
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.tools.namespace.file :as ns-file]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.track :as ns-track]
            [hawk.core :as hawk]
            [mount.core :as mount]
            [simple-static.components.config :refer [env]]
            [simple-static.components.ns-tracker :refer [tracker]]
            [taoensso.timbre :as timbre]))

(defn get-dep-graph [src-paths]
  (let [src-files
        (apply set/union
               (map (comp #(ns-find/find-clojure-sources-in-dir %)
                          io/file)
                    src-paths))
        tracker (ns-file/add-files {} src-files)
        dep-graph (tracker ::ns-track/deps)]
    dep-graph))

(defn all-local-deps-deep [ns-sym]
  (let [tracked-src (:tracked-src env ["src" "data"])
        all-dependencies (:dependencies (get-dep-graph tracked-src))
        ns-names (set (ns-find/find-namespaces (map io/file tracked-src)))
        part-of-project? (partial contains? ns-names)]
    (set (tree-seq identity
                   #(filter
                     part-of-project?
                     (get all-dependencies %))
                   ns-sym))))

(defn watch-handler [watched]
  (let [changed (set (tracker))
        reloaded (volatile! #{})]
    (doseq [{:keys [build-fn ns-sym]} @watched
            :let [deps (all-local-deps-deep ns-sym)
                  intersection (set/intersection deps changed)]
            :when (not-empty intersection)]
      ;; reload changed namespaces
      (doseq [reload-ns-sym intersection
              :when (not (contains? @reloaded reload-ns-sym))]
        (timbre/info "Reloading:" reload-ns-sym)
        (require reload-ns-sym :reload)
        (vswap! reloaded conj reload-ns-sym))
      (build-fn))))

(mount/defstate watch-and-run
  :start
  (let [watched (atom #{})
        watcher (hawk/watch!
                 [{:paths (:tracked-src env ["src" "data"])
                   :handler (fn [ctx ev] (watch-handler watched))}])]
    {:watcher watcher :watched watched})
  :stop
  (hawk/stop! (:watcher watch-and-run)))

(defn add-jobs [jobs]
  (if-let [watched (:watched watch-and-run)]
    (do (swap! watched into jobs)
        (doseq [{:keys [build-fn]} jobs]
          (build-fn)))
    (timbre/warn "Watcher not running.")))

(defn remove-jobs [jobs]
  (if-let [watched (:watched watch-and-run)]
    (doseq [job jobs]
      (swap! watched disj job))
    (timbre/warn "Watcher not running.")))
