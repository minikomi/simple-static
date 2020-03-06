(ns simple-static.build
  (:require
   [co.poyo.watch-and-run.file-map :as file-map]
   [clojure.java.io :as io]))

(defn build-all-pages []
  (->
   (file-map/load-file-map
    (io/resource "file-maps/pages.edn")
    {:base-path "target/public"})
   (file-map/run-all-jobs)))

(defn -main [& args]
  "Only for building pages."
  (build-all-pages))
