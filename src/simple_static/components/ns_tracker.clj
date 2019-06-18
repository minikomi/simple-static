(ns simple-static.components.ns-tracker
  (:require [mount.core :as mount]
            [simple-static.components.config :refer [env]]
            [ns-tracker.core :as ns-tracker]))

(mount/defstate tracker
  :start
  (ns-tracker/ns-tracker
   (:tracked-src env ["src" "data"])))
