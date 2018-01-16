;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.sync
  (:require [cljs-http.client :as http]
            [cljs.core.async :as async]
            [cljs.reader :as reader]
            [clojure.string :refer [index-of lower-case]]
            [reagent.core :as reagent]
            [semira.frontend.utils :as utils])
  (:require-macros [cljs.core.async.macros :as async]))

(defonce albums-atom (reagent/atom nil))

(defn fetch-tracks! [id]
  (async/go
    (let [tracks (-> (str "album/" id)
                     http/get
                     async/<!
                     :body
                     reader/read-string
                     :tracks)]
      (swap! albums-atom
             assoc-in [id :tracks] tracks))))

(defn select-album! [id & [flag]]
  (swap! albums-atom
         #(reduce-kv (fn [m k v]
                       (assoc-in m [k :selected]
                                 (and (= id k) flag)))
                     % %))
  (let [album (get @albums-atom id)]
    (when-not (:tracks album)
      (fetch-tracks! id))))

(defonce search-chan (async/chan 1))

(defn normalize-search-text [text]
  (lower-case text))

(defn set-search! [text]
  (async/go
    (async/put! search-chan (normalize-search-text text))))

(defonce search-atom (reagent/atom (normalize-search-text (utils/get-location-hash))))

(defonce do-setup
  (async/go-loop [val nil]
    (async/alt!
      (async/timeout 250)
      (when val
        (reset! search-atom val)
        (recur nil))

      search-chan
      ([text] (recur text)))
    (recur val)))

(defn albums []
  (let [albums @albums-atom
        search @search-atom]
    (when albums
      (filter #(index-of (:search %) search)
              (sort-by :index (vals albums))))))

(defn setup! []
  (async/go
    (reset! albums-atom
            (->> "albums"
                 http/get
                 async/<!
                 :body
                 reader/read-string
                 (map-indexed (fn [i v]
                                [(:id v)
                                 (assoc v
                                        :index i
                                        :search (lower-case (str v)))]))
                 (into {})))))
