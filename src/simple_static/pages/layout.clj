(ns simple-static.pages.layout
  (:require [clj-time.local :as local]
            [clojure.data.json :as json]
            [hiccup.page :as hp]))

;; common

(defn pad-number [n]
  (str (when (> 10 n) "0") n))

(defn json-blob [data]
  [:script {:type "text/json"} (json/write-str data)])

(defmacro defdiv [divname & divcontent]
  `(def ~divname
     [:div {:id ~(name divname)}
      (list ~@divcontent)]))

;; layout

(def title-str "Simple Static")

(defn make-title [params]
  (str title-str
       (when-let [extra (:title params)]
         (str " | " extra))))

(def description-str "A simple site")

(defn facebook [params]
  (list
   [:meta
    {:property "og:title"
     :content (make-title params)}]
   [:meta
    {:property "og:type"
     :content "website"}]
   [:meta
    {:property "og:url"
     :content ""}]
   [:meta
    {:property "og:image"
     :content ""}]
   [:meta
    {:property "og:description"
     :content (or (:description params) description-str)}]))

(defn html-meta [params]
  (list
   [:meta {:charset "UTF-8"}]
   [:meta {:name "description"
           :content (or (:description params) description-str)}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}])
  (facebook params))

(def default-css
<<<<<<< HEAD
  (list (hp/include-css "/css/main.css")))
=======
  (list (hp/include-css (str "/css/styles.css?v=" (local/local-now)))))
>>>>>>> 244b2787091c2738b982df42f106fc33f8cc87e3

;; menu

(def header
  [:div#header
   [:h1#header-logo "SIMPLE"]])

(defn section-header [bc]
  [:h2#section-header (name bc)])

(def google-analytics
  [:script {:a "c"}])

(defn base-template
  ([content] (base-template content {}))
  ([content params]
   (hp/html5
    [:head
     (str "<!-- Rendered:" (local/local-now) " -->")
     (html-meta params)
     default-css
     [:title (make-title params)]]
    [:body {:class (:body-class params "default")}
     [:div#total-wrapper
      [:div#content content]
      google-analytics
      [:div#footer]
      (case (:js params)
        [:script {:src "/cljs-out/dev-main.js"}])]])))
