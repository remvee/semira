(ns semira.frontend.utils
  (:require
   [cljs.reader :as reader]
   [goog.dom :as dom]
   [goog.dom.classes :as dom-classes]
   [goog.events :as events]
   [goog.string :as gstring]
   [goog.net.XhrIo :as xhr]
   [hiccups.runtime :as hiccupsrt])
  (:require-macros
   [hiccups.core :as hiccups]))

(defn debug [& args]
  (js/console.log (pr-str args)))

(defn escape-html [value]
  (gstring/htmlEscape value))

(defn seconds->time
  "Write seconds as HH:MM:SS."
  [i]
  (if i
    (let [hours (quot i 3600)
          minutes (quot (rem i 3600) 60)
          seconds (rem i 60)
          padded (fn [n] (if (< n 10) (str "0" n) (str n)))]
      (cond (not= 0 hours)   (str hours ":" (padded minutes) ":" (padded seconds))
            (not= 0 minutes) (str minutes ":" (padded seconds))
            :else            (str ":" (padded seconds))))))

(defn h
  "HTMLize value"
  [val]
  (cond (string? val) (escape-html val)
        (sequential? val) (escape-html (apply str (interpose ", " val)))
        :else ".."))

(defn interposed-html
  "Interposes values of a map into HTML format."
  [rec sep ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (h (rec %))])
                          (filter #(rec %) ks)))]
    (if (seq r) r "..")))

(defn map->query-string
  "Make query string from map."
  [m]
  (apply str (interpose "&" (map (fn [[k v]] (str (name k) "=" v)) m))))

(def ^:dynamic *page-size* 20)
(defn take-page
  "Take the nth page of a collection.  Page size is determined by *page-size*."
  [coll page]
  (take *page-size*
        (drop (* page *page-size*) coll)))


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

(defn busy [elm state]
  (when elm
    (dom-classes/enable elm "busy" state)))