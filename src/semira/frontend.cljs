(ns semira.frontend
  (:require
   [semira.frontend.audio :as audio]
   [semira.frontend.snippets :as snippets]
   [semira.frontend.utils :as utils]
   [goog.events :as events]
   [goog.uri.utils :as uri-utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def albums-list (atom ()))

(defn albums-query []
  (let [input (utils/by-id "search-query")]
    (. input value)))

(defn albums-update []
  (utils/inner-html (utils/by-id "albums")
                    (utils/htmlify (snippets/album-rows @albums-list))))

(defn ^:export albums-more []
  (let [uri (-> "/albums"
                (uri-utils/appendParam "offset" (count @albums-list))
                (uri-utils/appendParam "query" (albums-query)))]
    (utils/remote-get uri
                      #(do (swap! albums-list concat %)
                           (albums-update)))))

(defn ^:export album-toggle [id]
  (let [row (utils/by-id (str "album-" id))
        container (utils/first-by-tag-class "div" "album" row)]
    (if (utils/first-by-tag-class "ol" "tracks" container)
      (utils/inner-html container "")
      (utils/remote-get (str "album/" id)
                        #(utils/inner-html container
                                     (utils/htmlify (snippets/album %)))))))

(events/listen (utils/by-id "search")
               goog.events.EventType/SUBMIT
               #(do (. % (preventDefault))
                    (reset! albums-list nil)
                    (albums-more)))

(albums-more)

;; shell: cp lib/hiccups-0.1.1.jar ~/lib/clojurescript/lib; rm -rf public/js/semira*; cljsc src '{:output-dir "public/js/semira"}' > public/js/semira.js

;; cljs bug: cljs.reader.read_string("\"some \\\"quoted\\\" string\"")