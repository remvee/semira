(ns semira.frontend.audio
  (:require
   [semira.frontend.utils :as utils]
   [goog.events :as events]
   [goog.uri.utils :as uri-utils]
   [goog.userAgent :as user-agent]))

(def queue (atom ()))
(def player (atom (utils/by-id "player")))
(def playing-state (atom false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current []
  (first @queue))

(defn current-time []
  (. @player currentTime))

(defn playing? []
  @playing-state)

(defn loading? []
  (and @playing-state
       (< (. @player readyState) 4)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def update-current-fns (atom ()))

(defn- update-current []
  (doseq [fn @update-current-fns] (fn)))

(let [timer (goog.Timer. 1000)]
  (events/listen timer goog.Timer/TICK update-current)
  (. timer (start)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- track-uri [{id :id}]
  (let [uri (cond (and (. @player canPlayType)
                       (not= "" (. @player canPlayType "audio/mpeg")))
                  (str "/stream/track/" id ".mp3")

                  (and (. @player canPlayType)
                       (not= "" (. @player canPlayType "audio/ogg")))
                  (str "/stream/track/" id ".ogg")

                  :else
                  (str "/stream/track/" id ".mp3"))]
    (if user-agent/WEBKIT
      (uri-utils/appendParam uri "wait" true)
      uri)))

(defn stop []
  (reset! playing-state false)
  (events/removeAll @player)
  (try
    (set! (. @player src) "")
    (. @player (load))
    (catch js/Error e))
  (update-current))

(defn- play-first []
  (when (current)
    (. @player (pause))
    (set! (. @player autoplay) true)
    (set! (. @player src) (track-uri (current)))
    (. @player (load))
    (events/listen @player "ended"
                   (fn []
                     (stop)
                     (swap! queue next)
                     (play-first)))
    (reset! playing-state true)
    (update-current)))

(defn play [tracks]
  (stop)
  (reset! queue tracks)
  (play-first))

(defn play-pause []
  (if (. @player paused)
    (. @player (play))
    (. @player (pause))))

;; android:
;; * can not play without content-length
;; * optional lower bitrate for non wifi playback