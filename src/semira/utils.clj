(ns semira.utils
  (:require [clojure.string :as string])
  (:import [java.io File]
           [java.util.regex Pattern]))

(defn sha1
  "Calculate SHA1 digest for given string."
  [string]
  (let [digest (java.security.MessageDigest/getInstance "SHA1")
        string  (java.io.StringBufferInputStream. string)
        output (java.security.DigestInputStream. string digest)]
    (while (not= -1 (.read output)))
    (apply str (map #(format "%02x" %)
                    (.digest digest)))))

(defn sort-by-keys
  "Sort coll of maps by value with ks defining precedence."
  [coll ks]
  (sort-by (fn [val] (vec (flatten (map #(get val %) ks)))) coll))

(defn mkdir-p
  "create directory dir, including all parent directories when they do not exist yet."
  [dir]
  (let [parts (string/split dir
                            (Pattern/compile (Pattern/quote File/separator)))]
    (doseq [path (rest (reductions #(str %1 File/separator %2) parts))]
      (when-not (-> path File. .isDirectory)
        (prn (-> path File. .mkdir)))))
  (File. dir))