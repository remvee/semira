(ns semira.models
  (:use [semira.audio :as audio])
  (:import [java.io File]))

(def albums-container (atom []))

(defn albums [] @albums-container)

(defn- sha1
  "Calculate SHA1 digest for given string."
  [string]
  (let [digest (java.security.MessageDigest/getInstance "SHA1")
        string  (java.io.StringBufferInputStream. string)
        output (java.security.DigestInputStream. string digest)]
    (while (not= -1 (.read output)))
    (apply str (map #(format "%02x" %)
                    (.digest digest)))))

(defn- sort-by-keys
  "Sort coll of maps by value with ks defining precedence."
  [coll & ks]
  (sort-by (fn [val]
             (vec (map #(get val %)
                       ks)))
           coll))

(defn update-album [album]
  (swap! albums-container
         (fn [albums album]
           (sort-by-keys (conj (filter #(not= (:id album)
                                              (:id %))
                                       albums)
                               album)
                         :artist :year :album))
         album))

(defn normalize-album [album]
  (let [tracks (map #(merge album %) (:tracks album))
        common (filter #(and (not= :id %)
                             (apply = (map % tracks)))
                       (into #{} (flatten (map keys tracks))))]
    (merge
     (select-keys (first tracks) common)
     {:id (:id album)
      :tracks (vec (sort-by-keys (map #(apply dissoc % common)
                                      tracks)
                                 :track :path :title))})))

(defn update-track [track]
  (let [id (sha1 (:dir track))
        album (update-in (or (album-by-id id)
                             {:id id
                              :tracks []})
                         [:tracks]
                         (fn [tracks]
                           (conj (vec (filter #(not= (:id track)
                                                     (:id %))
                                              tracks))
                                 track)))]
    (update-album (normalize-album album))))

(defn update-file [file]
  (update-track (merge (audio/info file)
                       {:id (sha1 (.getPath file))
                        :dir (.getParent file)
                        :path (.getPath file)})))

(defn album-by-id [id]
  (first (filter #(= id (:id %))
                 (albums))))

(defn track-by-id [id]
  (first (filter #(= id (:id %))
                 (flatten (map :tracks (albums))))))

(comment
  (do
    (doseq [file (filter #(and (.isFile %)
                               (re-matches #".+\.(mp3|m4a|flac|ogg)"
                                           (.getName %)))
                         (file-seq (File. "/home/remco/Music")))]
      (update-file file))
    nil))