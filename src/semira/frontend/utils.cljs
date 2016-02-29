;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.utils)

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
