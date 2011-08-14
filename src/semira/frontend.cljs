(ns semira.frontend
  (:require-macros
   [hiccups.core :as hiccups])
  
  (:require
   [semira.frontend.snippets :as snippets]

   [cljs.reader :as reader]

   [goog.net.XhrIo :as xhr]
   [goog.events :as events]
   [goog.dom :as dom]
   [goog.uri.utils :as uri-utils]
   
   [hiccups.runtime :as hiccupsrt]))

(defn htmlify [v]
  (hiccups/html v))

(defn map->js [m]
  (let [out (js-obj)]
    (doseq [[k v] m] (aset out (name k) v))
    out))

(defn remote-get [url callback]
  (let [xhr (goog.net.XhrIo.)]
    (events/listen xhr goog.net.EventType/COMPLETE (fn [] (callback (reader/read-string (. xhr (getResponseText))))))
    (. xhr (send url "GET" "" (map->js {"Content-Type" "application/clojure; charset=utf-8"})))))

(def by-id dom/getElement)

(def by-tag-class dom/getElementsByTagNameAndClass)

(defn first-by-tag-class [tag class elm]
  (aget (by-tag-class tag class elm) 0))

(defn inner-html [elm html]
  (set! (. elm innerHTML) html))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def albums-list (atom ()))

(defn albums-query []
  (let [input (by-id "search-query")]
    (. input value)))

(defn albums-update []
  (inner-html (by-id "albums")
              (htmlify (snippets/album-rows @albums-list))))

(defn ^:export albums-more []
  (let [uri (-> "/albums"
                (uri-utils/appendParam "limit" 5)
                (uri-utils/appendParam "offset" (count @albums-list))
                (uri-utils/appendParam "query" (albums-query)))]
    (remote-get uri
                #(do (swap! albums-list concat %)
                     (albums-update)))))

(defn ^:export album-toggle [id]
  (let [row (by-id (str "album-" id))
        container (first-by-tag-class "div" "album" row)]
    (if (first-by-tag-class "ol" "tracks" container)
      (inner-html container "")
      (remote-get (str "album/" id)
                  #(inner-html container
                               (htmlify (snippets/album %)))))))

(events/listen (by-id "search")
               goog.events.EventType/SUBMIT
               #(do (. % (preventDefault))
                    (reset! albums-list nil)
                    (albums-more)))

(albums-more)

;; shell: cp lib/hiccups-0.1.1.jar ~/lib/clojurescript/lib; rm -rf public/js/semira*; cljsc src '{:output-dir "public/js/semira"}' > public/js/semira.js

;; cljs bug: cljs.reader.read_string("\"some \\\"quoted\\\" string\"")