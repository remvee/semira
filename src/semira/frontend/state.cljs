;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.state
  (:require
   [semira.frontend.utils :as utils]
   [goog.uri.utils :as guri-utils]))

(def albums (atom nil))

(defn clear-albums []
  (reset! albums nil))

(defn more-albums [query page-size callback]
  (let [offset (count @albums)
        uri (-> "/albums"
                (guri-utils/appendParam "offset" offset)
                (guri-utils/appendParam "limit" (inc page-size))
                (guri-utils/appendParam "query" query))]
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
