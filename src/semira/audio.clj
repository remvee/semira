(ns semira.audio
  (:import [java.io StringBufferInputStream]
           [java.util.logging LogManager]
           [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey TagField TagTextField]))

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
  (and (first v)
       (re-matches #"\d+" (first v))
       (Integer/valueOf (first v))))

(def procs {:track       to-i
            :track_total to-i
            :disc_no     to-i
            :disc_total  to-i
            :album       first
            :title       first
            :year        first
            :conductor   first})

(defmulti field-str class)
(defmethod field-str TagTextField
  [field] (.getContent field))
(defmethod field-str TagField
  [field] (.toString field))

(defn info
  "Pull meta data from audio file."
  [file]
  (let [audio (AudioFileIO/read file)
        tag (.getTag audio)
        header (.getAudioHeader audio)]
    (if (and tag header)
      (into {:length (.getTrackLength header)
             :encoding (.getEncodingType header)}
            (map (fn [[k v]] [k ((get procs k identity) v)])
                 (filter #(seq (last %))
                         (map (fn [k]
                                (let [fs (.getFields tag
                                                     (FieldKey/valueOf (.toUpperCase (name k))))]
                                  [k (vec (map field-str fs))]))
                              fields)))))))