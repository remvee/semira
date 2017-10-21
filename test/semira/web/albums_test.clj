;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.albums-test
  (:require [clojure.test :refer [deftest is testing]]
            [semira.web.albums :as sut]
            [ring.mock.request :as mock])
  (:import java.util.Date))

(deftest titleize
  (is (= "Foo - Bar - Zoo"
         (sut/titleize {:foo "Foo", :bar "Bar", :quux "Quux", :zoo "Zoo"}
                       :foo :bar :yelp :zoo))))

(def album1
  {:id "1"
   :tracks []
   :mtime 0})

(def album2
  {:id "2"
   :tracks []
   :mtime 0})

(def album3
  {:id "3"
   :tracks []
   :mtime 0})

(def albums [album1 album2 album3])

(deftest rss
  (is (re-matches #"<\?xml.*<rss>.*</rss>" (sut/rss [])))
  (is (= (count albums)
         (->> (sut/rss albums) (re-seq #"<item") count))))

(deftest bare-handler
  (let [handler #(sut/bare-handler (assoc % :albums albums))]
    (testing "GET /album/:id"
      (testing "not existing"
        (is (nil? (handler (mock/request :get "/album/31415")))))
      (testing "existing"
        (let [response (handler (mock/request :get "/album/1"))]
          (is (= 200 (:status response)))
          (is (.startsWith (get-in response [:headers "Content-Type"]) "application/clojure"))
          (let [body (read-string (:body response))]
            (is (= "1" (:id body)))
            (is (contains? body :tracks))))))
    (testing "GET /albums"
      (let [response (handler (mock/request :get "/albums"))]
        (is (= 200 (:status response)))
        (is (.startsWith (get-in response [:headers "Content-Type"]) "application/clojure"))
        (let [body (read-string (:body response))]
          (is (sequential? body))
          (is (= #{"1" "2" "3"} (->> body (map :id) set))))))
    (testing "GET /albums.rss"
      (let [response (handler (mock/request :get "/albums.rss"))]
        (is (= 200 (:status response)))
        (is (.startsWith (get-in response [:headers "Content-Type"]) "text/xml"))
        (is (re-matches #"<\?xml.*<rss>.*</rss>" (:body response)))))))
