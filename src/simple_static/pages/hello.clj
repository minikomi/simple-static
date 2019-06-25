(ns simple-static.pages.hello
  {:ns-tracker/resource-deps ["test.txt"]}
  (:require [simple-static.pages.layout
             :as layout
             :refer [defdiv]]
            [clojure.set :as set]
            [simple-static.pages.aaa]))

(defdiv hello-list
  [:h1 "hello"]
  [:h2
   [:a {:href "/"}
    "Back to top"]]
  [:h3
   (slurp "data/test.txt")]
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
