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
         (cond (not= 0 hours)   (format " %d:%02d:%02d" hours minutes seconds)
               (not= 0 minutes) (format " %d:%02d" minutes seconds)
               :else            (format " :%02d" seconds)))))

(defn album-show [album]
  [:div.album
   [:h2.title
    (interpose " - "
               (map #(vec [:span {:class %} (album %)])
                    (filter #(album %) [:artist :album :year])))]
   [:dl.meta
    (mapcat #(vec [[:dt (name %)] [:dd (album %)]])
            (filter #(album %)
                    [:artist :album :year :composer :conductor :producer :remixer :genre :encoding]))]
   [:ol.tracks
    (map (fn [track]
           [:li
            [:span.title (:title track)]
            [:span.length (int->time (:length track))]])
         (:tracks album))]])

(defn albums-index []
  (layout "SEMIRA index"
          [:ul.albums
           (map (fn [a]
                  [:li.album
                   (album-show a)])
                (models/albums))]))

(defroutes app
  (GET "/" [] (albums-index)))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))