(ns semira.frontend.snippets
  (:require
   [semira.frontend.utils :as utils]))

(def h utils/h)

(defn play-link [album image]
  [:a {:href (str "/stream/album/" (:id album) ".mp3")
       :class "play"}
   [:img {:src image :alt "&#x1D160;"}]])

(def track-row-keys [:artist :album :title])

(defn track-row [track]
  [:li.track
   [:a {:href (str "/stream/track/" (:id track) ".mp3")}
    (utils/interposed-html track " / " track-row-keys)]
   " "
   [:span.length (utils/seconds->time (:length track))]])

(defn album [album]
  [:ol.tracks
   (str "<!-- " (:dir album) " @ " (:mtime album) " -->")
   (map track-row (:tracks album))])

(def album-row-keys [:genre :artist :album :year])

(defn album-row [album]
  [:li.album {:id (str "album-" (:id album))}
   [:a {:onclick (str "semira.frontend.album_toggle('" (:id album) "')")}
    (utils/interposed-html album " - " album-row-keys)]
   " "
   (play-link album "/images/note.png")
   [:div.album]])

(defn album-rows [albums]
  (concat (map album-row albums)
          [[:li.more
            [:a.more {:onclick "semira.frontend.albums_more()"}
             [:img {:src "/images/next.png" :alt "&rarr;"}]]]]))