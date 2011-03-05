(ns semira.web
  (:use [semira.models :as models]
        [semira.stream :as stream]
        [semira.utils :as utils]
        
        [compojure.core :only [defroutes GET POST ANY]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [include-js]])
  (:import [java.io File]))

(def *title* "SEMIRA")

(defn layout [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html
                [:head
                 [:title *title*]
                 (include-js "/js/jquery.js" "/js/app.js")]
                [:body
                 [:div.header
                  [:h1 *title*]]
                 [:div.content
                  body]
                 [:div.footer]]])})

(defn int->time [i]
  (when i
       (let [hours (quot i 3600)
             minutes (quot (rem i 3600) 60)
             seconds (rem i 60)]
         (cond (not= 0 hours)   (format "%d:%02d:%02d" hours minutes seconds)
               (not= 0 minutes) (format "%d:%02d" minutes seconds)
               :else            (format ":%02d" seconds)))))

(defmulti h class)
(defmethod h java.util.List
  [val] (apply str (interpose ", " val)))
(defmethod h Object
  [val] (str val))

(defn interposed-html [rec sep & ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (h (rec %))])
                          (filter #(rec %) ks)))]
    (if (seq r) r "..")))

(def *page-size* 25)

(def album-key-precedence [:genre :artist :album :year])

(defn albums-sorted [page]
  (take *page-size*
        (drop (* page *page-size*)
              (apply utils/sort-by-keys (models/albums) album-key-precedence))))

(defn album-title [album]
  (apply interposed-html album " - " album-key-precedence))

(defn album-show [album]
  [:div.album
   [:h2.title
    (album-title album)]
   [:dl.meta
    (mapcat #(vec [[:dt {:class (name %)}
                    (name %)]
                   [:dd {:class (name %)}
                    (h (album %))]])
            (filter #(album %)
                    [:artist :album :year :composer :conductor :producer :remixer :genre :encoding]))]
   [:ol.tracks
    (map (fn [track]
           [:li.track
            [:a {:href (str "/track/" (:id track))}
             (interposed-html track " / " :artist :album :title)]
            " "
            [:span.length (int->time (:length track))]])
         (:tracks album))]
   [:div#audio-container]])

(defn albums-index [page]
  [:div
   (when-not (= 0 page)
     [:a {:href (str "/?page=" (dec page))} "PREVIOUS PAGE"])
   [:ul.albums
    (map (fn [album]
           [:li.album
            [:a {:href (str "/album/" (:id album))}
             (album-title album)]])
         (albums-sorted page))]
   (when-not (empty? (albums-sorted (inc page)))
     [:a {:href (str "/?page=" (inc page))} "NEXT PAGE"])])

(defroutes routes
  (GET "/" [page]
       (let [page (if page (Integer/valueOf page) 0)]
         (layout (albums-index page))))
  (GET "/album/:id" [id]
       (let [album (models/album-by-id id)]
         (layout (album-show album))))
  (GET "/track/:id" [id]
       (let [track (models/track-by-id id)]
         {:status 200
          :headers {"Content-Type" "audio/ogg"}
          :body (stream/input track)})))

(def app (-> routes wrap-params (wrap-file "public") wrap-file-info))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))