(ns semira.frontend.audio
  (:require
   [semira.frontend.utils :as utils]
   [goog.dom.classes :as dom-classes]
   [goog.events :as events]
   [goog.Timer :as timer]
   [goog.uri.utils :as uri-utils]
   [goog.userAgent :as user-agent]))

(def queue (atom ()))

(def playing? (atom false))

(def player (atom (new js/Audio "")))

(defn- update-current-state []
  (when-let [track (utils/by-id (str "track-" (first @queue)))]
    (dom-classes/enable track "playing" @playing?)
    (utils/busy track (and @playing? (< (. @player readyState) 4)))))

(defn- update-current-time []
  (if-let [current-time (utils/by-id (str "track-current-time-" (first @queue)))]
    (utils/inner-html
     current-time
     (if @playing?
       (str (utils/seconds->time (js/Math.round (. @player currentTime))) " / ")
       ""))))

(defn- update-current []
  (update-current-state)
  (update-current-time))

(defn- track-uri [id]
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

(defn ^:export stop []
  (reset! playing? false)
  (events/removeAll @player)
  (try
    (set! (. @player src) "")
    (. @player (load))
    (catch js/Error e))
  (update-current)

  (defn ^:export add [id]
    (swap! queue concat [id])))

(defn play-first []
  (utils/debug "play-first queue" @queue)
  
  (when (first @queue)
    (let [uri (track-uri (first @queue))
          current (first @queue)]
      (reset! player (new js/Audio uri))
      (events/listen @player "ended"
                     (fn []
                       (stop)
                       (swap! queue next)
                       (play-first)))
      (set! (. @player autoplay) true)
      (reset! playing? true))))

(defn ^:export play [ids]
  (stop)
  (reset! queue ids)
  (play-first))

(defn ^:export pause []
  (. @player (pause))
  (reset! playing? false))

(let [timer (goog.Timer. 1000)]
  (events/listen timer goog.Timer/TICK update-current)
  (. timer (start)))

;; android:
;; * can not play without content-length
;; * optional lower bitrate for non wifi playback