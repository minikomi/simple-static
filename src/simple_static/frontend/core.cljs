(ns simple-static.frontend.core
  (:require
   [goog.dom :as gdom]
   [goog.events :as events]
   [goog.events.EventType :as event-type]
   ))

(defn change-bg [ev]
  (when (not= ev.target.tagName "A")
   (set! (.. (gdom/getElementByTagNameAndClass "body") -style -background)
         (str "rgb("
              (rand-int 255) ","
              (rand-int 255) ","
              (rand-int 255)
              ")"))))

(defn init []
  (events/unlisten js/window event-type/CLICK change-bg)
  (events/listen js/window event-type/CLICK change-bg))

(defonce run-init
  (init))
