;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.components
  (:require [reagent.core :as reagent]
            [semira.frontend.audio :as audio]
            [semira.frontend.sync :as sync]
            [semira.frontend.utils :as utils]))

(defn tracks-component [tracks]
  (let [{:keys [current-track current-time paused]} (audio/state)]
    (if tracks
      [:ol.tracks
       (map-indexed
        (fn [pos {:keys [id length] :as track}]
          [:li.track {:key id
                      :class (when (= track current-track)
                               (if paused "paused" "playing"))}
           [:a {:on-click #(if (= track current-track)
                             (audio/play-pause)
                             (audio/play tracks pos))}
            [:div.details
             (for [k [:year :genre :artist :album :title :composer]]
               (when-let [v (get track k)]
                 [:div {:key k, :class k} (utils/h v)]))]
            [:div.length
             (when (= track current-track)
               [:span.played (utils/seconds->time current-time) " / "])
             [:span.full (utils/seconds->time length)]]]])
        tracks)]
      [:div.loading "..."])))

(defonce n-albums (reagent/atom 100))

(defn albums-component []
  (let [{:keys [current-track paused]} (audio/state)]
    [:ul.albums
     (for [{:keys [id tracks selected] :as album}
           (take @n-albums (sync/albums))]
       [:li.album
        {:key id
         :class (when (some (partial = current-track) tracks)
                  (if paused "paused" "playing"))}
        [:a {:on-click #(sync/select-album! id (not selected))}
         [:div.details
          (for [k [:year :genre :artist :album :composer]]
            (when-let [v (get album k)]
              [:div {:key k, :class k} (utils/h v)]))]]
        (when selected
          [:div.album
           [tracks-component tracks]])])]))

(defn search-component []
  [:div.search
   [:input {:type "search"
            :placeholder ".."
            :auto-focus true
            :on-change #(sync/set-search! (-> % .-target .-value))}]])

(defn audio-status-component []
  [:div.audio-status (pr-str (audio/state))])

(defn main-component [& {:keys [debug]}]
  [:main
   [search-component]
   [albums-component]
   (when debug [audio-status-component])])

(defonce do-setup
  (.addEventListener js/window "scroll"
                     (fn []
                       (let [bottom (+ (-> js/window
                                           .-scrollY)
                                       (-> js/window
                                           .-innerHeight))
                             height (-> js/window
                                        .-document
                                        .-body
                                        .-clientHeight)
                             old-value @n-albums
                             album-height (/ height old-value)
                             new-value (-> bottom
                                           (/ album-height)
                                           (/ 20)
                                           (+ 2)
                                           int
                                           (* 20))]
                         (when-not (= new-value old-value)
                           (reset! n-albums new-value))))))
