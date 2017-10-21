;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.utils-test
  (:require [clojure.test :refer [deftest is]]
            [semira.utils :as sut]))

(deftest sha1
  (is (= "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
         (sut/sha1 "test"))))

(deftest sort-by-keys
  (let [coll [{:foo 1}
              {:foo 2}
              {:foo 2, :bar ["1"]}
              {:foo 2, :bar ["2" "3"]}]]
    (is (= coll
           (sut/sort-by-keys [:foo :bar]
                             (shuffle coll))))))
