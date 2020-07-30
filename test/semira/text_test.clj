;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.text-test
  (:require [clojure.test :refer [are deftest]]
            [semira.text :as sut]))

(deftest translit-to-ascii
  (are [expected input] (= expected (sut/translit-to-ascii input))
    "Cosi fan tutte"
    "Così fan tutte"

    "OEoesssst"
    "Œœßßŧ"

    "I want some soup"
    "Ĩ ŵăŉŧ şōmð ſőůþ"))
