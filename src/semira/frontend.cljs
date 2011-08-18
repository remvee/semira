(ns semira.frontend
  (:require
   [semira.frontend.audio :as audio]
   [semira.frontend.snippets :as snippets]
   [semira.frontend.state :as state]
   [semira.frontend.utils :as utils]
   [goog.events :as events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn albums-query []
  (let [input (utils/by-id "search-query")]
    (. input value)))

(defn albums-update [albums]
  (utils/inner-html (utils/by-id "albums")
                    (utils/htmlify (snippets/album-rows albums))))

(defn album-update [album]
  (utils/inner-html (utils/by-id (str "album-" (:id album)))
                    (utils/htmlify (snippets/album album))))

(defn ^:export albums-more []
  (state/more-albums (albums-query) albums-update))

(defn ^:export album-toggle [id]
  (let [container (utils/by-id (str "album-" id))]
    (if (utils/first-by-tag-class "ol" "tracks" container)
      (utils/inner-html container "")
      (state/album id album-update))))

(defn ^:export track-play [id]
  (audio/load id))

(events/listen (utils/by-id "search")
               goog.events.EventType/SUBMIT
               #(do (. % (preventDefault))
                    (state/clear-albums)
                    (albums-more)))

(albums-more)

;; shell: cp lib/hiccups-0.1.1.jar ~/lib/clojurescript/lib; rm -rf public/js/semira*; cljsc src '{:output-dir "public/js/semira"}' > public/js/semira.js

;; cljs bug: cljs.reader.read_string("\"some \\\"quoted\\\" string\"")