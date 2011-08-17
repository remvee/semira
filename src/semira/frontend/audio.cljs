(ns semira.frontend.audio
  (:require
   [semira.frontend.utils :as utils]
   [goog.uri.utils :as uri-utils]
   [goog.userAgent :as user-agent]))

(def player (atom (new js/Audio "")))

(defn ^:export play []  
  (. @player (play)))

(defn ^:export pause []
  (. @player (pause)))

(defn ^:export play-or-pause []
  (if (. @player paused) (play) (pause)))

(defn ^:export load [id]
  (let [uri (cond (and (. @player canPlayType)
                       (not= "" (. @player canPlayType "audio/mpeg")))
                  (str "/stream/track/" id ".mp3")
                  
                  (and (. @player canPlayType)
                       (not= "" (. @player canPlayType "audio/ogg")))
                  (str "/stream/track/" id ".ogg")
                  
                  :else
                  (str "/stream/track/" id ".mp3"))
        uri (if user-agent/WEBKIT
              (uri-utils/appendParam uri "wait" true)
              uri)]
    (pause)
    (reset! player (new js/Audio uri))
    (set! (. @player autoplay) true)))

(defn update-current []
  (utils/inner-html
   (utils/by-id "current-track")
   (str "readyState: " (. @player readyState) " "
        "networkState: " (. @player networkState) " "
        "currentTime: " (utils/seconds->time (js/Math.round (. @player currentTime))) " "
        "error: " (. @player error) " "
        "muted: " (. @player muted) " "
        "ended: " (. @player ended) " "
        "paused: " (. @player paused) " "
        "volume: " (. @player volume))))

(defn update-current-continuously []
  (update-current)
  (js/setTimeout update-current-continuously 1000))

(update-current-continuously)

;; android:
;; * can not play without content-length
;; * optional lower bitrate for non wifi playback