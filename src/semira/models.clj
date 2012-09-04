(ns semira.models
  (:require [semira.audio :as audio]
            [semira.utils :as utils]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:import [java.io File FileInputStream FileOutputStream PushbackReader]))

(def albums-file (get (System/getenv) "SEMIRA_ALBUMS_SEXP" "/var/lib/semira.sexp"))
(def music-dir (get (System/getenv) "SEMIRA_MUSIC_DIR" "/var/lib/semira"))

(def ^:dynamic *albums*
  (atom
   (try
     (with-open [r (PushbackReader.
                    (io/reader (FileInputStream.
                                albums-file)))]
       (binding [*in* r]
         (read)))
     (catch Exception e
       (do (prn e)
           [])))))

(def backup-agent (agent albums-file))

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
  (assoc album :doc (reduce (fn [m [p r]] (s/replace m p r))
                            (.toLowerCase (str (:genre album) " "
                                               (:composer album) " "
                                               (:artist album) " "
                                               (:album album) " "
                                               (:year album)))
                            [[#"\s+" " "]
                             [#"^\s|\s$" ""]])))

(defn normalize-album [album]
  (let [tracks (map #(merge album %) (:tracks album))
        common (filter #(apply = (map % tracks))
                       (disj (into #{} (flatten (map keys tracks)))
                             :id :mtime :path :title :track :length))]
    (merge
     (select-keys (first tracks) common)
     {:tracks (vec (utils/sort-by-keys (map #(apply dissoc % common)
                                            tracks)
                                       [:track :path :title]))}
     (select-keys album [:id :mtime]))))

(defn update-track [albums track]
  (let [id (utils/sha1 (:dir track))
        album (-> (update-in (or (first (filter #(= id (:id %)) albums))
                                 {:id id
                                  :tracks []})
                             [:tracks]
                             (fn [tracks]
                               (conj (vec (remove #(= (:id track)
                                                      (:id %))
                                                  tracks))
                                     track)))
                  normalize-album
                  doc-album
                  mtime-album)]
    (conj (vec (remove #(= (:id album) (:id %)) albums)) album)))

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
                                   (vec (remove #(= id (:id %))
                                                tracks)))))
                    albums))))

(defn remove-file! [file]
  (swap! *albums* remove-track
         (utils/sha1 (.getPath file))))

(defn hit-track! [track tstamp]
  (swap! *albums*
         (fn [albums]
           (if-let [album (first (filter #(some (partial = track) (:tracks %))
                                         albums))]
             (replace
              {album
               (update-in album [:tracks]
                          (partial replace
                                   {track
                                    (update-in track [:hits]
                                               #(if % (conj % tstamp)
                                                    [tstamp]))}))}
              albums)
             albums))))

(defn scan []
  (let [before (System/currentTimeMillis)]
    (doseq [file (filter #(and (.isFile %)
                               (re-matches #".+\.(mp3|m4a|flac|ogg)"
                                           (.getName %)))
                         (file-seq (File. music-dir)))]
      (println "updating:" file)
      (update-file! file))
    (println "scanned in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))

(defn purge []
  (let [before (System/currentTimeMillis)]
    (doseq [file (map #(File. %) (filter identity (map :path (flatten (map :tracks (deref *albums*))))))]
      (when-not (.exists file)
        (println "removing:" file)
        (remove-file! file)))
    (println "purged in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))
