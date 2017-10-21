;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.stream-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [semira.web.stream :as sut]))

(def albums [{:tracks [{:id "1"}]}])

(defn- open [{:keys [id]} type]
  (str "open:" id ":" type))

(defn- length [{:keys [id]} type]
  (str "length:" id ":" type))

(deftest bare-handler
  (let [handler #(sut/bare-handler (assoc % :albums albums, :open open, :length length))]
    (testing "GET /stream/:id.mp3"
      (testing "not existing id"
        (is (nil? (:status (handler (mock/request :get "/stream/31415.mp3"))))))
      (testing "unknown extension"
        (is (nil? (:status (handler (mock/request :get "/stream/1.yelp"))))))
      (testing "existing"
        (testing "mp3"
          (let [response (handler (mock/request :get "/stream/1.mp3"))]
            (is (= 200 (:status response)))
            (is (= "audio/mpeg" (get-in response [:headers "Content-Type"])))
            (is (= "open:1:audio/mpeg" (:body response)))
            (is (= "length:1:audio/mpeg" (get-in response [:headers "Content-Length"])))))
        (testing "ogg"
          (let [response (handler (mock/request :get "/stream/1.ogg"))]
            (is (= 200 (:status response)))
            (is (= "audio/ogg" (get-in response [:headers "Content-Type"])))
            (is (= "open:1:audio/ogg" (:body response)))
            (is (= "length:1:audio/ogg" (get-in response [:headers "Content-Length"])))))))))
