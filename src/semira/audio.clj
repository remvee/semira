(ns semira.audio
  (:import [java.io StringBufferInputStream]
           [java.util.logging LogManager]
           [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey TagField TagTextField]))

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

(defn translate-genres [v]
  (and (first v)
       (map #(if-let [[_ n] (re-matches #"\((\d+)\).*" %)]
               (nth id3v1-genres (Integer/valueOf n))
               %) v)))

(def field-fns ^{:private true}
  {:track       to-i
   :track_total to-i
   :disc_no     to-i
   :disc_total  to-i
   :album       first
   :title       first
   :year        first
   :conductor   first
   :genre       translate-genres})

(defmulti field-str class)
(defmethod field-str TagTextField [field] (.getContent field))
(defmethod field-str TagField [field] (.toString field))

(defn info
  "Pull meta data from audio file."
  [file]
  (let [audio (AudioFileIO/read file)
        tag (.getTag audio)
        header (.getAudioHeader audio)]
    (if (and tag header)
      (into {:length (.getTrackLength header)
             :encoding (.getEncodingType header)}
            (map (fn [[k v]] [k ((get field-fns k identity) v)])
                 (filter #(seq (last %))
                         (map (fn [k]
                                (let [fs (.getFields tag
                                                     (FieldKey/valueOf (.toUpperCase (name k))))]
                                  [k (vec (filter #(not= "" %) (map field-str fs)))]))
                              fields)))))))