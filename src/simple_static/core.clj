(ns simple-static.core
  (:gen-class)
  (:require
   [simple-static.components.nrepl :as n]
   [simple-static.components.pagebuilder]
   [mount.core :as mount]

   [taoensso.timbre :as timbre]))

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
  (timbre/info (mount/start))
  (timbre/info "yo we started"))
