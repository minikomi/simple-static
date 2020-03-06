(ns simple-static.styles.core
  (:require
   [clojure.java.io :as io]
   [simple-static.styles.top :as top]
   [simple-static.styles.hello :as hello]
   [co.poyo.static-tools.config :as config]
   [garden.core :as garden]
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

(defn compiled [data]
  (garden/css
   {:prety-print? (:dev config/env false)
    :preamble #{(io/resource "css/normalize.css")}}
   combined))
