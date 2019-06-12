(ns simple-static.pages.hello
  (:require [simple-static.pages.layout
             :as layout
             :refer [defdiv]]))

(defdiv hello-list
  [:h1 "hello"]
  [:h2
   [:a {:href "/"}
    "Back to top"]]
  [:ul
   (for [n (range 100)]
     (case (rem n 3)
       0 [:li "こんにちは"]
       1 [:li "Bonjour"]
       [:li "Hello"]))])

(defn template [args]
  (layout/base-template
   [:div hello-list]
   args))
