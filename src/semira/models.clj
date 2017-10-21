;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.models
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [semira.audio :as audio]
            [semira.utils :as utils])
  (:import [java.io File FileInputStream FileOutputStream PushbackReader]))

(def ^:private albums-file (get (System/getenv) "SEMIRA_ALBUMS_SEXP"
                                "/tmp/semira.sexp"))
(def ^:private music-dir (get (System/getenv) "SEMIRA_MUSIC_DIR"
                              (str (get (System/getenv) "HOME") "/Music")))

(defonce ^:private
  album-store (atom (try
                      (read-string (slurp albums-file))
                      (catch Exception _ []))))

(defonce ^:private
  backup-agent (agent nil))

(defn- send-off-backup []
  (send-off backup-agent (fn [_] (spit albums-file (pr-str @album-store)))))

(defn albums []
  @album-store)

(defn album-by-id [id albums]
  (->> albums (filter #(= id (:id %))) first))

(defn track-by-id [id albums]
  (->> albums (mapcat :tracks) (filter #(= id (:id %))) first))

(defn- search-index-album [album]
  (assoc album
         :search-index
         (reduce (fn [m [p r]] (s/replace m p r))
                 (.toLowerCase (str (:genre album) " "
                                    (:composer album) " "
                                    (:artist album) " "
                                    (:album album) " "
                                    (:year album)))
                 [[#"\s+" " "]
                  [#"^\s|\s$" ""]
                  [#"[^\w ]" ""]])))

(defn mtime-album [album]
  (if-let [mtime (->> album :tracks (map :mtime) (filter identity) sort last)]
    (assoc album :mtime mtime)
    album))

(defn normalize-album [album]
  (let [full-tracks       (map #(merge album %) (:tracks album))
        common-value-keys (filter #(apply = (map % full-tracks))
                                  (disj (set (mapcat keys full-tracks))
                                        :id :mtime :path :title :track :length))]
    (merge (select-keys (first full-tracks) common-value-keys)
           {:tracks (->> full-tracks
                         (map #(apply dissoc % common-value-keys))
                         (utils/sort-by-keys [:track :path :title])
                         vec)}
           (select-keys album [:id :mtime]))))

(defn update-track [albums album-id track]
  (let [album (-> (update (or (->> albums (filter #(= album-id (:id %))) first)
                              {:id album-id, :tracks []})
                          :tracks (fn [tracks]
                                    (conj (filterv #(not= (:id track) (:id %)) tracks)
                                          track)))
                  normalize-album
                  search-index-album
                  mtime-album)]
    (conj (filterv #(not= album-id (:id %)) albums)
          album)))

(defn- update-file! [file]
  (let [album-id (utils/sha1 (.getParent file))
        track    (merge (audio/info file)
                        {:id    (utils/sha1 (.getPath file))
                         :path  (.getPath file)
                         :mtime (.lastModified file)})]
    (swap! album-store
           (fn [albums]
             (update-track albums album-id track)))))

(def ^:private audio-file-re #".+\.(mp3|m4a|flac|ogg)")

(defn scan []
  (let [before (System/currentTimeMillis)]
    (doseq [file (->> (File. music-dir)
                      file-seq
                      (filter #(and (.isFile %)
                                    (re-matches audio-file-re (.getName %)))))]
      (log/info "updating:" file)
      (update-file! file))
    (log/info "scanned in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))

(defn- remove-track [albums id]
  (->> albums
       (map (fn [album]
              (update-in album
                         [:tracks]
                         (fn [tracks]
                           (vec (remove #(= id (:id %))
                                        tracks))))))
       (filter #(seq (:tracks %)))))

(defn- remove-file! [file]
  (swap! album-store remove-track
         (utils/sha1 (.getPath file))))

(defn purge []
  (let [before (System/currentTimeMillis)]
    (doseq [file (->> @album-store
                      (mapcat :tracks)
                      (map :path)
                      (filter identity)
                      (map #(File. %)))]
      (when-not (.exists file)
        (log/info "removing:" file)
        (remove-file! file)))
    (log/info "purged in" (- (System/currentTimeMillis) before) "ms"))
  (send-off-backup))
