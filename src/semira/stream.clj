;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.stream
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [semira.mime-type :refer [type-by-file-name]]
            [semira.utils :as utils])
  (:import [java.io File FileInputStream IOException PipedInputStream PipedOutputStream]))

(def cache-dir (get (System/getenv) "SEMIRA_CACHE_DIR"
                    "/tmp/semira"))
(def ^:dynamic *bitrate* 96)

(defn- cache-file [track type]
  (str cache-dir
       File/separator
       (str "semira-"
            (:id track)
            "."
            (string/replace type #"[^a-z0-1]" ""))))

(def conversions ^{:private true} (atom #{}))

(defn convert-command [in-file out-file target-type]
  (let [decoder ({"audio/flac" ["flacparse" "!" "flacdec"]
                  "audio/ogg"  ["oggdemux" "!" "vorbisdec"]
                  "audio/mpeg" ["mpegaudioparse" "!" "mpg123audiodec"]
                  "audio/mp4"  ["qtdemux" "!" "faad"]}
                 (type-by-file-name in-file))
        encoder ({"audio/mpeg" ["lamemp3enc"
                                "target=bitrate" (str "bitrate=" *bitrate*)
                                "!" "xingmux" "!" "id3mux"]
                  "audio/ogg"  ["vorbisenc"
                                (str "bitrate=" (* *bitrate* 1000))
                                "!" "oggmux"]}
                 target-type)]
    (flatten ["gst-launch-1.0" "-q"
              "filesrc" "location=" in-file "!"
              decoder "!"
              "audioconvert" "!"
              encoder "!"
              "filesink" "location=" out-file])))

(defn- convert [track target-type]
  (utils/mkdirs cache-dir)

  (let [filename (cache-file track target-type)
        command  (convert-command (:path track) filename target-type)
        process  (.exec (Runtime/getRuntime) (into-array command))]
    (log/info "Started conversion: " command)

    ;; register running conversion
    (swap! conversions conj filename)

    ;; wait for conversion to finish and deregister it
    (async/thread
      (.waitFor process)
      (swap! conversions disj filename)
      (let [exit-value (.exitValue process)]
        (log/debug "Conversion finished: " command)
        (when-not (zero? exit-value)
          (log/error "conversion exited with non zero exit value" exit-value
                     "command:" (pr-str command)
                     "stdout:" (pr-str (slurp (.getInputStream process)))
                     "stderr:" (pr-str (slurp (.getErrorStream process)))))))))

(def ^:const wait-for-input-ms 100)

(defn- live-input [track type]
  (let [filename (cache-file track type)
        in       (PipedInputStream.)
        out      (PipedOutputStream. in)]
    (async/thread
      (try
        ;; wait for file to appear
        (loop [n 100]
          (when (and (pos? n)
                     (not (-> filename File. .canRead)))
            (Thread/sleep wait-for-input-ms)
            (recur (dec n))))

        ;; read till conversion no longer running
        (try
          (with-open [in (FileInputStream. filename)]
            (while (@conversions filename)
              (if (pos? (.available in))
                (io/copy in out)
                (Thread/sleep wait-for-input-ms)))

            ;; read remainer of file
            (loop []
              (Thread/sleep wait-for-input-ms)
              (when (pos? (.available in))
                (io/copy in out)
                (recur))))

          ;; ignore "Pipe closed" when stream is closed by browser
          (catch IOException _
            nil))
        (finally
          (try (.close out) (catch IOException _ nil)))))
    in))

(defn open [track type]
  (locking open
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

(defn length [track type]
  (let [filename (cache-file track type)
        file     (File. filename)]
    (when (and (not (@conversions filename)) (.canRead file))
      (.length file))))
