;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.components
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [semira.frontend.audio :as audio]
            [semira.frontend.sync :as sync]
            [semira.frontend.utils :as utils]))

(defn track-component [track]
  [:div.details
   (for [k [:artist :album :title :composer]]
     (when-let [v (get track k)]
       [:div {:key k, :class k} (utils/h v)]))])

(defn tracks-component [{:keys [tracks] :as album}]
  (let [{:keys [current-track
                current-time
                paused
                network-state
                ready-state]} (audio/state)]
    (if tracks
      [:ol.tracks
       (map-indexed
        (fn [pos {:keys [id length] :as track}]
          [:li.track {:key id
                      :class (when (= track current-track)
                               (->> [(if paused "paused" "playing")
                                     (when (or (= network-state :no-source)
                                               (= ready-state :nothing))
                                       "loading")]
                                    (filter identity)
                                    (string/join " ")))}
           [:a {:on-click #(if (= track current-track)
                             (audio/play-pause)
                             (audio/play album pos))}
            (track-component track)
            [:div.length
             (when (= track current-track)
               [:span.played (utils/seconds->time current-time) " / "])
             [:span.full (utils/seconds->time length)]]]])
        tracks)]
      [:div.tracks.loading "..."])))

(defonce n-albums (reagent/atom 100))

(defn album-component [{:keys [id tracks selected] :as album}]
  (let [{:keys [current-track paused]} (audio/state)]
    [:li.album
     {:key   id
      :class [(when (some (partial = current-track) tracks)
                (if paused "paused" "playing"))
              (when selected "selected")]}
     [:a {:on-click #(sync/select-album! id (not selected))}
      [:div.details
       (for [k [:year :genre :artist :album :composer]]
         (when-let [v (get album k)]
           [:div {:key k, :class k} (utils/h v)]))]]
     (when selected
       [:div.album
        [tracks-component album]])]))

(def album-component-scroll-into-view
  (with-meta album-component
    {:component-did-update (fn [this [_ prev-props]]
                             (when (or (and (:selected (reagent/props this))
                                            (not (:selected prev-props)))
                                       (and (:tracks (reagent/props this))
                                            (not (:tracks prev-props))))
                               (utils/scroll-into-view-if-needed (reagent/dom-node this))))}))

(defn albums-component []
  (let [albums (sync/albums)]
    (if (nil? albums)
      [:div.albums.loading "..."]
      [:ul.albums
       (for [album (take @n-albums albums)]
         [album-component-scroll-into-view (assoc album :key (:id album))])])))

(let [search-atom (reagent/atom (utils/get-location-hash))]
  (add-watch search-atom :history-state
             (fn [_ _ _ v]
               (utils/set-location-hash v)))

  (defn search-component []
    [:div.search
     [:input {:type        "text"
              :value       @search-atom
              :placeholder ".."
              :on-change   #(let [v (-> % .-target .-value)]
                              (reset! search-atom v)
                              (sync/set-search! v))}]]))

(defn audio-status-component []
  [:div.audio-status (pr-str (audio/state))])

(def play-svg
  [:svg {:viewBox "0 0 512 1024" :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M0 192l512 320L0 832V192z"}]])

(def pause-svg
  [:svg {:viewBox "0 0 512 1024" :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M0 832h192V192H0V832zM320 192v640h192V192H320z"}]])

(defn player-component []
  (let [{:keys              [paused current-track current-time]
         {:keys [length]}   :current-track
         {:keys [playlist]} :queue} (audio/state)
        current-track               (when current-track
                                      (into current-track playlist))]
    [:div.player {:class (when current-track
                           (if paused "paused" "playing"))}
     (if paused
       [:button.play {:on-click #(audio/play)} play-svg]
       [:button.pause {:on-click #(audio/pause)} pause-svg])
     [:div.track
      [:div.details
       (when current-track
         (->> [:artist :title]
              (map current-track)
              (filter identity)
              (map utils/h)
              (string/join " - ")))]
      (when (and length current-track)
        [:input {:type      "range"
                 :min       0
                 :max       length
                 :value     current-time
                 :read-only true
                 :on-change #(audio/seek (-> % .-target .-value))}])]]))

(defn main-component [& {:keys [debug]}]
  [:div
   [:main
    [search-component]
    [albums-component]
    (when debug [audio-status-component])]
   [:footer
    [player-component]]])

(defonce do-setup
  (.addEventListener js/window "scroll"
                     (fn []
                       (let [bottom (+ (.-scrollY js/window)
                                       (.-innerHeight js/window))
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
