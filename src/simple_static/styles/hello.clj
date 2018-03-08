(ns simple-static.styles.hello
  (:require [garden.selectors :as gs]
            [garden.color :as gc]
            [garden.units :refer [px percent em]]))

(def styles
  [:&.hello
   {
    :width (percent 100)
    :font-size (px 40)}
   ])
