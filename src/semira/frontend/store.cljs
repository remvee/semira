(ns semira.frontend.store
  (:require [cljs.core.async :as async]
            [semira.frontend.idb :as idb])
  (:require-macros [cljs.core.async.macros :as async]))

(defn -track-uri [{:keys [id]} type]
  (str "/stream/track/" id "."
       (get {"audio/mpeg" "mp3"
             "audio/ogg" "ogg"}
            type)))

(defonce db (atom nil))

(defn- http-success? [xhr]
  (<= 200 (.-status xhr) ))

(defn track-uri [{:keys [id] :as track} type]
  (let [out (async/chan)]
    (async/go
      (if-let [data (async/<! (idb/get @db "tracks" id))]
        (do
          (prn :store-hit id)
          (async/put! out (.createObjectURL js/URL data)))
        (do
          (prn :store-miss id)
          (let [xhr (js/XMLHttpRequest.)
                url (-track-uri track type)]
            (.open xhr "GET" url)
            (set! (.-responseType xhr) "blob")
            (set! (.-onload xhr)
                  #(when (= 200 (.-status xhr))
                     (async/put! out
                                 (.createObjectURL js/URL (.-response xhr)))
                     (idb/put @db "tracks" id (.-response xhr))))
            (.send xhr)))))
    out))

(async/go
  (reset! db
          (async/<! (idb/open "semira"
                              [{:name "tracks"}]))))
