(ns simple-static.components.pagebuilder
  (:require [mount.core :as mount]
            [co.poyo.file-map :as file-map]
            [co.poyo.watch-and-run :as watch-and-run]
            [simple-static.pages.hello :as hello]
            [simple-static.pages.top :as top]
            [simple-static.components.config :as config]
            [clojure.java.io :as io]))

(mount/defstate
  simple-pages
  :start
  (let [jobs (file-map/load-file-map
              (io/resource "file-maps/pages.edn")
              {:base-path (:target config/env)})]
    (watch-and-run/add-jobs jobs)
    jobs)
  :stop
  (watch-and-run/remove-jobs simple-pages))
