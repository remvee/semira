;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.models-test
  (:require [clojure.test :refer [deftest is testing]]
            [semira.models :as sut]))

(deftest update-track
  (let [album-id (rand)
        track-1  {:id (rand), :track 1, :mtime 0}
        albums   [{:id album-id, :tracks [track-1], :search-index "", :mtime 0}]]
    (testing "one track album"
      (is (= albums (sut/update-track [] album-id track-1))
          "track added")
      (is (= albums (sut/update-track albums album-id track-1))
          "track already added"))
    (testing "multi track album"
      (let [track-2  {:id (rand), :track 2, :mtime 1}
            albums-2 [{:id album-id, :tracks [track-1 track-2], :search-index "", :mtime 1}]]
        (is (= albums-2 (sut/update-track albums album-id track-2))
            "track added to end")
        (let [track-0  {:id (rand), :track 0, :mtime 0}
              albums-0 [{:id album-id, :tracks [track-0 track-1 track-2], :search-index "", :mtime 1}]]
          (is (= albums-0 (sut/update-track albums-2 album-id track-0))
              "track added to beginning"))))))

(deftest normalize-album
  (is (= {:artist "test"
          :tracks [{:title "test 1"} {:title "test 2"}]}
         (sut/normalize-album {:tracks [{:title "test 1" :artist "test"}
                                        {:title "test 2" :artist "test"}]}))
      "same track artist moved up to album")
  (is (= {:artist "test"
          :tracks [{:track 0} {:track 0}]}
         (sut/normalize-album {:tracks [{:artist "test", :track 0}
                                        {:artist "test", :track 0}]}))
      "track number untouched"))
