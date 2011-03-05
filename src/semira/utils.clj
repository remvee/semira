(ns semira.utils)

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
  [coll & ks]
  (sort-by (fn [val]
             (vec (map #(get val %)
                       ks)))
           coll))
