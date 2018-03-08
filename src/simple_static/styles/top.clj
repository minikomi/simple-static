(ns simple-static.styles.top
  (:require [garden.selectors :as gs]
            [garden.color :as gc]
            [garden.units :refer [px percent em]]))

(def styles
  [:&.top
   {:text-align 'center
    :width (percent 100)}
   [:h1 {:background 'blue
         :color 'white
         :font-size (px 32)}]])
