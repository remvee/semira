;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.albums
  (:require [clojure.string :as string]
            [compojure.core :as compojure]
            [hiccup.core :as hiccup]
            [semira.models :as models]
            [semira.utils :as utils])
  (:import java.text.SimpleDateFormat
           java.util.Date))

(def app-title "SEMIRA")
(def rss-description "Latest additions")
(def rss-page-size 20)

(def album-keys [:genre :composer :artist :album :year :id]) ; also sort order!
(def track-keys [:id :composer :artist :album :title :length])

(defn titleize [rec & keys]
  (->> keys (map rec) flatten (filter identity) (string/join " - ")))

(defn track-list [album]
  [:ol
   (map #(vec [:li (hiccup/h (titleize % :artist :album :title))])
        (:tracks album))])

(defn date-rfc822 [date]
  (.format (SimpleDateFormat. "EEE, d MMM yyyy HH:mm:ss z")
           (Date. date)))

(defn rss [albums]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       "<?xml-stylesheet type=\"text/xsl\" href=\"/rss.xsl\"?>"
       (hiccup/html {:mode :xml}
        [:rss
         [:channel
          [:title app-title]
          [:link "/"]
          [:description rss-description]
          (map (fn [album]
                 (vec [:item
                       [:title (hiccup/h (titleize album :artist :album))]
                       [:link (str "/#" (:id album))]
                       [:guid (:id album)]
                       [:description (str "<![CDATA[" (-> album track-list hiccup/html) "]]>")]
                       [:author]
                       [:pubDate (date-rfc822 (:mtime album))]]))
               albums)]])))

(compojure/defroutes bare-handler
  (compojure/GET "/album/:id" {:keys        [albums]
                               {:keys [id]} :params}
                 (when-let [album (models/album-by-id albums id)]
                   (let [album (assoc (select-keys album album-keys)
                                      :tracks (map #(select-keys % track-keys) (:tracks album)))]
                     {:status  200
                      :headers {"Content-Type" "application/clojure; charset=utf-8"}
                      :body    (pr-str album)})))
  (compojure/GET "/albums" {:keys [albums]}
                 (let [albums (->> albums
                                   (map #(select-keys % album-keys))
                                   (utils/sort-by-keys album-keys))]
                   {:status  200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body    (pr-str albums)}))
  (compojure/GET "/albums.rss" {:keys [albums]}
                 (let [albums (->> albums
                                   (sort-by :mtime)
                                   reverse
                                   (take rss-page-size))]
                   {:status  200
                    :headers {"Content-Type" "text/xml"}
                    :body    (rss albums)}))
  (compojure/GET "/update" []
                 (future
                   (models/scan)
                   (models/purge))
                 (Thread/sleep 2000)
                 {:status  307
                  :headers {"Location" "/"}}))

(def handler
  (fn [req]
    (bare-handler (assoc req :albums (models/albums)))))
