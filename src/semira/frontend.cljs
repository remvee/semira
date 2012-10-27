;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend
  (:require [semira.frontend.audio :as audio]
            [semira.frontend.html :as html]
            [semira.frontend.state :as state]
            [semira.frontend.utils :as utils]
            [goog.events :as gevents]
            [goog.events.KeyCodes :as gevents-keycodes]
            [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.History :as ghist]
            [clojure.browser.repl :as repl]))

(def gevent-type goog.events.EventType)
(def gevent-keycodes goog.events.KeyCodes)

(def window (js* "window"))
(def page-size 15)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare album-listing album-row album-more-row album-not-found)

(defn albums-query []
  (. (utils/by-id "search-query") -value))

(def history (goog.History.))

(let [query (. history (getToken))]
  (set! (. (utils/by-id "search-query") -value) query))

(defn album-container [id]
  (utils/by-id (str "album-" id)))

(defn album-busy [id state]
  (let [album (gdom/getAncestorByTagNameAndClass (album-container id) "li" "album")]
    (utils/busy album state)))

(defn album-collapse [id]
  (album-busy id false)
  (gdom/removeChildren (album-container id)))

(defn albums-update [albums & {offset :offset end-reached :end-reached}]
  (doseq [id ["albums-more" "search-query"]]
    (utils/busy (utils/by-id id) false))

  (let [container (utils/by-id "albums")]
    (cond (empty? albums)
          (do (gdom/removeChildren container)
              (gdom/append container (html/build (album-not-found))))

          (or (nil? offset) (= offset 0))
          (do
            (gdom/removeChildren container)
            (doseq [row (map album-row albums)]
              (gdom/append container (html/build row))))

          :else
          (do
            (when (utils/by-id "albums-more")
              (gdom/removeNode (utils/by-id "albums-more")))
            (doseq [row (map album-row (drop offset albums))]
              (gdom/append container (html/build row)))
            (.focus (utils/by-id (str "album-"
                                 (:id (first (drop (dec offset) albums))))))))
    (when-not end-reached
      (gdom/append container (html/build (album-more-row))))))

(defn album-update [album]
  (album-busy (:id album) false)
  (let [container (album-container (:id album))]
    (gdom/removeChildren container)
    (gdom/append container (html/build (album-listing album)))))

(defn albums-more []
  (utils/busy (utils/by-id "albums-more") true)
  (utils/scroll-into-view (utils/by-id "albums-more")) ; workaround android browser jumping to top somethings (why does it do that?)
  (state/more-albums (albums-query) page-size albums-update))

(albums-more)

(defn album-toggle [id]
  (album-busy id true)
  (if (utils/first-by-tag-class "ol" "tracks" (album-container id))
    (album-collapse id)
    (state/album id album-update)))

(defn album-search []
  (utils/busy (utils/by-id "search-query") true)
  (. history (replaceToken (albums-query)))
  (state/clear-albums)
  (albums-more))

(gevents/listen (utils/by-id "search")
                (. gevent-type -SUBMIT)
                #(do (. % (preventDefault))
                     (album-search)))

(gevents/listen window
                (. gevent-type -KEYDOWN)
                (fn [event]
                  (when (and (not (= "INPUT" (.. event -target -tagName)))
                             (#{(. gevent-keycodes -PAUSE)
                                (. gevent-keycodes -SPACE)} (. event -keyCode)))
                    (.preventDefault event)
                    (audio/play-pause))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track-play [tracks]
  (if (= (first tracks) (audio/current))
    (audio/play-pause)
    (audio/play tracks)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-current-state []
  (when-let [track (utils/by-id (str "track-" (:id (audio/current))))]
    (gclasses/enable track "playing" (audio/playing?))
    (gclasses/enable track "paused" (audio/paused?))
    (utils/busy track (and (audio/loading?))))
  (when-let [album (utils/by-id (str "album-" (:album-id (audio/current))))]
    (gclasses/enable (. album -parentNode) "playing" (audio/playing?))
    (gclasses/enable (. album -parentNode) "paused" (audio/paused?))))

(defn update-current-time []
  (if-let [track-current-time (utils/by-id (str "track-current-time-" (:id (audio/current))))]
    (utils/inner-html
     track-current-time
     (if (or (audio/playing?) (audio/paused?))
       (str (utils/seconds->time (js/Math.round (audio/current-time))) " / ")
       ""))))

(swap! audio/update-current-fns conj update-current-state)
(swap! audio/update-current-fns conj update-current-time)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn onclick [event f & args]
  (.preventDefault event)
  (apply f args))

(defn track-row [track album]
  (let [id (:id track)]
    [:a.track {:id (str "track-" id)
               :href "#"
               :onclick #(onclick %
                                  track-play (drop-while (fn [x] (not= id (:id x)))
                                                         (map (fn [x] {:id (:id x) :album-id (:id album)})
                                                              (:tracks album))))}
     [:span.title
      (utils/interposed-html track " / " [:composer :artist :album :title])]
     " "
     [:span.status {:id (str "track-status-" id)}]
     [:span.length
      [:span.played {:id (str "track-current-time-" id)}]
      [:span.full (utils/seconds->time (:length track))]]]))

(defn album-listing [album]
  [:ol.tracks
   (map (fn [t] (track-row t album)) (:tracks album))])

(defn album-row [album]
  [:li.album
   [:a.album-info {:href "#" :onclick #(onclick % album-toggle (:id album))}
    (utils/interposed-html album " " [:year :genre :artist :composer :album])]
   " "
   [:div.album  {:id (str "album-" (:id album))}]])

(defn album-not-found []
  [:li.not-found "not found.."])

(defn album-more-row []
  [:a#albums-more.more {:href "#" :onclick #(onclick % albums-more)}
   [:img {:src "/images/more.png" :alt "&rarr;"}]])

;; a repl for debugging..
(when (re-find #"\?debug" (. window/location -href))
  (repl/connect "http://localhost:9000/repl"))