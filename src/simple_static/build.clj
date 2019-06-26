(ns simple-static.build
  (:require
   [mount.core :as mount]
   [simple-static.components.pagebuilder]
   [simple-static.components.garden]
   [simple-static.components.config :as config]
   [simple-static.components.helper.file-map :as file-map]
   [me.raynes.fs :as fs]
   [figwheel.main.api :as figwheel]
   [clojure.java.io :as io]))


(defn -main [& args]
  (mount/start (mount/only #{#'config/env}))
  (when (fs/exists? (:target config/env))
    (fs/delete-dir (:target config/env)))
  (doseq [job (file-map/load-file-map "css" {:base-path "css"})]
    ((:build-fn job)))
  (doseq [job (file-map/load-file-map "pages")]
    ((:build-fn job)))
  (figwheel/start {:id "dev"
                   :mode :build-once
                   :options
                   {:main 'simple-static.frontend.core
                    :optimizations :advanced}})
  (fs/copy-dir (io/file (:static config/env) "images")
               (io/file (:target config/env) "images")))
