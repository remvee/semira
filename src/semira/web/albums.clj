(ns semira.web.albums
  (:require
   [semira.models :as models]
   [semira.utils :as utils]
   [semira.web.core :as web]
   [compojure.core :as compojure]))

(def h utils/h)

(def overview-keys [:genre :artist :album :year])

(defn play-link [album image]
  [:a {:href (str "/stream/album/" (:id album) ".mp3")
       :class "play"}
   [:img {:src image :alt "&#x1D160;"}]])

(defn show-header [album]
  [:div.header
    (play-link album "/images/note-larger.png")
    (if (:artist album) [:h2.artist (h (:artist album))])
    (if (:album album) [:h3.album (h (:album album))])])

(defn show [album]
  [:div.album
   (str "<!-- " (:dir album) " @ " (:mtime album) " -->")
   (show-header album)
   [:dl.meta
    (mapcat #(vec [[:dt {:class (name %)} (h (name %))]
                   [:dd {:class (name %)} (h (album %))]])
            (filter #(album %)
                    [:composer :conductor :producer :remixer :year :genre :encoding]))]
   [:ol.tracks
    (map (fn [track]
           [:li.track
            [:a {:href (str "/stream/track/" (:id track) ".mp3")}
             (utils/interposed-html track " / " [:artist :album :title])]
            " "
            [:span.length (utils/seconds->time (:length track))]])
         (:tracks album))]
   [:div#audio-container]])

(defn overview-header [query]
   [:div.header
    [:form {:method "get" :class "search"}
     [:div
      [:span.input
       [:input {:type "text", :name "query", :value query}]]
      [:span.button
       [:button {:type "submit"} "Go!"]]]]])

(defn overview [sorting albums & [{page :page, query :query :as params}]]
  [:div
   (overview-header query)
   (let [url-fn #(str "?" (utils/map->query-string (assoc params :page %)))
         pagination
         (let [first-page (= page 0)
               last-page (empty? (utils/take-page albums (inc page)))]
           (if-not (and first-page last-page)
             [:div.pagination
              [:div
               [:span.previous
                (if-not first-page
                  [:a {:href (url-fn (dec page))}
                   [:img {:src "/images/previous.png" :alt "&larr;"}]])]
               [:span.sorting
                [:a (if-not (= sorting :sorted) {:href "/"}) "sorted"]
                " | "
                [:a (if-not (= sorting :latest) {:href "/latest"}) "latest additions"]]
               [:span.next
                (if-not last-page
                  [:a.next {:href (url-fn (inc page))}
                   [:img {:src "/images/next.png" :alt "&rarr;"}]])]]]))]
     [:ul.albums
      (if pagination [:li.pagination pagination])
      (map (fn [album]
             [:li.album
              [:a {:href (str "/album/" (:id album))}
               (utils/interposed-html album " - " overview-keys)]
              " "
              (play-link album "/images/note.png")])
           (utils/take-page albums page))
      (if pagination [:li.pagination pagination])])])

(compojure/defroutes handler
  (compojure/GET "/" [page query]
                 (web/layout
                  (overview :sorted
                            (utils/sort-by-keys (models/albums {:query query})
                                                overview-keys)
                            {:page (if page (Integer/valueOf page) 0)
                             :query query})))
  (compojure/GET "/latest" [page query]
                 (web/layout
                  (overview :latest
                            (reverse (sort-by :mtime (models/albums {:query query})))
                            {:page 0
                             :query query})))
  (compojure/GET "/album/:id" [id]
                 (web/layout
                  (show (models/album-by-id id))))
  (compojure/GET "/update" []
                 (future
                   (models/scan)
                   (models/purge))
                 (Thread/sleep 2000)
                 {:status 307
                  :headers {"Location" "/latest"}}))
