(ns simple-static.components.garden
  (:require [garden.core :as garden]
            [mount.core :as mount]
            [simple-static.components.config :as config]
            [simple-static.styles.core :as styles]
            [co.poyo.watch-and-run
             :as watch-and-run]
            [co.poyo.file-map :as file-map]
            [clojure.java.io :as io]
            ))

(mount/defstate
  garden
  :start
  (let [jobs (file-map/load-file-map
              (io/resource "file-maps/css.edn")
              {:base-path (io/file (:target config/env) "css")})]
    (watch-and-run/add-jobs jobs)
    jobs)
  :stop
  (watch-and-run/remove-jobs garden))
