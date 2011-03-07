(ns semira.models
  (:use [semira.audio :as audio]
        [semira.utils :as utils])
  (:import [java.io File]))

(def *albums-file* "/tmp/semira.sexp")

(def *page-size* 20)

(def *albums* (atom (try (read-string (slurp *albums-file*))
                         (catch Exception _ []))))

(def backup-agent ^{:private true} (agent *albums-file*))

(defn send-off-backup []
  (send-off backup-agent (fn [file]
                           (spit file (pr-str (deref *albums*)))
                           file)))

(defn albums [& [{:keys [page order] :or {page 0, order []}}]]
  (take *page-size*
        (drop (* page *page-size*)
              (utils/sort-by-keys (deref *albums*)
                                  order))))

(defn album-by-id [id]
  (first (filter #(= id (:id %))
                 (deref *albums*))))

(defn track-by-id [id]
  (first (filter #(= id (:id %))
                 (flatten (map :tracks (deref *albums*))))))

(defn update-album [album]
  (swap! *albums*
         (fn [albums album]
           (conj (filter #(not= (:id album) (:id %)) albums)
                 album))
         album))

(defn normalize-album [album]
  (let [tracks (map #(merge album %) (:tracks album))
        common (filter #(and (not= :id %)
                             (apply = (map % tracks)))
                       (into #{} (flatten (map keys tracks))))]
    (merge
     (select-keys (first tracks) common)
     {:id (:id album)
      :tracks (vec (utils/sort-by-keys (map #(apply dissoc % common)
                                            tracks)
                                       [:track :path :title]))})))

(defn update-track [track]
  (let [id (utils/sha1 (:dir track))
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
                       {:id (utils/sha1 (.getPath file))
                        :dir (.getParent file)
                        :path (.getPath file)})))

(defn scan []
  (doseq [file (filter #(and (.isFile %)
                             (re-matches #".+\.(mp3|m4a|flac|ogg)"
                                         (.getName %)))
                       (file-seq (File. "/home/remco/Music")))]
    (update-file file))
  (send-off-backup))