;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.stream-test
  (:require [clojure.test :refer [deftest is testing]]
            [semira.stream :as sut]))

(deftest convert-command
  (is (= ["gst-launch-1.0" "-q" "filesrc" "location=" "test.flac"
          "!" "flacparse"
          "!" "flacdec"
          "!" "audioconvert"
          "!" "lamemp3enc" "target=bitrate" "bitrate=96"
          "!" "xingmux"
          "!" "id3mux"
          "!" "filesink" "location=" "test.mp3"]
         (sut/convert-command "test.flac" "test.mp3" "audio/mpeg"))
      "flac to mp3")
  (is (= ["gst-launch-1.0" "-q" "filesrc" "location=" "test.flac"
          "!" "flacparse" "!" "flacdec"
          "!" "audioconvert"
          "!" "vorbisenc" "bitrate=96000"
          "!" "oggmux"
          "!" "filesink" "location=" "test.ogg"]
         (sut/convert-command "test.flac" "test.ogg" "audio/ogg"))
      "flac to ogg")
  (is (= ["gst-launch-1.0" "-q" "filesrc" "location=" "test.ogg"
          "!" "oggdemux"
          "!" "vorbisdec"
          "!" "audioconvert"
          "!" "lamemp3enc" "target=bitrate" "bitrate=96"
          "!" "xingmux"
          "!" "id3mux"
          "!" "filesink" "location=" "test.mp3"]
         (sut/convert-command "test.ogg" "test.mp3" "audio/mpeg"))
      "ogg to mp3")
  (is (= ["gst-launch-1.0" "-q" "filesrc" "location=" "test.mp3"
          "!" "mpegaudioparse"
          "!" "mpg123audiodec"
          "!" "audioconvert"
          "!" "vorbisenc" "bitrate=96000"
          "!" "oggmux"
          "!" "filesink" "location=" "test.ogg"]
         (sut/convert-command "test.mp3" "test.ogg" "audio/ogg"))
      "mp3 to ogg")
  (is (= ["gst-launch-1.0" "-q" "filesrc" "location=" "test.mp3"
          "!" "mpegaudioparse"
          "!" "mpg123audiodec"
          "!" "audioconvert"
          "!" "audioresample"
          "!" "opusenc" "bitrate=96000"
          "!" "oggmux"
          "!" "filesink" "location=" "test.opus"]
         (sut/convert-command "test.mp3" "test.opus" "audio/ogg; codecs=\"opus\""))
      "mp3 to opus"))
