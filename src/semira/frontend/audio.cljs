(ns semira.frontend.audio
  (:require
   [semira.frontend.utils :as utils]
   [goog.events :as events]
   [goog.Timer :as timer]
   [goog.uri.utils :as uri-utils]
   [goog.userAgent :as user-agent]))

(def current (atom nil))

(def playing? (atom nil))

(def player (atom (new js/Audio "")))

(defn update-current []
  (if-let [status (utils/by-id (str "track-status-" @current))]
    (utils/inner-html
     status
     (if @playing?
       (pr-str {:readyState (. @player readyState)
                :networkState (. @player networkState)
                :error (. @player error)
                :paused (. @player paused)
                :ended (. @player ended)})
       "")))
                     
  (if-let [current-time (utils/by-id (str "track-current-time-" @current))]
    (utils/inner-html
     current-time
     (if @playing?
       (str (utils/seconds->time (js/Math.round (. @player currentTime))) " / ")
       ""))))

(defn track-uri [id]
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

(defn ^:export play []
  (. @player (play))
  (reset! playing? true))

(defn ^:export pause []
  (. @player (pause))
  (reset! playing? false))

(defn ^:export play-or-pause []
  (if (. @player paused) (play) (pause)))

(defn kill-player []
  (try
    (set! (. @player src) "")
    (. @player (load))
    (catch js/Error e)))

(defn ^:export load [id]
  (let [uri (track-uri id)]
    (pause)
    (update-current)
    (kill-player)
    (reset! current id)
    (reset! player (new js/Audio uri))
    (set! (. @player autoplay) true)
    (reset! playing? true)))


(let [timer (goog.Timer. 1000)]
  (. timer (start))
  (events/listen timer goog.Timer/TICK update-current))

;; android:
;; * can not play without content-length
;; * optional lower bitrate for non wifi playback