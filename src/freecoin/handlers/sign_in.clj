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

(ns freecoin.handlers.sign-in
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [freecoin.auth :as auth]
            [freecoin.blockchain :as blockchain]
            [freecoin.config :as config]
            [freecoin.context-helpers :as ch]
            [freecoin.db.uuid :as uuid]
            [freecoin.db.wallet :as wallet]
            [freecoin.routes :as routes]
            [freecoin.views :as fv]
            [freecoin.views.index-page :as index-page]
            [freecoin.views.landing-page :as landing-page]
            [freecoin.views.welcome-page :as welcome-page]
            [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [stonecutter-oauth.client :as soc]))

(lc/defresource index-page
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> (index-page/build)
                 fv/render-page))

(lc/defresource landing-page [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :exists? (fn [ctx]
             (if-let [uid (:uid (auth/is-signed-in ctx))]
               (let [wallet (wallet/fetch wallet-store uid)]
                 {:wallet wallet})
               {}))

  :handle-ok (fn [ctx]
               (if-let [wallet (:wallet ctx)]
                 (-> (routes/absolute-path (config/create-config) :account :uid (:uid wallet))
                     r/redirect
                     lr/ring-response)
                 (-> {:sign-in-url "/sign-in-with-sso"}
                     landing-page/landing-page
                     fv/render-page))))

(lc/defresource sign-in [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> (soc/authorisation-redirect-response sso-config)
                 lr/ring-response))

(defn wallet->access-key [blockchain wallet]
  (let [secret (get-in wallet [:blockchain-secrets (blockchain/label blockchain)])]
    (s/join "::" [(:cookie secret) (:_id secret)])))

(lc/defresource sso-callback [wallet-store blockchain sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :allowed? (fn [ctx]
              (when-let [code (get-in ctx [:request :params :code])]
                (try
                  (when-let [token-response (soc/request-access-token! sso-config code)]
                    {::token-response token-response})
                  (catch Exception e nil))))
  :handle-forbidden (-> (routes/absolute-path (config/create-config) :landing-page)
                        r/redirect
                        lr/ring-response)
  :exists? (fn [ctx]
             (let [token-response (::token-response ctx)
                   sso-id (get-in token-response [:user-info :sub])
                   email (get-in token-response [:user-info :email])
                   name (first (s/split email #"@"))]
               ;; the wallet exists already
               (if-let [wallet (wallet/fetch-by-sso-id wallet-store sso-id)]
                 {::uid (:uid wallet)}

                 ;; a new wallet has to be made
                 (when-let [{:keys [wallet apikey]}
                            (wallet/new-empty-wallet!
                                wallet-store
                                blockchain uuid/uuid
                                sso-id name email)]

                   ;; TODO: distribute other shares to organization and auditor
                   ;; see in freecoin.db.wallet
                   ;; {:wallet (mongo/store! wallet-store :uid wallet)
                   ;;  :apikey       (secret->apikey              account-secret)
                   ;;  :participant  (secret->participant-shares  account-secret)
                   ;;  :organization (secret->organization-shares account-secret)
                   ;;  :auditor      (secret->auditor-shares      account-secret)
                   ;;  }))

                   ;; saved in context
                   {::uid (:uid wallet)
                    ::cookie-data apikey}))))

  :handle-ok
  (fn [ctx]
    (lr/ring-response (cond-> (r/redirect (routes/absolute-path
                                           (config/create-config) :sign-in-welcome))
                        (::cookie-data ctx) (assoc-in [:session :cookie-data] (::cookie-data ctx))
                        true (assoc-in [:session :signed-in-uid] (::uid ctx)))))

  :handle-not-found
  (-> (routes/absolute-path (config/create-config) :landing-page)
      r/redirect
      lr/ring-response))

(lc/defresource sign-in-welcome [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :exists? #(auth/has-wallet % wallet-store)

  :handle-ok
  (fn [ctx]
    (-> {:wallet (:wallet ctx) :secret (:cookie-data (:request ctx))}
        welcome-page/build
        fv/render-page)))

(defn preserve-session [response request]
  (assoc response :session (:session request)))

(lc/defresource sign-out
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :handle-ok (fn [ctx]
               (-> (routes/absolute-path (config/create-config) :index)
                   r/redirect
                   (preserve-session (:request ctx))
                   (update-in [:session] dissoc :signed-in-uid)
                   lr/ring-response)))

(lc/defresource forget-secret
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok
  (fn [ctx]
    (-> (routes/absolute-path (config/create-config) :account :uid (:uid ctx))
        r/redirect
        (preserve-session (:request ctx))
        (update-in [:session] dissoc :cookie-data)
        lr/ring-response)))
