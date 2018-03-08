(ns simple-static.frontend.core
  (:require [goog.dom :as gdom]))

(defn change-bg []
  (set! (.. (gdom/getElementByTagNameAndClass "h1") -style -background)
        (str "rgb("
             (rand-int 255) ","
             (rand-int 255) ","
             (rand-int 255)
             ")")))

(defn init []
  (change-bg))

(defonce run-init
  (init))
