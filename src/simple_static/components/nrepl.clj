(ns simple-static.components.nrepl
  (:require [mount.core :refer [defstate] :as mount]
            [taoensso.timbre :as timbre]
            [nrepl.server :as nrepl-server]
            [simple-static.components.config :refer [env]]
            [refactor-nrepl.middleware :as refactor]
            ))

(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(mount/defstate ^{:on-reload :noop}
  nrepl
  :start
  (let [nrepl-port (Integer. (env :nrepl-port "9091"))
        nrepl-bind (env :nrepl-bind "0.0.0.0")]
    (require 'cider.nrepl)
    (ns-resolve 'cider.nrepl 'cider-nrepl-handler)
    (timbre/info (str "Starting nrepl on port " nrepl-port))
    (spit ".nrepl-port" nrepl-port)
    (nrepl-server/start-server
     :port nrepl-port
     :init-ns 'sekistone.server.repl
     :handler (-> (nrepl-handler) refactor/wrap-refactor)
     :bind nrepl-bind))
  :stop
  (when nrepl
    (nrepl-server/stop-server nrepl)))
