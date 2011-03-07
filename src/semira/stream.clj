(ns semira.stream
  (:use [clojure.java.io :as io]
        [semira.utils :as utils])
  (:import [java.io File FileInputStream PipedInputStream PipedOutputStream]))

(def *cache-dir* "tmp")

;; ensure cache directory exists
(utils/mkdir-p *cache-dir*)

(defn- cache-file [track type]
  (str *cache-dir* File/separator (str "semira-" (:id track) "." type)))

(def conversions ^{:private true} (atom #{}))

(defn- convert [track type]
  (let [file (cache-file track type)
        decoder (condp re-matches (:path track)
                  #".*\.flac" "flacdec"
                  #".*\.ogg"  "oggdemux ! vorbisdec"
                  #".*\.mp3"  "ffdemux_mp3 ! ffdec_mp3"
                  #".*\.m4a"  "ffdemux_mov_mp4_m4a_3gp_3g2_mj2 ! faad")
        encoder (condp = type
                    :mp3 ["lame preset=56" "!" "id3v2mux"]
                    :ogg ["vorbisenc quality=-0.1" "!" "oggmux"])
        command (flatten ["gst-launch" "-q" "filesrc" "location=" (:path track)
                          "!" decoder "!" "audioconvert" "!" encoder "!"
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
                       command
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
            (when (@conversions file)
              (if (pos? (.available in))
                (io/copy in pout)
                (Thread/sleep 100))
              (recur)))))))
    pin))

(defn input
  "Return a input stream of the given track."
  [track type]
  (locking input
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