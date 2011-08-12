(ns semira.web
  (:require [semira.models :as models]
            [semira.stream :as stream]
            [semira.utils  :as utils])
  (:use [ring.middleware.params :only [wrap-params]]
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [remvee.ring.middleware.partial-content :only [wrap-partial-content]]
        [compojure.core :only [defroutes GET POST ANY]]
        [hiccup.core :only [html escape-html]]
        [hiccup.page-helpers :only [include-css include-js]])
  (:import [java.io File]))

(defn int->time [i]
  (if i
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

(def ^:dynamic *page-size* 20)
(defn take-page [coll page]
  (take *page-size*
        (drop (* page *page-size*) coll)))

(def app-title "SEMIRA")

(defn layout [body & [{:keys [title]}]]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (html [:html
                [:head
                 [:title (h (if title (str app-title " / " title) app-title))]
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
    (str "<!-- " (:dir album) " @ " (:mtime album) " -->")
    [:div.header
     (album-play-link album "/images/note-larger.png")
     (if (:artist album) [:h2.artist (h (:artist album))])
     (if (:album album) [:h3.album (h (:album album))])]
    [:dl.meta
     (mapcat #(vec [[:dt {:class (name %)} (h (name %))]
                    [:dd {:class (name %)} (h (album %))]])
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

(defn albums-index [sorting albums & [{page :page, query :query :as params}]]
  (layout
   [:div
    [:div.header
     [:form {:method "get" :class "search"}
      [:div
       [:span.input
        [:input {:type "text", :name "query", :value query}]]
       [:span.button
        [:button {:type "submit"} "Go!"]]]]]
    (let [url-fn #(str "?" (map->query-string (assoc params :page %)))
          pagination
          (let [first-page (= page 0)
                last-page (empty? (take-page albums (inc page)))]
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
                (interposed-html album " - " albums-index-keys)]
               " "
               (album-play-link album "/images/note.png")])
            (take-page albums page))
       (if pagination [:li.pagination pagination])])]))

(defroutes routes
  (GET "/" [page query]
       (albums-index :sorted
                     (utils/sort-by-keys (models/albums {:query query})
                                         albums-index-keys)
                     {:page (if page (Integer/valueOf page) 0)
                      :query query}))
  (GET "/latest" [page query]
       (albums-index :latest
                     (reverse (sort-by :mtime (models/albums {:query query})))
                     {:page (if page (Integer/valueOf page) 0)
                      :query query}))
  (GET "/album/:id" [id]
       (album-show (models/album-by-id id)))
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
                          (if len {"Content-Length" (str len)}))
          :body in}))
  (GET "/update" []
       (future
         (models/scan)
         (models/purge))
       (Thread/sleep 2000)
       {:status 307
        :headers {"Location" "/latest"}}))

(defn wrap-log-request [app]
  (fn [req]
    (println (str (-> (java.text.SimpleDateFormat. "[yyyy/MM/dd HH:mm:ss] ")
                      (.format (java.util.Date.)))
                  (.toUpperCase (name (:request-method req)))
                  " "
                  (:uri req)
                  (if (:query-string req)
                    (str "?" (:query-string req)))))
    (app req)))

(def app (-> routes wrap-params (wrap-file "public") wrap-file-info wrap-partial-content wrap-log-request))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))
