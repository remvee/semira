(ns semira.models
  (:use [semira.audio :as audio]
        [semira.utils :as utils]
        [clojure.java.io :as io])
  (:import [java.io File FileInputStream FileOutputStream PushbackReader]))

(def *albums-file* "/tmp/semira.sexp")

(def *albums*
  (atom
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
  (send-off backup-agent
            (fn [file]
              (with-open [w (io/writer (FileOutputStream. file))]
                (binding [*out* w] (pr (deref *albums*))))
              file)))

(defn albums [& [{:keys [query]}]]
  (let [query (and query (.toLowerCase query))
        f (if (or (nil? query) (= "" query))
            (fn [_] true)
            (fn [album]
              (if-let [v (:doc album)]
                (not= -1 (.indexOf v query)))))]
    (filter f (deref *albums*))))

(defn album-by-id [id]
  (first (filter #(= id (:id %))
                 (deref *albums*))))

(defn track-by-id [id]
  (first (filter #(= id (:id %))
                 (flatten (map :tracks (deref *albums*))))))

(defn- mtime-album [album]
  (if (:mtime album)
    album
    (assoc album :mtime (first
                         (filter identity
                                 (map :mtime (:tracks album)))))))

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
        album (-> (update-in (or (first (filter #(= id (:id %)) albums))
                                 {:id id
                                  :tracks []})
                             [:tracks]
                             (fn [tracks]
                               (conj (vec (filter #(not= (:id track)
                                                         (:id %))
                                                  tracks))
                                     track)))
                  normalize-album
                  doc-album
                  mtime-album)]
    (conj (vec (filter #(not= (:id album) (:id %)) albums)) album)))

(defn update-file! [file]
  (swap! *albums* update-track
         (merge (audio/info file)
                {:id (utils/sha1 (.getPath file))
                 :path (.getPath file)
                 :mtime (.lastModified file)
                 :dir (.getParent file)})))

(defn remove-track [albums id]
  (vec (filter #(not (empty? (:tracks %)))
               (map (fn [album]
                      (update-in album
                                 [:tracks]
                                 (fn [tracks]
                                   (vec (filter (fn [track]
                                                  (not= id (:id track)))
                                                tracks)))))
                    albums))))

(defn remove-file! [file]
  (swap! *albums* remove-track
         (utils/sha1 (.getPath file))))

(defn scan []
  (let [before (System/currentTimeMillis)]
    (doseq [file (filter #(and (.isFile %)
                               (re-matches #".+\.(mp3|m4a|flac|ogg)"
                                           (.getName %)))
                         (file-seq (File. "/home/remco/Music")))]
      (println "updating" file)
      (update-file! file))
    (println "scanned in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))

(defn purge []
  (let [before (System/currentTimeMillis)]
    (doseq [file (map #(File. %) (filter identity (map :path (flatten (map :tracks (deref *albums*))))))]
      (when-not (.exists file)
        (println "removing" file)
        (remove-file! file)))
    (println "purged in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))
