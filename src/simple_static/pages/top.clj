(ns simple-static.pages.top
  (:require [simple-static.pages.layout
             :as layout
             :refer [defdiv]]))

(defdiv top-header
  [:h1 "static"]
  [:div
   [:img {:src "/images/clojure.png"}]
   [:h2
    [:a {:href "/hello"}
     "TO HELLO PAGE"]]])

(defn template [args]
  (layout/base-template
   [:div#top-inner
    top-header]
   args))
