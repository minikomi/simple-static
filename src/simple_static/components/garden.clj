(ns simple-static.components.garden
  (:require [garden.core :as garden]
            [mount.core :as mount]
            [simple-static.styles.core :as styles]
            [simple-static.components.watch-and-run
             :refer [watch-and-run add-jobs remove-jobs]]
            [clojure.java.io :as io]
            [simple-static.components.helper.file-map :as file-map]))

(mount/defstate
  garden
  :start
  (let [jobs (file-map/load-file-map "css" {:base-path "css"})]
    (add-jobs jobs)
    jobs)
  :stop
  (remove-jobs garden))
