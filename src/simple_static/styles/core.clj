(ns simple-static.styles.core
  (:require
   [simple-static.styles.top :as top]
   [simple-static.styles.hello :as hello]
   [garden.selectors :as gs]
   [garden.units :refer [em percent px]]))

(def combined
  [[:*
    {:box-sizing 'border-box}]
   [:body
    {:background 'green
     :font-size (px 14)}]
   top/styles
   hello/styles])
