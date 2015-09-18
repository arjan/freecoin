(ns freecoin.test.handlers.participant-query
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [stonecutter-oauth.client :as sc]
            [freecoin.db.uuid :as uuid]
            [freecoin.storage :as storage]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.blockchain :as fb]
            [freecoin.test.test-helper :as th]
            [freecoin.handlers.participant-query :as pq]))

(defn create-wallet [wallet-store blockchain wallet-data]
  (let [{:keys [sso-id name email]} wallet-data]
    (:wallet (w/new-empty-wallet! wallet-store blockchain uuid/uuid sso-id name email))))

(defn index->wallet-data [index]
  (let [sso-id (str "sso-id-" index)
        name (str "name-" index)
        email (str "wallet-" index "@email.com")]
    {:sso-id sso-id :name name :email email}))

(facts "about the participant query form"
       (fact "can be accessed by signed-in users"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain uuid/uuid
                                                        "stonecutter-user-id" "name" "test@email.com"))
                   query-form-handler (pq/query-form wallet-store)
                   response (-> (th/create-request
                                 :get "/participants-query"
                                 {} (th/authenticated-session (:uid wallet)))
                                query-form-handler)]
               (:status response) => 200))

       (fact "can not be accessed when user is not signed in"
             (let [wallet-store (fm/create-memory-store)
                   query-form-handler (pq/query-form wallet-store)
                   response (-> (th/create-request :get "/participants-query" {})
                                query-form-handler)]
               (:status response) => 401)))

(facts "about the participants query handler"
       (fact "can not be accessed when user is not signed in"
             (let [wallet-store (fm/create-memory-store)
                   participants-handler (pq/participants wallet-store)
                   response (-> (th/create-request :get "/participants" {})
                                participants-handler)]
               (:status response) => 401))
       
       (fact "without any query parameters, lists all participants"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallets (doall (->> (range 5)
                                       (map index->wallet-data)
                                       (map (partial create-wallet wallet-store blockchain))))
                   participants-handler (pq/participants wallet-store)
                   response (-> (th/create-request
                                 :get "/participants"
                                 {} (th/authenticated-session (:uid (first wallets))))
                                participants-handler)]
               (:status response) => 200
               (-> (:body response) html/html-snippet) => (th/element-count [:.clj--participant__item] 5)))

       (tabular
        
        (fact "can query by name or email"
              (let [wallet-store (fm/create-memory-store)
                    blockchain (fb/create-in-memory-blockchain :bk)
                    wallet-data [{:name "James Jones" :email "james@jones.com" :sso-id "sso-id-1"}
                                 {:name "James Jones" :email "jim@jones.com" :sso-id "sso-id-2"}
                                 {:name "Sarah Lastname" :email "sarah@email.com" :sso-id "sso-id-3"}]
                    wallets (doall (map (partial create-wallet wallet-store blockchain) wallet-data))
                    participants-handler (pq/participants wallet-store)
                    query-m {:field ?field :value ?value}
                    response (-> (th/create-request
                                  :get "/participants"
                                  query-m (th/authenticated-session (:uid (first wallets))))
                                 participants-handler)]
                (-> (:body response) html/html-snippet)
                => (th/element-count [:.clj--participant__item] ?expected-count)))
        
        ?field   ?value                  ?expected-count
        "name"   "James Jones"           2
        "email"  "james@jones.com"       1
        "fake"   "whatever"              0))

(future-facts "Query parameters are validated")