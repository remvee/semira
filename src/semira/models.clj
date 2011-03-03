(ns semira.models
  (:use [semira.audio :as audio])
  (:import [java.io File]))

(def albums-container (atom []))

(defn albums [] @albums-container)

(defn- sha1 [value]
  (let [digest (java.security.MessageDigest/getInstance "SHA1")
        value  (java.io.StringBufferInputStream. value)
        output (java.security.DigestInputStream. value digest)]
    (while (not= -1 (.read output)))
    (apply str (map #(format "%02x" %)
                    (.digest digest)))))

(defn update-album [album]
  (let [tracks (map #(merge album %) (:tracks album))
        common (filter #(apply = (map % tracks))
                       (into #{} (flatten (map keys tracks))))
        album (merge
               (select-keys (first tracks) common)
               {:tracks (sort (fn [a b]
                                (cond ; TODO make prettier
                                 (and (:track a) (:track b))
                                 (.compareTo (:track a) (:track b))

                                 (and (:path a) (:path b))
                                 (.compareTo (:path a) (:path b))

                                 :else 0))
                              (map #(apply dissoc % common) tracks))}
               {:id (sha1 (:dir (first tracks)))})]
    (swap! albums-container
           (fn [albums album]
             (sort (fn [a b]
                     (.compareTo (apply str (map #(% a) [:artist :year :album]))
                                 (apply str (map #(% b) [:artist :year :album]))))
                   (conj (filter #(not= (:dir album)
                                        (:dir %))
                                 albums)
                         album))) album)))

(defn update-track [track] ; TODO should happen in update-album swap!
  (let [dir (-> (:path track) File. .getParent)
        cur (first (filter #(= dir (:dir %))
                           (albums)))]
    (update-album
     (if cur
       (update-in cur [:tracks]
                  (fn [tracks track]
                    (conj (vec (filter #(not= (:path track)
                                              (:path %)) tracks))
                          track)) track)
       (into {:dir dir
              :tracks [track]}
             (filter last
                     {:dir dir
                      :album (:album track)
                      :year (:year track)
                      :genre (:genre track)}))))))

(defn update-file [file]
  (update-track (merge (audio/info file)
                       {:path (.getPath file)
                        :id (sha1 (.getPath file))})))

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