(ns semira.frontend.utils
  (:require [goog.string :as gstring]))

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
