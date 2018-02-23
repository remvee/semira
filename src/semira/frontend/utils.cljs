;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.utils
  (:require [clojure.string :as string]))

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
        (sequential? val) (string/join ", " val)
        :else ".."))

(defn decode-uri-component [v]
  (.decodeURIComponent js/window v))

(defn encode-uri-component [v]
  (.encodeURIComponent js/window v))

(defn get-location-hash []
  (-> js/document .-location .-hash
      (subs 1)
      decode-uri-component))

(defn set-location-hash [v]
  (set! (-> js/document .-location .-hash)
        (str "#" (encode-uri-component v))))

(defn rect-in? [a b]
  (and (<= (:top a) (:top b) (:bottom b) (:bottom a))
       (<= (:left a) (:left b) (:right b) (:right a))))

(defn rect [el]
  (let [rect (.getBoundingClientRect el)]
    {:top    (.-top rect)
     :left   (.-left rect)
     :bottom (.-bottom rect)
     :right  (.-right rect)}))

(defn viewport-rect []
  (let [el (.-documentElement js/document)]
    {:top    (.-clientTop el)
     :left   (.-clientLeft el)
     :bottom (+ (.-clientTop el) (.-clientHeight el))
     :right  (+ (.-clientLeft el) (.-clientWidth el))}))

(defn fully-visible? [el]
  (rect-in? (viewport-rect) (rect el)))

(defn scroll-into-view [el]
  (.scrollIntoView el #js {:behavior "smooth"
                           :block    "start"}))

(defn scroll-into-view-if-needed [el]
  (when-not (fully-visible? el)
    (scroll-into-view el)))
