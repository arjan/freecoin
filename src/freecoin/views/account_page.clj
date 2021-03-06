;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Duncan Mortimer <dmortime@thoughtworks.com>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.views.account-page
  (:require [clavatar.core :as clavatar]
            [freecoin.routes :as routes]))

(defn render-wallet [wallet]
  (let [email (:email wallet)]
    [:div {:class "wallet-details"}
     [:div {:class "card"}
      [:span (str "Name: " (:name wallet))]
      [:br]
      [:span (str "Email: ") [:a {:href (str "mailto:" email)} email]]
      [:br]
      [:span {:class "qrcode pull-left"}
       [:img {:src (routes/path :qrcode :uid (:uid wallet))}]]
      [:span {:class "gravatar pull-right"}
       [:img {:src (clavatar/gravatar (:email wallet) :size 87 :default :mm)}]]
      [:div {:class "clearfix"}]]]))

(defn build [context]
  (let [wallet (:wallet context)
        balance (:balance context)]
    {:body-class "func--account-page--body"
     :title "Welcome to Freecoin"
     :body [:div
            (render-wallet wallet) 
            [:div {:class "balance"}
             (str "Balance: ")
             [:span {:class "func--account-page--balance"}
              balance]]
            [:div
             [:a {:class "btn btn-primary" :href (routes/path :get-transaction-form)}
              (str "Send currency")]]]
     }))
