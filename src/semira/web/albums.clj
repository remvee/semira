(ns semira.web.albums
  (:require
   [semira.models :as models]
   [semira.utils :as utils]
   [semira.web.core :as web]
   [compojure.core :as compojure]))


(defn overview-header [query]
  [:div.header
   [:form#search.search {:method "get"}
    [:span.input
     [:input#search-query {:type "text", :name "query", :value query}]]
    [:span.button
     [:button {:type "submit"} "Go!"]]]])

(defn overview []
  [:div
   (overview-header "")
   [:ul#albums.albums
    [:li#albums-more ".."]]])

(compojure/defroutes handler
  (compojure/GET "/" [page query]
                 (web/layout (overview)))
  (compojure/GET "/album/:id" [id]
                 (let [album (dissoc (models/album-by-id id) :doc)] ; TODO cljs reader bug; can't read "\"foo\""
                   {:status 200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body (pr-str album)}))
  (compojure/GET "/albums" [offset limit keys query]
                 (let [offset (if offset (Integer/valueOf offset) 0)
                       limit (if limit (Integer/valueOf limit) utils/*page-size*)
                       keys (if keys (map keyword keys) [:genre :artist :album :year :id])
                       albums (take limit
                                    (drop offset
                                          (map #(select-keys % keys)
                                               (utils/sort-by-keys (models/albums {:query query})
                                                                   keys))))]
                   {:status 200
                    :headers {"Content-Type" "application/clojure; charset=utf-8"}
                    :body (pr-str albums)}))
  (compojure/GET "/update" []
                 (future
                   (models/scan)
                   (models/purge))
                 (Thread/sleep 2000)
                 {:status 307
                  :headers {"Location" "/latest"}}))
