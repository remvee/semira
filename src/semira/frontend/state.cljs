(ns semira.frontend.state
  (:require
   [semira.frontend.utils :as utils]
   [goog.uri.utils :as uri-utils]))

(def albums (atom nil))

(defn clear-albums []
  (reset! albums nil))

(defn more-albums [query callback]
  (let [uri (-> "/albums"
                (uri-utils/appendParam "offset" (count @albums))
                (uri-utils/appendParam "limit" 15)
                (uri-utils/appendParam "query" query))]
    (utils/remote-get uri
                      #(do (swap! albums concat %)
                           (callback @albums)))))

(defn album [id callback]
  (let [album (first (filter #(= id (:id %)) @albums))]
    (if (:tracks album)
      (callback album)
      (let [uri (str "/album/" id)]
        (utils/remote-get uri
                          #(do (swap! albums
                                      (fn [albums]
                                        (map (fn [a] (if (= id (:id a)) % a)) albums)))
                               (callback %)))))))
