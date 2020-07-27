;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.albums
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :as compojure]
            [hiccup.core :as hiccup]
            [semira.image :as image]
            [semira.models :as models])
  (:import java.text.SimpleDateFormat
           java.util.Date))

(def app-title "SEMIRA")

(def rss-description "Latest additions")
(def rss-page-size 20)

(def artwork-size 500)
(def artwork-expires-msecs (* 1000 60 60 24 365)) ; one year

(defn rfc1123-date-format []
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")
    (.setTimeZone (java.util.TimeZone/getTimeZone "GMT"))))

(defn ->album [{:keys [genre composer artist album year id search-index artwork]}]
  {:genre        (sort genre)
   :composer     (sort composer)
   :artist       (sort artist)
   :album        album
   :year         year
   :id           id
   :search-index search-index
   :artwork      (some? artwork)})

(defn ->track [{:keys [composer artist album title length id]}]
  {:composer (sort composer)
   :artist   (sort artist)
   :album    album
   :title    title
   :length   length
   :id       id})

(defn album-sort [{:keys [genre composer artist album year id]}]
  (str [genre composer artist album year id]))

(defn titleize [rec & keys]
  (->> keys (map rec) flatten (filter identity) (string/join " - ")))

(defn track-list [{:keys [id artwork] :as album}]
  [:div
   (when artwork
     [:img {:src (str "/artwork/" id)}])
   [:ol
    (map #(vec [:li (hiccup/h (titleize % :artist :album :title))])
         (:tracks album))]])

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
                       [:link (str "/#rss-id|" (:id album))]
                       [:guid (:id album)]
                       [:description (str "<![CDATA[" (-> album track-list hiccup/html) "]]>")]
                       [:author]
                       [:pubDate (date-rfc822 (:mtime album))]]))
               albums)]])))

(defn image-stream [path]
  (let [image          (-> path
                           io/file
                           image/from-file)]
    (-> image
        (image/crop-to-square)
        (image/scale [artwork-size artwork-size])
        (image/to-stream ))))

(compojure/defroutes bare-handler
  (compojure/GET "/album/:id" {:keys        [albums]
                               {:keys [id]} :params}
                 (when-let [album (models/album-by-id albums id)]
                   (let [album (assoc (->album album)
                                      :tracks (map ->track (:tracks album)))]
                     {:status  200
                      :headers {"Content-Type" "application/clojure; charset=utf-8"}
                      :body    (pr-str album)})))
  (compojure/GET "/artwork/:id" {:keys        [albums]
                                 {:keys [id]} :params}
                 (when-let [artwork (:artwork (models/album-by-id albums id))]
                   {:status  200
                    :headers {"Content-Type"  "image/jpeg"
                              "Expires"       (.format (rfc1123-date-format)
                                                       (Date. (+ (System/currentTimeMillis)
                                                                 artwork-expires-msecs)))}
                    :body    (image-stream artwork)}))
  (compojure/GET "/albums" {:keys [albums]}
                 (let [albums (->> albums
                                   (map ->album)
                                   (sort-by album-sort))]
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
                  :headers {"Location" "/"}})
)

(def handler
  (fn [req]
    (bare-handler (assoc req :albums (models/albums)))))
