(ns simple-static.components.figwheel
  (:require
   [mount.core :as mount]
   [figwheel.main.api :as figwheel]))

(mount/defstate
  figwheel
  :start
  (figwheel/start {:mode :serve :open-url false} :dev)
  :stop
  (figwheel/stop-all))
