(ns simple-static.core
  (:gen-class)
  (:require
   [simple-static.components.nrepl]
   [mount.core :as mount]
   ))

(defn -main [& args]
  (mount/start)
  (println "yo"))
