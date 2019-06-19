(ns simple-static.components.helper.watch-and-run
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
           java.nio.file.Files)
  )

(def watched (atom
              {"index.html"
               {:path "index.html",
                :ns 'simple-static.pages.top,
                :data {:body-class "top"},
                :template pprint/pprint},
               "hello/index.html"
               {:path "hello/index.html",
                :ns 'simple-static.pages.hello,
                :data {:body-class "hello"},
                :template pprint/pprint}}
              ))

(defn all-nested-deps [ns-sym]
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

(all-nested-deps
 'simple-static.pages.hello)

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

(defn watch-handler [ctx ev]
  (let [watched-ns-syms (set (map :ns (vals @watched)))
        changed (set (tracker))]
    (doseq [ns-sym watched-ns-syms
            :let [nested-deps (all-nested-deps ns-sym)
                  intersection (set/intersection nested-deps changed)]
            :when (not-empty intersection)]
      (timbre/info "intersection:\n" ns-sym "\n---- " (set/intersection nested-deps changed))
      ))
  ctx)

(mount/defstate watch-and-run
  :start
  (hawk/watch! [{:paths   (into
                           (:tracked-src env ["src" "data"]))
                 :handler watch-handler}])
  :stop
  (hawk/stop! watch-and-run))
