(ns semira.models
  (:use [semira.audio :as audio]
        [semira.utils :as utils]
        [clojure.java.io :as io])
  (:import [java.io File FileInputStream FileOutputStream PushbackReader]))

(def *albums-file* "/tmp/semira.sexp")

(def *albums* (atom
               (try
                 (with-open [r (PushbackReader.
                                (io/reader (FileInputStream.
                                            *albums-file*)))]
                   (binding [*in* r]
                     (read)))
                 (catch Exception e
                   (do (prn e)
                       [])))))

(def backup-agent (agent *albums-file*))

(defn send-off-backup []
  (send-off backup-agent (fn [file]
                           (with-open [w (io/writer (FileOutputStream.
                                                     file))]
                             (binding [*out* w]
                               (pr (deref *albums*))))
                           file)))

(defn albums [& [{:keys [order query] :or {order []}}]]
  (let [query (and query (.toLowerCase query))
        f (if (or (nil? query) (= "" query))
            (fn [_] true)
            (fn [album]
              (if-let [v (:doc album)]
                (not= -1 (.indexOf v query)))))]
    (utils/sort-by-keys (filter f (deref *albums*))
                        order)))

(defn album-by-id [id]
  (first (filter #(= id (:id %))
                 (deref *albums*))))

(defn track-by-id [id]
  (first (filter #(= id (:id %))
                 (flatten (map :tracks (deref *albums*))))))

(defn- update-album [albums album]
)

(defn- doc-album [album]
  (assoc album :doc (.toLowerCase (str (:artist album) " "
                                       (:album album) " "
                                       (:genre album) " "
                                       (:year album)))))

(defn- normalize-album [album]
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

(defn update-track [albums track]
  (let [id (utils/sha1 (:dir track))
        album (doc-album
               (normalize-album
                (update-in (or (first (filter #(= id (:id %)) albums))
                               {:id id
                                :tracks []})
                           [:tracks]
                           (fn [tracks]
                             (conj (vec (filter #(not= (:id track)
                                                       (:id %))
                                                tracks))
                                   track)))))]
    (conj (filter #(not= (:id album) (:id %)) albums) album)))

(defn update-file! [file]
  (swap! *albums* update-track
         (merge (audio/info file)
                {:id (utils/sha1 (.getPath file))
                 :dir (.getParent file)
                 :path (.getPath file)})))

(defn scan []
  (let [before (System/currentTimeMillis)]
    (doseq [file (filter #(and (.isFile %)
                               (re-matches #".+\.(mp3|m4a|flac|ogg)"
                                           (.getName %)))
                         (file-seq (File. "/home/remco/Music")))]
      (prn file)
      (update-file! file))
    (println "scanned plenty of files in" (- (System/currentTimeMillis) before)))
  (send-off-backup))
