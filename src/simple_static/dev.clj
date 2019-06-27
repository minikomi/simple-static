(ns simple-static.dev
  (:gen-class)
  (:require
   [simple-static.components.nrepl :as n]
   [simple-static.components.pagebuilder]
   [simple-static.components.garden]
   [simple-static.components.http-server]
   [simple-static.components.figwheel]
   [simple-static.components.config :as config]
   [clojure.pprint :as pprint]
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
  (restart!))

(defn -main [& args]
  (timbre/info "Cleaning target dir")
  (mount/start (mount/only #{#'config/env}))
  (println (:target config/env))
  (when (fs/exists? (:target config/env))
    (fs/delete-dir (:target config/env)))
  (fs/mkdir (:target config/env))
  (timbre/info "-={ MOUNTING }=-")
  (timbre/info
   (with-out-str (pprint/pprint
                  (mount/start)))))
