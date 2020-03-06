(ns simple-static.pages.hello
  (:require [simple-static.pages.layout
             :as layout
             :refer [defdiv]]
            [clojure.set :as set]
            [org.httpkit.client :as http]
            [simple-static.pages.aaa]
            [clojure.data.json :as json]))

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

(defn get-works-entries []
  (-> @(http/get "https://cdn.contentful.com/spaces/getvqzrigw6d/entries?access_token=AtgmLIGYPpflgHHf1YAZSIIGAhIMDx1xclKR9uGzPqE&order=-sys.createdAt")
      :body
      (json/read-str
       :key-fn keyword)
      ))

(def works (get-works-entries))

(defn get-img-map [data]
 (reduce
  (fn [acc a]
    (if-let [id (get-in a [:sys :id])]
      (assoc acc id (:fields a))
      acc))
  {}
  (:Asset (:includes data))))

(defn template [args]
  (let [data (get-works-entries)
        entries (:items data)
        img-map (get-img-map data)]
   (layout/base-template
    [:ul
     (for [entry entries]
       [:li
        [:h4 (-> entry :fields :description)]
        [:div
         [:img {:src
                (->  (get img-map (-> entry :fields :image :sys :id))
                     :file
                     :url)
                }]]])]
    args)))
