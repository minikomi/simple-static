(ns simple-static.components.config
  (:require [mount.core :refer [defstate]]
            [config.core :refer [load-env]]))

(defstate env
  :start (load-env))
