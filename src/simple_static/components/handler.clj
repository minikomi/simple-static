(ns simple-static.server.components.handler
  (:require [sekistone.server.components.queries :refer [db]]
            [sekistone.server.handlers.admin :as admin]
            [sekistone.server.handlers.login :as login]
            [sekistone.server.handlers.stone :as stone]
            [sekistone.shared.route-map :as r]
            [clj-time.coerce :as coerce]
            [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [environ.core :refer [env]]
            [mount.core :refer [defstate]]
            [prone.middleware :as prone]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.default-charset :refer [wrap-default-charset]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.http-response :as http]
            [ring.util.response :as res]
            [taoensso.timbre :as timbre]
            [ring.util.http-response :as h-resp]
            [sekistone.server.pages.layout :as layout]
            [sekistone.shared.route-map :as route]
            [sekistone.server.handlers.api :as api]
            [sekistone.server.handlers.admin-api :as admin-api]
            [sekistone.server.handlers.search :as search]
            [sekistone.server.handlers.case-study :as case-study]))

;; generic handlers

(defn !404-handler [kw]
  (fn [req]
    (-> (http/ok (str
                  "<h2>The requested page has no handler. [" kw "]</h2>"
                  "<pre>" (with-out-str (clojure.pprint/pprint req)) "</pre>"))
        (res/content-type "text/html")
        (res/status 404))))

;; middleware

(defn wrap-not-found [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (and resp (not= 404 (:status resp)))
        resp
        (let [bidi-kw (bidi/match-route r/route-map (:uri req))]
          (timbre/info "Not Found" bidi-kw)
          (-> (http/ok
               (str "<h2>The requested page does not exist.</h2>"
                    (when (:debug env)
                      (str
                       "<pre>"
                       (with-out-str
                         (clojure.pprint/pprint
                          (assoc req :bidi
                                 bidi-kw)))
                       "</pre>"))))
              (res/content-type "text/html")
              (res/status 404)))))))

(def debug-middleware
  (fn [req]
    (prone/wrap-exceptions req {:app-namespaces ['sekistone]})))

(defn wrap-index-for-folders [handler]
  (fn [req]
    (let [new-req (update-in req [:uri]
                             #(if (io/resource
                                   (str "public" % "/index.html"))
                                (str % "/index.html")
                                %))
          response (handler new-req)]
      (if
       (and (= 200 (:status response))
            (= java.io.File (type (:body response)))
            (re-find #"index.html$" (.getName (:body response))))
        (assoc-in response [:headers "Content-Type"] "text/html")
        response))))

;; trailing slash

(defn wrap-remove-slash [handler]
  (fn [{:keys [uri] :as req}]
    (if (and (not= uri "/")
             (.endsWith uri "/"))
      (res/redirect (subs uri 0 (- (count uri) 1)))
      (handler req))))

;; dev

(defn create-mode-defaults [debug]
  (if debug
    (assoc-in site-defaults [:session :store]
              (cookie/cookie-store {:key "DEBUG DEBUG DEBU"}))
    site-defaults))

(defn create-mode-middleware [debug]
  (if debug
    (fn [req]
      (-> req
          (prone/wrap-exceptions {:app-namespaces ['sekistone]})))
    identity))

(defn dispatch-route [kw]
  (timbre/info "routing" kw)
  (or
   ((some-fn
     admin/handlers
     stone/handlers
     login/handlers
     admin-api/handlers
     api/handlers
     search/handlers
     case-study/handlers)
    kw)
   (!404-handler kw)))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   #(-> % coerce/to-date .getTime)
   #(-> % coerce/to-date .getTime .toString)))

(def transit-opts
  {:handlers {org.joda.time.DateTime
              joda-time-writer}})

(defn with-prefix-handler [handler]
  (fn [req]
    (clojure.pprint/pprint req)
    (handler req)))

(defn wrap-handler []
  (let [mode-middleware (create-mode-middleware (:debug env))
        mode-defaults (create-mode-defaults (:debug env))]
    (-> (br/make-handler r/route-map dispatch-route)
        (wrap-not-found)
        (wrap-defaults mode-defaults)
        (wrap-resource "public")
        (mode-middleware)
        (wrap-file (io/file (env :dir-static)))
        (wrap-remove-slash)
        (wrap-restful-format {:response-options
                              {:transit-json transit-opts
                               :transit-messagepack transit-opts}})
        (wrap-content-type)
        (wrap-index-for-folders)
        (wrap-default-charset "utf-8"))))

(defstate wrapped-handler
  :start (wrap-handler))
