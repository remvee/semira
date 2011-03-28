(ns semira.utils
  (:require [clojure.string :as string])
  (:import [java.io File StringBufferInputStream]
           [java.util.regex Pattern]
           [java.security MessageDigest DigestInputStream]))

(defn sha1
  "Calculate SHA1 digest for given string."
  [string]
  (let [digest (MessageDigest/getInstance "SHA1")]
    (with-open [sbin  (StringBufferInputStream. string)]
      (with-open [din (DigestInputStream. sbin digest)]
        (while (pos? (.read din))))
      (apply str (map #(format "%02x" %) (.digest digest))))))

(defn sort-by-keys
  "Sort coll of maps by value with ks defining precedence."
  [coll ks]
  (sort-by (fn [val] (vec (flatten (map #(get val %) ks)))) coll))

(defn mkdir-p
  "Create directory dir, including all parent directories when they do not exist yet."
  [dir]
  (let [parts (string/split dir
                            (Pattern/compile (Pattern/quote File/separator)))]
    (doseq [path (rest (reductions #(str %1 File/separator %2) parts))]
      (when-not (-> path File. .isDirectory)
        (-> path File. .mkdir))))
  (File. dir))