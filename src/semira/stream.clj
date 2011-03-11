(ns semira.stream
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [semira.utils :as utils])
  (:import [java.io File FileInputStream PipedInputStream PipedOutputStream]))

(def *cache-dir* "/tmp/semira")
(def *bitrate* 56)

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
  (let [file (cache-file track type)
        decoder (condp re-matches (:path track)
                  #".*\.flac" "flacdec"
                  #".*\.ogg"  "oggdemux ! vorbisdec"
                  #".*\.mp3"  "ffdemux_mp3 ! ffdec_mp3"
                  #".*\.m4a"  "ffdemux_mov_mp4_m4a_3gp_3g2_mj2 ! faad")
        encoder (condp = type
                    "audio/mpeg" [(str "lame bitrate=" *bitrate*) "!" "xingmux" "!" "id3mux"]
                    "audio/ogg" [(str "vorbisenc bitrate=" *bitrate*) "!" "oggmux"])
        command (flatten ["gst-launch" "-q"
                          "filesrc" "location=" (:path track) "!"
                          decoder "!"
                          "audioconvert" "!"
                          encoder "!"
                          "filesink" "location=" file])
        process (-> (Runtime/getRuntime) (.exec (into-array command)))]

    ;; register running conversion
    (swap! conversions conj file)

    ;; wait for conversion to finish and deregister it
    (.start
     (Thread.
      (fn []
        (.waitFor process)
        (swap! conversions disj file)
        (when-not (= 0 (.exitValue process))
          (prn (format "ERROR: %s: %s"
                       (apply str command)
                       (slurp (.getErrorStream process))))))))))

(defn- live-input [track type]
  (let [file (cache-file track type)
        pin (java.io.PipedInputStream.)
        pout (java.io.PipedOutputStream. pin)]
    (.start
     (Thread.
      (fn []
        ;; wait for file to appear
        (loop [n 100]
          (when (and (pos? n)
                     (not (-> file File. .canRead)))
            (Thread/sleep 100)
            (recur (dec n))))

        ;; read from file till conversion no longer running
        (with-open [in (FileInputStream. file)]
          (loop []
            (when (or (@conversions file)
                      (pos? (.available in)))
              (if (pos? (.available in))
                (io/copy in pout)
                (do
                  (Thread/sleep 100)))
              (recur)))))))
    pin))

(defn get
  "Return an input stream of type for the given track."
  [track type]
  (locking get
    (let [file (cache-file track type)]
      (cond
       (@conversions file)
       (live-input track type)

       (-> file File. .canRead)
       (FileInputStream. file)
       
       :else
       (do
         (convert track type)
         (live-input track type))))))

(defn length
  "Return the length of the stream when already known, otherwise nil."
  [track type]
  (let [file (File. (cache-file track type))]
    (and (.canRead file) (.length file))))