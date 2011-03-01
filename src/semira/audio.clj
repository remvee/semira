(ns semira.audio
  (:import [java.io File StringBufferInputStream]
           [java.util.logging LogManager]
           [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey]))

;; kill log messages from jaudiotagger
(.readConfiguration (LogManager/getLogManager)
                    (StringBufferInputStream. "org.jaudiotagger.level = OFF"))

(def fields [:artist
             :album
             :composer
             :conductor
             :disc_no
             :disc_total
             :genre
             :producer
             :remixer
             :title
             :track
             :track_total
             :year])

(defn to-i [v]
  (and v
       (re-matches #"\d+" v)
       (Integer/valueOf v)))

(def procs {:track       to-i
            :track_total to-i})

(defn info
  "Pull meta data from audio file."
  [file]
  (let [audio (AudioFileIO/read file)
        tag (.getTag audio)
        header (.getAudioHeader audio)]
    (if (and tag header)
      (into {:length (.getTrackLength header)
             :encoding (.getEncodingType header)}
            (filter (complement #(or (nil? (last %))
                                     (= "" (last %))))
                    (map (fn [v]
                           [v ((get procs v identity)
                               (.getFirst tag
                                          (FieldKey/valueOf (.toUpperCase (name v)))))])
                         fields))))))

(defn ogg-stream
  "Return a OGG Vorbis input stream of the given track."
  [file]
  (let [decoder (condp re-matches (.getPath file)
                  #".*\.flac" "flacdec"
                  #".*\.ogg"  "oggdemux ! vorbisdec"
                  #".*\.mp3"  "ffdemux_mp3 ! ffdec_mp3"
                  #".*\.m4a"  "ffdemux_mov_mp4_m4a_3gp_3g2_mj2 ! faad")
        process (-> (Runtime/getRuntime) (.exec (into-array ["gst-launch"
                                                             "-q"
                                                             "filesrc"
                                                             "location="
                                                             (.getPath file)
                                                             "!"
                                                             decoder
                                                             "!"
                                                             "audioconvert"
                                                             "!"
                                                             "vorbisenc"
                                                             "!"
                                                             "oggmux"
                                                             "!"
                                                             "fdsink"
                                                             "fd=1"])))]
    (.getInputStream process)))