(ns semira.frontend
  (:require
   [semira.frontend.audio :as audio]
   [semira.frontend.html :as html]
   [semira.frontend.state :as state]
   [semira.frontend.utils :as utils]
   [goog.events :as events]
   [goog.dom :as dom]
   [goog.dom.classes :as dom-classes]))

(def page-size 15)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare album-listing album-row album-more-row album-not-found)

(defn albums-query []
  (. (utils/by-id "search-query") value))

(let [query (utils/url-decode (.replace (.. js/window location hash) #"^#" ""))]
  (set! (. (utils/by-id "search-query") value) query))

(defn album-container [id]
  (utils/by-id (str "album-" id)))

(defn album-busy [id state]
  (let [album (dom/getAncestorByTagNameAndClass (album-container id) "li" "album")]
    (utils/busy album state)))

(defn album-collapse [id]
  (album-busy id false)
  (dom/removeChildren (album-container id)))

(defn albums-update [albums & {offset :offset end-reached :end-reached}]
  (doseq [id ["albums-more" "search-query"]]
    (utils/busy (utils/by-id id) false))

  (let [container (utils/by-id "albums")]
    (cond (empty? albums)
          (do (dom/removeChildren container)
              (dom/append container (html/build (album-not-found))))

          (or (nil? offset) (= offset 0))
          (do
            (dom/removeChildren container)
            (doseq [row (map album-row albums)]
              (dom/append container (html/build row))))

          :else
          (do
            (when (utils/by-id "albums-more")
              (dom/removeNode (utils/by-id "albums-more")))
            (doseq [row (map album-row (drop offset albums))]
              (dom/append container (html/build row)))
            (utils/scroll-into-view (str "album-"
                                         (:id (first (drop (dec offset) albums)))))))
    (when-not end-reached
      (dom/append container (html/build (album-more-row))))))

(defn album-update [album]
  (album-busy (:id album) false)
  (let [container (album-container (:id album))]
    (dom/removeChildren container)
    (dom/append container (html/build (album-listing album)))))

(defn albums-more []
  (utils/busy (utils/by-id "albums-more") true)
  (state/more-albums (albums-query) page-size albums-update))

(albums-more)

(defn album-toggle [id]
  (album-busy id true)
  (if (utils/first-by-tag-class "ol" "tracks" (album-container id))
    (album-collapse id)
    (state/album id album-update)))

(defn album-search []
  (utils/busy (utils/by-id "search-query") true)
  (set! (.. js/window location hash) (utils/url-encode (albums-query)))
  (state/clear-albums)
  (albums-more))

(events/listen (utils/by-id "search")
               goog.events.EventType/SUBMIT
               #(do (. % (preventDefault))
                    (album-search)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track-play [tracks]
  (if (= (first tracks) (audio/current))
    (audio/play-pause)
    (audio/play tracks)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-current-state []
  (when-let [track (utils/by-id (str "track-" (:id (audio/current))))]
    (dom-classes/enable track "playing" (audio/playing?))
    (utils/busy track (and (audio/loading?))))
  (when-let [album (utils/by-id (str "album-" (:album-id (audio/current))))]
    (dom-classes/enable (.parentNode album) "playing" (audio/playing?))))

(defn update-current-time []
  (if-let [track-current-time (utils/by-id (str "track-current-time-" (:id (audio/current))))]
    (utils/inner-html
     track-current-time
     (if (audio/playing?)
       (str (utils/seconds->time (js/Math.round (audio/current-time))) " / ")
       ""))))

(swap! audio/update-current-fns conj update-current-state)
(swap! audio/update-current-fns conj update-current-time)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn track-row [track album]
  (let [id (:id track)]
    [:li.track {:id (str "track-" id)
                :onclick #(track-play (drop-while (fn [x] (not= id (:id x)))
                                                  (map (fn [x] {:id (:id x) :album-id (:id album)})
                                                       (:tracks album))))}
     [:span.title
      (utils/interposed-html track " / " [:artist :album :title])]
     " "
     [:span.status {:id (str "track-status-" id)}]
     [:span.length
      [:span.played {:id (str "track-current-time-" id)}]
      [:span.full (utils/seconds->time (:length track))]]]))

(defn album-listing [album]
  [:ol.tracks
   (map (fn [t] (track-row t album)) (:tracks album))])

(defn album-row [album]
  [:li.album
   [:div.album-info  {:onclick #(album-toggle (:id album))}
    (utils/interposed-html album " " [:year :genre :artist :album])]
   " "
   [:div.album  {:id (str "album-" (:id album))}]])

(defn album-not-found []
  [:li.not-found "not found.."])

(defn album-more-row []
  [:li#albums-more.more {:onclick albums-more}
   [:img {:src "/images/more.png" :alt "&rarr;"}]])
