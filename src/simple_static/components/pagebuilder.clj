(ns simple-static.components.pagebuilder
  (:require [mount.core :as mount]
            [simple-static.components.helper.file-map :as file-map]
            [simple-static.components.watch-and-run :refer [watch-and-run add-jobs remove-jobs]]
            [simple-static.pages.hello :as hello]
            [simple-static.pages.top :as top]))

(def page-map
  {"index.html" {:template 'simple-static.pages.top/template
                 :data {:body-class "top"}}
   "hello" {"index.html" {:template 'simple-static.pages.hello/template
                          :data {:body-class "hello"}}}})

(mount/defstate
  simple-pages
  :start
  (let [jobs (file-map/file-map->jobs page-map)]
    (add-jobs jobs)
    jobs)
  :stop
  (remove-jobs simple-pages))
