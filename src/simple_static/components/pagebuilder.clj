(ns simple-static.components.pagebuilder
  (:require [mount.core :as mount]
            [simple-static.components.helper.file-map :as file-map]
            [simple-static.components.watch-and-run :refer [watch-and-run add-jobs remove-jobs]]
            [simple-static.pages.hello :as hello]
            [simple-static.pages.top :as top]
            [clojure.java.io :as io]))

(mount/defstate
  simple-pages
  :start
  (let [jobs (file-map/load-file-map "pages")]
    (add-jobs jobs)
    jobs)
  :stop
  (remove-jobs simple-pages))
