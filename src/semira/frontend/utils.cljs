;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.utils
  (:require
   [semira.frontend.html :as html]
   [cljs.reader :as reader]
   [goog.dom :as gdom]
   [goog.dom.classes :as gclasses]
   [goog.events :as gevents]
   [goog.string :as gstring]
   [goog.net.XhrIo :as gxhr]))

(def gevent-type goog.events.EventType)
(def gnet-event-type goog.net.EventType)

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
  "humanize value"
  [val]
  (cond (string? val) val
        (sequential? val) (apply str (interpose ", " val))
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

(defn map->js [m]
  (let [out (js-obj)]
    (doseq [[k v] m] (aset out (name k) v))
    out))

(defn remote-get [url callback]
  (let [gxhr (goog.net.XhrIo.)]
    (gevents/listen gxhr (. gnet-event-type -COMPLETE) (fn [] (callback (reader/read-string (. gxhr (getResponseText))))))
    (. gxhr (send url "GET" "" (map->js {"Content-Type" "application/clojure; charset=utf-8"})))))

(def by-id gdom/getElement)

(def by-tag-class gdom/getElementsByTagNameAndClass)

(defn first-by-tag-class [tag class elm]
  (aget (by-tag-class tag class elm) 0))

(defn inner-html [elm html]
  (set! (. elm -innerHTML) html))

(defn busy [elm state]
  (when elm
    (gclasses/enable elm "busy" state)))

(defn scroll-into-view [elm]
  (when elm
    (. elm (scrollIntoView))))
