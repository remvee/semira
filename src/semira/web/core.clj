(ns semira.web.core
  (:require
   [semira.utils :as utils]
   [hiccup.core :as hiccup]
   [hiccup.page-helpers :as hiccup-helpers]))

(def app-title "SEMIRA")

(defn layout [body & [{:keys [title]}]]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (str "<!DOCTYPE HTML>"
              (hiccup/html [:html
                            [:head
                             [:title (utils/h (if title (str app-title " / " title) app-title))]
                             [:meta {:name "viewport", :content "width=device-width, initial-scale=1, maximum-scale=1"}]
                             (hiccup-helpers/include-css "/css/screen.css")]
                            [:body
                             [:div#container body]
                             (hiccup-helpers/include-js "/js/semira/goog/base.js")
                             (hiccup-helpers/include-js "/js/semira.js")
                             [:script {:type "application/javascript"} "goog.require('semira.frontend')"]
                             ]]))})
