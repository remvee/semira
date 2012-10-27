;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.audio
  (:require [semira.frontend.utils :as utils]
            [goog.events :as gevents]
            [goog.uri.utils :as guri-utils]
            [goog.userAgent :as guser-agent]))

(def queue (atom ()))
(def player (atom (utils/by-id "player")))
(def playing-state (atom false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current []
  (first @queue))

(defn current-time []
  (. @player -currentTime))

(defn loading? []
  (and @playing-state (< (. @player -readyState) 4)))

(defn playing? []
  (and @playing-state (not (. @player -paused)) (not (loading?))))

(defn paused? []
  (and @playing-state (. @player -paused) (not (loading?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def update-current-fns (atom ()))

(defn- update-current []
  (doseq [fn @update-current-fns] (fn)))

(let [timer (goog.Timer. 1000)]
  (gevents/listen timer goog.Timer/TICK update-current)
  (. timer (start)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- track-uri [{id :id}]
  (let [uri (cond (and (. @player -canPlayType)
                       (not= "" (. @player canPlayType "audio/mpeg")))
                  (str "/stream/track/" id ".mp3")

                  (and (. @player -canPlayType)
                       (not= "" (. @player canPlayType "audio/ogg")))
                  (str "/stream/track/" id ".ogg")

                  :else
                  (str "/stream/track/" id ".mp3"))]
    (if guser-agent/WEBKIT
      (guri-utils/appendParam uri "wait" true)
      uri)))

(defn stop []
  (reset! playing-state false)
  (gevents/removeAll @player)
  (try
    (set! (. @player -src) "")
    (. @player (load))
    (catch js/Error e))
  (update-current))

(defn- play-first []
  (when (current)
    (. @player (pause))
    (set! (. @player -autoplay) true)
    (set! (. @player -src) (track-uri (current)))
    (. @player (load))
    (gevents/listen @player "ended"
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
  (if (. @player -paused)
    (. @player (play))
    (. @player (pause))))

;; android:
;; * can not play without content-length
;; * optional lower bitrate for non wifi playback