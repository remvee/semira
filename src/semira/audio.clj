;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.audio
  (:require [clojure.string :as string])
  (:import java.io.StringBufferInputStream
           java.util.logging.LogManager
           org.jaudiotagger.audio.AudioFileIO
           [org.jaudiotagger.tag FieldKey Tag TagField TagTextField]
           [org.jaudiotagger.tag.mp4 Mp4FieldKey Mp4Tag]))

;; kill log messages from jaudiotagger
(.readConfiguration (LogManager/getLogManager)
                    (StringBufferInputStream. "org.jaudiotagger.level = OFF"))

(def id3v1-genres ["Blues", "Classic Rock", "Country", "Dance", "Disco",
                   "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age",
                   "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock",
                   "Techno", "Industrial", "Alternative", "Ska",
                   "Death Metal", "Pranks", "Soundtrack", "Euro-Techno",
                   "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion",
                   "Trance", "Classical", "Instrumental", "Acid", "House",
                   "Game", "Sound Clip", "Gospel", "Noise", "AlternRock",
                   "Bass", "Soul", "Punk", "Space", "Meditative",
                   "Instrumental Pop", "Instrumental Rock", "Ethnic",
                   "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
                   "Pop-Folk", "Eurodance", "Dream", "Southern Rock",
                   "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap",
                   "Pop/Funk", "Jungle", "Native American", "Cabaret",
                   "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer",
                   "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka",
                   "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk",
                   "Folk/Rock", "National Folk", "Swing", "Fast-Fusion",
                   "Bebob", "Latin", "Revival", "Celtic", "Bluegrass",
                   "Avantgarde", "Gothic Rock", "Progressive Rock",
                   "Psychedelic Rock", "Symphonic Rock", "Slow Rock",
                   "Big Band", "Chorus", "Easy Listening", "Acoustic",
                   "Humour", "Speech", "Chanson", "Opera", "Chamber Music",
                   "Sonata", "Symphony", "Booty Bass", "Primus",
                   "Porn Groove", "Satire", "Slow Jam", "Club", "Tango",
                   "Samba", "Folklore", "Ballad", "Power Ballad",
                   "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock",
                   "Drum Solo", "A capella", "Euro-House", "Dance Hall",
                   "Goa", "Drum & Bass", "Club House", "Hardcore", "Terror",
                   "Indie", "BritPop", "NegerPunk", "Polsk Punk", "Beat",
                   "Christian Gangsta", "Heavy Metal", "Black Metal",
                   "Crossover", "Contemporary C", "Christian Rock",
                   "Merengue", "Salsa", "Thrash Metal", "Anime", "JPop",
                   "SynthPop"])

(def fields [:artist
             :album
             :composer
             :conductor
             :disc-no
             :genre
             :producer
             :remixer
             :title
             :track
             :year])

(defn to-i [vals]
  (when-let [val (and (seq vals) (re-find #"\d+" (first vals)))]
    (Integer/valueOf val)))

(defn to-year [vals]
  (->> vals
       (map #(re-find #"\d{4}" %))
       (filter identity)
       first))

(defn translate-genres [vals]
  (mapv (fn [val]
          (if-let [[_ n] (re-matches #"\((\d+)\).*" val)]
            (nth id3v1-genres (Integer/valueOf n))
            val))
        vals))

(def field-fns ^{:private true}
  {:track       to-i
   :disc-no     to-i
   :album       first
   :title       first
   :year        to-year
   :conductor   first
   :genre       translate-genres})

(defmulti field-str class)

(defmethod field-str TagTextField
  [field]
  (.getContent field))

(defmethod field-str TagField
  [field]
  (.toString field))

(defn field-key [k]
  (FieldKey/valueOf (string/replace (.toUpperCase (name k)) "-" "_")))

(defn base-tag-fields [tag]
  (->> fields
       (map (fn [k]
              (let [fs (.getFields tag (field-key k))]
                [k (->> fs
                        (map field-str)
                        (mapcat #(string/split % #"\000"))
                        (filterv (complement string/blank?)))])))
       (filter #(seq (last %)))
       (map (fn [[k v]]
              (let [f (get field-fns k identity)]
                [k (f v)])))
       (into {})))

(defmulti tag-fields class)

(defmethod tag-fields Tag
  [tag]
  (base-tag-fields tag))

(defmethod tag-fields Mp4Tag
  [tag]
  (let [track (.getFirst tag Mp4FieldKey/TRACK)]
    (merge-with #(or %1 %2)
                (base-tag-fields tag)
                {:genre (mapv field-str (.get tag Mp4FieldKey/GENRE_CUSTOM))}
                (let [[_ track] (re-matches #"(\d+)(/\d+)?" (str track))]
                  {:track       (when track (Integer/parseInt track))}))))

(defn info
  "Pull meta data from audio file."
  [file]
  (let [audio  (AudioFileIO/read file)
        tag    (.getTag audio)
        header (.getAudioHeader audio)]
    (when (and tag header)
      (into {:length   (.getTrackLength header)
             :encoding (.getEncodingType header)}
            (tag-fields tag)))))
