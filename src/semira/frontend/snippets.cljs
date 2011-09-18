(ns semira.frontend.snippets
  (:require
   [semira.frontend.utils :as utils]))

(defn play-link [album image]
  [:a {:href (str "/stream/album/" (:id album) ".mp3")
       :class "play"}
   [:img {:src image :alt "&#x1D160;"}]])

(def track-row-keys [:artist :album :title])

(defn track-row [track]
  (let [id (:id track)]
    [:li.track {:id (str "track-" id)}
     [:a {:onclick #(js/semira.frontend.track_play id)}
      (utils/interposed-html track " / " track-row-keys)]
     " "
     [:a.queue {:onclick #(js/semira.frontend.track_queue id )} "(+)"]
     [:span.status {:id (str "track-status-" id)}]
     [:span.length
      [:span.played {:id (str "track-current-time-" id)}]
      [:span.full (utils/seconds->time (:length track))]]]))

(defn album [album]
  [:ol.tracks
   (map track-row (:tracks album))])

(def album-row-keys [:genre :artist :album :year])

(defn album-row [album]
  [:li.album
   [:a {:onclick #(js/semira.frontend.album_toggle (:id album))}
    (utils/interposed-html album " - " album-row-keys)]
   " "
   (play-link album "/images/note.png")
   [:div.album  {:id (str "album-" (:id album))}]])

(defn album-more-row []
  [:li#albums-more.more
   [:a.more {:onclick #(js/semira.frontend.albums_more)}
    [:img {:src "/images/more.png" :alt "&rarr;"}]]])
