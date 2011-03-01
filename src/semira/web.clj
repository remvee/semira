(ns semira.web
  (:use [semira.models :as models]
        [compojure.core :only [defroutes GET POST ANY]]
        [hiccup.core :only [html]]))

(defn layout [title body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html
                [:head [:title title]]
                [:body
                 [:div.header
                  [:h1 title]]
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

(defn interposed-html [rec sep & ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (rec %)])
                          (filter #(rec %) ks)))]
    (if (seq r) r "..")))

(defn album-title [album]
  (interposed-html album " - " :artist :album :year))

(defn album-show [album]
  [:div.album
   [:h2.title
    (album-title album)]
   [:dl.meta
    (mapcat #(vec [[:dt {:class (name %)}
                    (name %)]
                   [:dd {:class (name %)}
                    (album %)]])
            (filter #(album %)
                    [:artist :album :year :composer :conductor :producer :remixer :genre :encoding]))]
   [:ol.tracks
    (map (fn [track]
           [:li.track
            [:a {:href "#"}
             (interposed-html track " / " :artist :album :title)]
            " "
            [:span.length (int->time (:length track))]])
         (:tracks album))]])

(defn albums-index []
  [:ul.albums
   (map (fn [a]
          [:li.album
           [:a {:href (str "/album/" (:id a))}
            (album-title a)]])
        (models/albums))])

(defroutes app
  (GET "/" []
       (layout "SEMIRA index" (albums-index)))
  (GET "/album/:id" [id]
       (let [album (models/album-by-id id)]
         (layout (apply str "SEMIRA album")
                 (album-show album)))))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))