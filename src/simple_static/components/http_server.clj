(ns simple-static.server.components.http-server
  (:require [simple-static.components.handler :refer [handler]]
            [mount.core :refer [defstate]]
            [org.httpkit.server :as ohs]
            [taoensso.timbre :as timbre]))

;; mounting

(defn- stop-server! [server]
  (server :timeout 100))

(defstate server
  :start (do (timbre/info "Server started on port " (:port env 3000))
             (ohs/run-server #'wrapped-handler {:port (:port env 3000)}))
  :stop (stop-server! server))
