(ns semira.frontend
  (:require
   [semira.frontend.audio :as audio]
   [semira.frontend.snippets :as snippets]
   [semira.frontend.state :as state]
   [semira.frontend.utils :as utils]
   [goog.events :as events]
   [goog.dom :as dom]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn albums-query []
  (let [input (utils/by-id "search-query")]
    (. input value)))

(defn album-container [id]
  (utils/by-id (str "album-" id)))

(defn album-busy [id state]
  (let [album (dom/getAncestorByTagNameAndClass (album-container id) "li" "album")]
    (utils/busy album state)))

(defn album-collapse [id]
  (album-busy id false)
  (utils/inner-html (album-container id) ""))

(defn albums-update [albums]
  (utils/busy (utils/by-id "albums-more") false)
  (utils/inner-html (utils/by-id "albums")
                    (utils/htmlify (snippets/album-rows albums))))

(defn album-update [album]
  (album-busy (:id album) false)
  (utils/inner-html (album-container (:id album))
                    (utils/htmlify (snippets/album album))))

(defn ^:export albums-more []
  (utils/busy (utils/by-id "albums-more") true)
  (state/more-albums (albums-query) albums-update))

(defn ^:export album-toggle [id]
  (album-busy id true)
  (if (utils/first-by-tag-class "ol" "tracks" (album-container id))
    (album-collapse id)
    (state/album id album-update)))

(defn ^:export track-play [id]
  (audio/play id))

(defn ^:export track-queue [id]
  (audio/add id))

(events/listen (utils/by-id "search")
               goog.events.EventType/SUBMIT
               #(do (. % (preventDefault))
                    (state/clear-albums)
                    (albums-more)))

(albums-more)

;; dev: cp lib/hiccups-0.1.1.jar ~/lib/clojurescript/lib; rm -rf public/js/semira*; cljsc src '{:output-dir "public/js/semira"}' > public/js/semira.js
;; prod: cp lib/hiccups-0.1.1.jar ~/lib/clojurescript/lib; rm -rf public/js/semira*; cljsc src '{:output-dir "public/js/semira" :optimizations :advanced}' > public/js/semira.js
;; deploy:

;; cljs bug: cljs.reader.read_string("\"some \\\"quoted\\\" string\"")