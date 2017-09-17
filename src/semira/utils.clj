;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.utils
  (:require [clojure.string :as string]
            [hiccup.util :refer :all])
  (:import [java.io File StringBufferInputStream]
           [java.security DigestInputStream MessageDigest]
           java.util.regex.Pattern))

(defn sha1
  "Calculate SHA1 digest for given string."
  [string]
  (let [digest (MessageDigest/getInstance "SHA1")]
    (with-open [sbin  (StringBufferInputStream. string)]
      (with-open [din (DigestInputStream. sbin digest)]
        (while (pos? (.read din))))
      (string/join (map #(format "%02x" %) (.digest digest))))))

(defn sort-by-keys
  "Sort coll of maps by value with ks defining precedence."
  [ks coll]
  (sort-by (fn [val] (vec (flatten (map #(get val %) ks)))) coll))

(defn mkdirs
  "Create directory dir, including all parent directories when they do not exist yet."
  [dir]
  (let [parts (string/split dir
                            (Pattern/compile (Pattern/quote File/separator)))]
    (doseq [path (rest (reductions #(str %1 File/separator %2) parts))]
      (when-not (-> path File. .isDirectory)
        (-> path File. .mkdir))))
  (File. dir))

(defn seconds->time
  "Write seconds as HH:MM:SS."
  [i]
  (if i
    (let [hours (quot i 3600)
          minutes (quot (rem i 3600) 60)
          seconds (rem i 60)]
      (cond (not= 0 hours)   (format "%d:%02d:%02d" hours minutes seconds)
            (not= 0 minutes) (format "%d:%02d" minutes seconds)
            :else            (format ":%02d" seconds)))))

(defmulti h "HTMLize value" class)
(defmethod h java.util.List [val] (escape-html (string/join ", " val)))
(defmethod h :default [val] (escape-html (str val)))

(defn interposed-html
  "Interposes values of a map into HTML format."
  [rec sep ks]
  (let [r (interpose sep
                     (map #(vec [:span {:class (name %)} (h (rec %))])
                          (filter rec ks)))]
    (if (seq r) r "..")))

(defn map->query-string
  "Make query string from map."
  [m]
  (string/join "&" (map (fn [[k v]] (str (name k) "=" v)) m)))
