;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.albums
  (:require [compojure.core :as compojure]
            [hiccup.core :as hiccup]
            [semira
             [models :as models]
             [utils :as utils]]))

(def app-title "SEMIRA")
(def rss-description "Latest additions")

(def ^:dynamic *page-size* 20)
(def album-keys [:genre :composer :artist :album :year :id]) ; also sort order!
(def track-keys [:id :composer :artist :album :title :length])

(defn tracks [album]
  (hiccup/html
   [:ol
    (map (fn [track]
           (vec [:li (apply str (->> [:artist :album :title]
                                     (map #(% track))
                                     flatten
                                     (filter identity)
                                     (interpose " - ")))]))
         (:tracks album))]))

(defn date-rfc822 [date]
  (.format (java.text.SimpleDateFormat. "EEE, d MMM yyyy HH:mm:ss z")
           (java.util.Date. date)))

(defn rss [albums]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       "<?xml-stylesheet type=\"text/xsl\" href=\"/rss.xsl\"?>"
       (hiccup/html
        {:mode :xml}
        [:rss
         [:channel
          [:title app-title]
          [:link "#"]
          [:description rss-description]
          (map (fn [album]
                 (vec [:item
                       [:title (hiccup/h (apply str (interpose " - "
                                                               (filter identity
                                                                       (flatten (map #(get album %)
                                                                                     [:artist :album]))))))]
                       [:link (str "/#" (:id album))]
                       [:guid (:id album)]
                       [:description (str "<![CDATA[" (tracks album) "]]>")]
                       [:author]
                       [:pubDate (date-rfc822 (:mtime album))]]))
               albums)]])))

(compojure/defroutes handler
  (compojure/GET "/album/:id" [id]
                 (let [album (models/album-by-id id)
                       album (assoc (select-keys album album-keys)
                               :tracks (map #(select-keys % track-keys) (:tracks album)))]
                   {:status 200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body (pr-str album)}))
  (compojure/GET "/albums" [offset limit query]
                 (let [offset (if offset
                                (partial drop (Integer/valueOf offset))
                                identity)
                       limit (if limit
                               (partial take (Integer/valueOf limit))
                               identity)
                       albums (->> (models/albums {:query query})
                                   (utils/sort-by-keys album-keys)
                                   (map #(select-keys % album-keys))
                                   offset
                                   limit)]
                   {:status 200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body (pr-str albums)}))
  (compojure/GET "/albums.rss" []
                 (let [albums (take *page-size*
                                    (reverse (sort-by :mtime (models/albums))))]
                   {:status 200
                    :headers {"Content-Type" "text/xml"}
                    :body (rss albums)}))
  (compojure/GET "/update" []
                 (future
                   (models/scan)
                   (models/purge))
                 (Thread/sleep 2000)
                 {:status 307
                  :headers {"Location" "/"}}))
