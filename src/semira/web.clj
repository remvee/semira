(ns semira.web
  (:require [semira.models :as models]
            [semira.stream :as stream])
  (:use [compojure.core :only [defroutes GET POST ANY]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [remvee.ring.middleware.partial-content :only [wrap-partial-content]]
        
        [hiccup.core :only [html escape-html]]
        [hiccup.page-helpers :only [include-css include-js]])
  (:import [java.io File]))

(defn int->time [i]
  (when i
       (let [hours (quot i 3600)
             minutes (quot (rem i 3600) 60)
             seconds (rem i 60)]
         (cond (not= 0 hours)   (format "%d:%02d:%02d" hours minutes seconds)
               (not= 0 minutes) (format "%d:%02d" minutes seconds)
               :else            (format ":%02d" seconds)))))

(defmulti h class)
(defmethod h java.util.List [val] (escape-html (apply str (interpose ", " val))))
(defmethod h :default [val] (escape-html (str val)))

(defn interposed-html [rec sep ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (h (rec %))])
                          (filter #(rec %) ks)))]
    (if (seq r) r "..")))

(defn map->query-string [m]
  (apply str (interpose "&" (map (fn [[k v]] (str (name k) "=" v)) m))))

(def *page-size* 20)
(defn take-page [coll page]
  (take *page-size*
        (drop (* page *page-size*) coll)))

(def *title* "SEMIRA")

(defn layout [body & [{:keys [title]}]]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (html [:html
                [:head
                 [:title (h (if title (str *title* " / " title) *title*))]
                 [:meta {:name "viewport", :content "width=device-width, initial-scale=1, maximum-scale=1"}]
                 (include-css "/css/screen.css")]
                [:body body]])})

(defn album-play-link [album image]
  [:a {:href (str "/stream/album/" (:id album) ".mp3")
       :class "play"}
   [:img {:src image :alt "&#x1D160;"}]])

(defn album-show [album]
  (layout
   [:div.album
    (str "<!-- " (:dir album) " -->")
    [:div.header
     (album-play-link album "/images/note-larger.png")
     (when (:artist album)
       [:h2.artist (h (:artist album))])
     (when (:album album)
       [:h3.album (h (:album album))])]
    [:dl.meta
     (mapcat #(vec [[:dt {:class (name %)}
                     (h (name %))]
                    [:dd {:class (name %)}
                     (h (album %))]])
             (filter #(album %)
                     [:composer :conductor :producer :remixer :year :genre :encoding]))]
    [:ol.tracks
     (map (fn [track]
            [:li.track
             [:a {:href (str "/stream/track/" (:id track) ".mp3")}
              (interposed-html track " / " [:artist :album :title])]
             " "
             [:span.length (int->time (:length track))]])
          (:tracks album))]
    [:div#audio-container]]
   {:title (apply str
                  (interpose " - " (flatten (filter identity
                                                    (map album
                                                         [:artist :album :year])))))}))

(def albums-index-keys [:genre :artist :album :year])

(defn albums-index [albums & [{page :page, query :query :as params}]]
  (layout
   (let [pagination (if (or (> page 0)
                            (not (empty? (take-page albums (inc page)))))
                      [:div.pagination
                       [:span.previous
                        (if (= 0 page)
                          ""
                          [:a {:href (str "/?" (map->query-string (assoc params :page (dec page))))}
                           [:img {:src "/images/previous.png" :alt "&larr;"}]])]
                       [:span.next
                        (if (empty? (take-page albums (inc page)))
                          ""
                          [:a.next {:href (str "/?" (map->query-string (assoc params :page (inc page))))}
                           [:img {:src "/images/next.png" :alt "&rarr;"}]])]])]
     
     [:div
      [:div.header
       [:form {:method "get" :class "search"}
        [:div
         [:span.input
          [:input {:type "text", :name "query", :value query}]]
         [:span.button
          [:button {:type "submit"} "Go!"]]]]]

      [:ul.albums
       (when pagination [:li.pagination pagination])
       (map (fn [album]
              [:li.album
               [:a {:href (str "/album/" (:id album))}
                (interposed-html album " - " albums-index-keys)]
               " "
               (album-play-link album "/images/note.png")])
            (take-page albums page))
       (when pagination [:li.pagination pagination])]])))

(defroutes routes
  (GET "/" [page query]
       (let [page (if page (Integer/valueOf page) 0)
             albums (models/albums {:query query, :order albums-index-keys})]
         (albums-index albums {:page page, :query query})))
  (GET "/album/:id" [id]
       (let [album (models/album-by-id id)]
         (album-show album)))
  (GET "/stream/:model/:id.:ext" [model id ext :as request]
       (let [object ((get {"track" models/track-by-id
                           "album" models/album-by-id}
                          model models/track-by-id) id)
             type ({"mp3" "audio/mpeg"
                    "ogg" "audio/ogg"} ext)
             in (stream/get object type)
             len (stream/length object type)]
         {:status 200
          :headers (merge {"Content-Type" type}
                          (when len
                            {"Content-Length" (and len (str len))}))
          :body in}))
  (GET "/scan" []
       (future (models/scan))
       (Thread/sleep 2000)
       {:status 307
        :headers {"Location" "/"}}))

(def app (-> routes wrap-params (wrap-file "public") wrap-file-info wrap-partial-content))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))