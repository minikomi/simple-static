(ns simple-static.core
  (:gen-class)
  (:require
   [simple-static.components.nrepl :as n]
   [simple-static.components.pagebuilder]
   [simple-static.components.http-server]
   [simple-static.components.figwheel]
   [simple-static.components.config :as config]
   [mount.core :as mount]
   [taoensso.timbre :as timbre]
   [me.raynes.fs :as fs]))

(defn start! []
  (->
   (mount/except [#'n/nrepl])
   (mount/start)))

(defn stop! []
  (->
   (mount/except [#'n/nrepl])
   (mount/stop)))

(defn restart! []
  (stop!)
  (start!))

(comment
  (start!)
  (stop!)
  (restart!)
  )

(defn -main [& args]
  (timbre/info "Cleaning target dir")
  (fs/delete-dir (:target config/env "target/public"))
  (timbre/info "-={ MOUNTING }=-")
  (timbre/info (mount/start))
  )
