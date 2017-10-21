;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.audio-test
  (:require [clojure.test :refer [deftest is testing]]
            [semira.audio :as sut])
  (:import org.jaudiotagger.tag.FieldKey
           [org.jaudiotagger.tag.id3 ID3v11Tag ID3v22Tag]
           org.jaudiotagger.tag.mp4.Mp4Tag))

(deftest tag-fields
  (testing "ID3v1"
    (let [tag (doto (ID3v11Tag.)
                (.addField FieldKey/GENRE "Classic Rock")
                (.addField FieldKey/ARTIST "Fred & Barney")
                (.addField FieldKey/ALBUM "Best of")
                (.addField FieldKey/YEAR "1970")
                (.addField FieldKey/TRACK "2")
                (.addField FieldKey/TITLE "Wilma!"))]
      (is (= {:genre  ["Classic Rock"]
              :artist ["Fred & Barney"]
              :album  "Best of"
              :year   "1970"
              :track  2
              :title  "Wilma!"}
             (sut/tag-fields tag)))))
  (testing "ID3v2"
    (let [tag (doto (ID3v22Tag.)
                (.addField FieldKey/GENRE "Classic Rock")
                (.addField FieldKey/ARTIST "Fred Flintstone")
                (.addField FieldKey/ARTIST "Barney Rubble")
                (.addField FieldKey/ALBUM "Best of")
                (.addField FieldKey/YEAR "1970")
                (.addField FieldKey/DISC_NO "1")
                (.addField FieldKey/TRACK "2")
                (.addField FieldKey/TITLE "Wilma!"))]
      (is (= {:genre   ["Classic Rock"]
              :artist  ["Fred Flintstone" "Barney Rubble"]
              :album   "Best of"
              :year    "1970"
              :disc-no 1
              :track   2
              :title   "Wilma!"}
             (sut/tag-fields tag)))))
  (testing "MP4"
    (let [tag (doto (Mp4Tag.)
                (.addField FieldKey/GENRE "Classic Rock")
                (.addField FieldKey/ARTIST "Fred Flintstone")
                (.addField FieldKey/ARTIST "Barney Rubble")
                (.addField FieldKey/ALBUM "Best of")
                (.addField FieldKey/YEAR "1970")
                (.addField FieldKey/DISC_NO "1")
                (.addField FieldKey/TRACK "2")
                (.addField FieldKey/TITLE "Wilma!"))]
      (is (= {:genre   ["Classic Rock"]
              :artist  ["Fred Flintstone" "Barney Rubble"]
              :album   "Best of"
              :year    "1970"
              :disc-no 1
              :track   2
              :title   "Wilma!"}
             (sut/tag-fields tag))))))
