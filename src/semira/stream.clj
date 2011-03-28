(ns semira.stream
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [semira.utils :as utils])
  (:import [java.io File FileInputStream PipedInputStream PipedOutputStream]))

(def *cache-dir* "/tmp/semira")
(def *bitrate* 80)

;; ensure cache directory exists
(utils/mkdir-p *cache-dir*)

(defn- cache-file [track type]
  (str *cache-dir*
       File/separator
       (str "semira-"
            (:id track)
            "."
            (string/replace type #"[^a-z0-1]" ""))))

(def conversions ^{:private true} (atom #{}))

(defn- convert [track type]
  (let [filename (cache-file track type)
        decoder (condp re-matches (:path track)
                  #".*\.flac" "flacdec"
                  #".*\.ogg"  "oggdemux ! vorbisdec"
                  #".*\.mp3"  "ffdemux_mp3 ! ffdec_mp3"
                  #".*\.m4a"  "ffdemux_mov_mp4_m4a_3gp_3g2_mj2 ! faad")
        encoder (condp = type
                    "audio/mpeg" ["lame" "mode=1" (str "bitrate=" *bitrate*) "!" "xingmux" "!" "id3mux"]
                    "audio/ogg" ["vorbisenc" (str "bitrate=" (* *bitrate* 1000)) ")!" "oggmux"])
        command (flatten ["gst-launch" "-q"
                          "filesrc" "location=" (:path track) "!"
                          decoder "!"
                          "audioconvert" "!"
                          encoder "!"
                          "filesink" "location=" filename])
        process (-> (Runtime/getRuntime) (.exec (into-array command)))]

    ;; register running conversion
    (swap! conversions conj filename)

    ;; wait for conversion to finish and deregister it
    (.start
     (Thread.
      (fn []
        (.waitFor process)
        (swap! conversions disj filename)
        (when-not (= 0 (.exitValue process))
          (printf "ERROR: %s: %s"
                  (apply str command)
                  (slurp (.getErrorStream process)))))))))

(defn- live-input [track type]
  (let [filename (cache-file track type)
        pipe (PipedInputStream.)
        out (PipedOutputStream. pipe)]
    (.start
     (Thread.
      (fn []
        (try
          (do
            ;; wait for file to appear
            (loop [n 100]
              (when (and (pos? n)
                         (not (-> filename File. .canRead)))
                (Thread/sleep 100)
                (recur (dec n))))

            ;; read from file till conversion no longer running
            (with-open [in (FileInputStream. filename)]
              (loop []
                (when (@conversions filename)
                  (if (pos? (.available in))
                    (io/copy in out)
                    (Thread/sleep 100))
                  (recur)))

              ;; read remainer of file
              (io/copy in out)))
          (finally (.close out))))))
    pipe))

(defn- object-type [object & rest]
  (cond (:tracks object) :album
        (:path object)   :track))

(defmulti get
  "Return an input stream of given type for the given object."
  object-type)

(defmethod get :track [track type]
  (locking get
    (let [filename (cache-file track type)]
      (cond
       (@conversions filename)
       (live-input track type)

       (-> filename File. .canRead)
       (FileInputStream. filename)

       :else
       (do
         (convert track type)
         (live-input track type))))))

(defmethod get :album [album type]
  (let [pipe (PipedInputStream.)
        out (PipedOutputStream. pipe)]
    (.start
     (Thread.
      (fn []
        (try
          (doseq [track (:tracks album)]
            (with-open [in (get track type)]
              (io/copy in out)))
          (finally (.close out))))))
    pipe))

(defmulti length
  "Return the length of the stream when already known, otherwise nil."
  object-type)

(defmethod length :track [track type]
  (let [filename (cache-file track type)
        file (File. filename)]
    (and (not (@conversions filename)) (.canRead file) (.length file))))

(defmethod length :album [album type]
  (reduce #(when %1 (let [len (length %2 type)] (when len (+ %1 len))))
          0 (:tracks album)))