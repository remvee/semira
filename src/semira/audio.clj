(ns semira.audio
  (:import [java.io StringBufferInputStream]
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