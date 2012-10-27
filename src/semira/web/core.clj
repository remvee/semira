;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.core
  (:require
   [semira.utils :as utils]
   [hiccup.core :as hiccup]
   [hiccup.page :as hiccup-page]))

(def app-title "SEMIRA")

(def development-mode (find-ns 'swank.core))

(defn layout [body & [{:keys [title]}]]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (str "<!DOCTYPE HTML>"
              (hiccup/html [:html
                            [:head
                             [:title (utils/h (if title (str app-title " / " title) app-title))]
                             [:meta {:name "viewport", :content "width=device-width, initial-scale=1, maximum-scale=1"}]
                             (hiccup-page/include-css "/css/screen.css")]
                            [:body
                             [:div#container body]
                             (if development-mode
                               [:div
                                (hiccup-page/include-js "/js/semira/goog/base.js")
                                (hiccup-page/include-js "/js/semira.js")
                                [:script {:type "application/javascript"} "goog.require('semira.frontend')"]]
                               (hiccup-page/include-js "/js/semira.js"))]]))})