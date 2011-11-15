(ns semira.audio
  (:import [java.io StringBufferInputStream]
           [java.util.logging LogManager]
           [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey Tag TagField TagTextField]
           [org.jaudiotagger.tag.mp4 Mp4Tag Mp4FieldKey]))

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

(defn to-i [vals]
  (if (first vals)
    (if-let [val (re-find #"\d+" (first vals))]
      (Integer/valueOf val))))

(defn to-year [vals]
  (first (filter identity (map #(re-find #"\d{4}" %) vals))))

(defn translate-genres [vals]
  (and (first vals)
       (vec (map (fn [val]
                   (if-let [[_ n] (re-matches #"\((\d+)\).*" val)]
                     (nth id3v1-genres (Integer/valueOf n))
                     val))
                 vals))))

(def field-fns ^{:private true}
  {:track       to-i
   :track_total to-i
   :disc_no     to-i
   :disc_total  to-i
   :album       first
   :title       first
   :year        to-year
   :conductor   first
   :genre       translate-genres})

(defmulti field-str class)
(defmethod field-str TagTextField [field] (.getContent field))
(defmethod field-str TagField [field] (.toString field))

(defn base-tag-fields [tag]
  (into {}
        (map (fn [[k v]] [k ((get field-fns k identity) v)])
             (filter #(seq (last %))
                     (map (fn [k]
                            (let [fs (.getFields tag
                                                 (FieldKey/valueOf (.toUpperCase (name k))))]
                              [k (vec (filter #(not= "" %) (map field-str fs)))]))
                          fields)))))

(defmulti tag-fields class)
(defmethod tag-fields Tag [tag] (base-tag-fields tag))
(defmethod tag-fields Mp4Tag [tag]
  (let [track (.getFirst tag Mp4FieldKey/TRACK)]
    (merge-with #(or %1 %2)
                (base-tag-fields tag)
                {:genre (vec (map field-str (.get tag Mp4FieldKey/GENRE_CUSTOM)))}
                (let [[_ track _ track-total] (re-matches #"(\d+)(/(\d+))?" (str track))]
                  {:track (and track (Integer/parseInt track))
                   :track_total (and track-total (Integer/parseInt track-total))}))))

(defn info
  "Pull meta data from audio file."
  [file]
  (let [audio (AudioFileIO/read file)
        tag (.getTag audio)
        header (.getAudioHeader audio)]
    (if (and tag header)
      (into {:length (.getTrackLength header)
             :encoding (.getEncodingType header)}
            (tag-fields tag)))))
