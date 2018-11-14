;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.mime-type)

(def type-by-ext {"flac" "audio/flac"
                  "m4a"  "audio/mp4"
                  "mp3"  "audio/mpeg"
                  "ogg"  "audio/ogg"
                  "opus" "audio/opus"})

(defn type-by-file-name [file-name]
  (type-by-ext (->> file-name (re-find #"\.(\w+)$") last)))
