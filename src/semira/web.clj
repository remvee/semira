(ns semira.web
  (:require [semira.models :as models]
            [semira.stream :as stream])
  (:use [compojure.core :only [defroutes GET POST ANY]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.file :only [wrap-file]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [remvee.ring.middleware.partial-content :only [wrap-partial-content]]
        
        [hiccup.core :only [html]]
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
(defmethod h java.util.List [val] (apply str (interpose ", " val)))
(defmethod h Object [val] (str val))

(defn interposed-html [rec sep ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (h (rec %))])
                          (filter #(rec %) ks)))]
    (if (seq r) r "..")))

(def *title* "SEMIRA")

(defn layout [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html
                [:head
                 [:title *title*]
                 [:meta {:name "viewport", :content "width=device-width, initial-scale=1, maximum-scale=1"}]

                 (include-js "/js/jquery.js" "/js/app.js")
                 (include-css "/css/screen.css")]
                [:body
                 [:div.header
                  [:h1 [:a {:href "/"} *title*]]]
                 [:div.content
                  body]
                 [:div.footer]]])})

(defn album-show [album]
  [:div.album
   [:h2.title
    (interposed-html album " - " [:artist :album])]
   [:dl.meta
    (mapcat #(vec [[:dt {:class (name %)}
                    (name %)]
                   [:dd {:class (name %)}
                    (h (album %))]])
            (filter #(album %)
                    [:composer :conductor :producer :remixer :year :genre :encoding]))]
   [:ol.tracks
    (map (fn [track]
           [:li.track
            [:a {:href (str "/track/" (:id track) ".mp3")
                 :href-mp3 (str "/track/" (:id track) ".mp3")
                 :href-ogg (str "/track/" (:id track) ".ogg")}
             (interposed-html track " / " [:artist :album :title])]
            " "
            [:span.length (int->time (:length track))]])
         (:tracks album))]
   [:div#audio-container]])

(defn albums-index [page]
  (let [ks [:genre :artist :album :year]
        paging [:div.paging
                (if (= 0 page)
                  [:span.previous "&larr;"]
                  [:a.previous {:href (str "/?page=" (dec page))} "&larr;"])
                " "
                (if (empty? (models/albums {:page (inc page)}))
                  [:span.next "&rarr;"]
                  [:a.next {:href (str "/?page=" (inc page))} "&rarr;"])]]
    [:div
     paging
     [:ul.albums
      (map (fn [album]
             [:li.album
              [:a {:href (str "/album/" (:id album))}
               (interposed-html album " - " ks)]])
           (models/albums {:page page, :order ks}))]
     paging]))

(defroutes routes
  (GET "/" [page]
       (let [page (if page (Integer/valueOf page) 0)]
         (layout (albums-index page))))
  (GET "/album/:id" [id]
       (let [album (models/album-by-id id)]
         (layout (album-show album))))
  (GET "/track/:id.:ext" [id ext :as request]
       (let [track (models/track-by-id id)
             type ({"mp3" "audio/mpeg"
                    "ogg" "audio/ogg"} ext)
             in (stream/get track type)
             len (stream/length track type)]
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