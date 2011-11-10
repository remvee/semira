(ns semira.frontend.state
  (:require
   [semira.frontend.utils :as utils]
   [goog.uri.utils :as uri-utils]))

(def albums (atom nil))

(defn clear-albums []
  (reset! albums nil))

(defn more-albums [query page-size callback]
  (let [offset (count @albums)
        uri (-> "/albums"
                (uri-utils/appendParam "offset" offset)
                (uri-utils/appendParam "limit" (inc page-size))
                (uri-utils/appendParam "query" query))]
    (utils/remote-get uri
                      #(do (when (= offset (count @albums))
                             (swap! albums concat (take page-size %))
                             (callback @albums :offset offset :end-reached (> page-size (count %))))))))

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
