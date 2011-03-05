(ns semira.stream
  (:use [clojure.java.io :as io]
        [semira.utils :as utils])
  (:import [java.io File FileInputStream PipedInputStream PipedOutputStream]))

(def *cache-dir* "tmp")

;; ensure cache directory exists
(utils/mkdir-p *cache-dir*)

(defn- cache-file [track]
  (str *cache-dir* File/separator (str "semira-" (:id track))))

(def conversions ^{:private true} (atom #{}))

(defn- convert [track]
  (let [decoder (condp re-matches (:path track)
                  #".*\.flac" "flacdec"
                  #".*\.ogg"  "oggdemux ! vorbisdec"
                  #".*\.mp3"  "ffdemux_mp3 ! ffdec_mp3"
                  #".*\.m4a"  "ffdemux_mov_mp4_m4a_3gp_3g2_mj2 ! faad")
        command ["gst-launch" "-q" "filesrc" "location=" (:path track)
                 "!" decoder "!" "audioconvert" "!" "vorbisenc" "quality=-0.1" "!" "oggmux" "!"
                 "filesink" "location=" (cache-file track)]
        process (-> (Runtime/getRuntime) (.exec (into-array command)))]

    ;; register running conversion
    (swap! conversions conj (:id track))

    ;; wait for conversion to finish and deregister it
    (.start
     (Thread.
      (fn []
        (.waitFor process)
        (swap! conversions disj (:id track))
        (when-not (= 0 (.exitValue process))
          (prn (format "ERROR: %s: %s"
                       command
                       (slurp (.getErrorStream process))))))))))

(defn- live-input [track]
  (let [pin (java.io.PipedInputStream.)
        pout (java.io.PipedOutputStream. pin)]
    (.start
     (Thread.
      (fn []
        ;; wait for file to appear
        (loop [n 100]
          (when (and (pos? n)
                     (not (-> (cache-file track) File. .canRead)))
            (Thread/sleep 100)
            (recur (dec n))))

        ;; read from file till conversion no longer running
        (with-open [in (FileInputStream. (cache-file track))]
          (loop []
            (when (@conversions (:id track))
              (if (pos? (.available in))
                (io/copy in pout)
                (Thread/sleep 100))
              (recur)))))))
    pin))

(defn input
  "Return a input stream of the given track."
  [track]
  (locking input
    (cond
     (@conversions (:id track))
     (live-input track)

     (-> (cache-file track) File. .canRead)
     (FileInputStream. (cache-file track))

     :else
     (do
       (convert track)
       (live-input track)))))