(ns semira.web.albums
  (:require
   [semira.models :as models]
   [semira.utils :as utils]
   [semira.web.core :as web]
   [compojure.core :as compojure]
   [hiccup.core :as hiccup]))

(def rss-description "Latest additions")

(def ^:dynamic *page-size* 20)

(defn tracks [album]
  (hiccup/html
   [:ol
    (map (fn [track]
           (vec [:li (apply str (interpose " - "
                                           (filter identity
                                                   (flatten (map #(% track)
                                                                 [:artist :album :title])))))]))
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
          [:title web/app-title]
          [:link "#"]
          [:description rss-description]
          (map (fn [album]
                 (vec [:item
                       [:title (hiccup/h (apply str (interpose " - "
                                                               (filter identity
                                                                       (flatten (map #(get album %)
                                                                                     [:artist :album]))))))]
                       [:link "#"]
                       [:guid (:id album)]
                       [:description (str "<![CDATA[" (tracks album) "]]>")]
                       [:author]
                       [:pubDate (date-rfc822 (:mtime album))]]))
               albums)]])))

(compojure/defroutes handler
  (compojure/GET "/album/:id" [id]
                 (let [album (dissoc (models/album-by-id id) :doc)]
                   {:status 200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body (pr-str album)}))
  (compojure/GET "/albums" [offset limit keys query]
                 (let [offset (if offset (Integer/valueOf offset) 0)
                       limit (if limit (Integer/valueOf limit) *page-size*)
                       keys (if keys (map keyword keys) [:genre :composer :artist :album :year :id])
                       sorter (if (empty? query)
                                #(reverse (sort-by :mtime %))
                                #(utils/sort-by-keys % keys))
                       albums (take limit
                                    (drop offset
                                          (map #(select-keys % keys)
                                               (sorter (models/albums {:query query})))))]
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
