(ns freecoin.views.welcome-page
  (:require [freecoin.routes :as routes]
            [clojure.tools.logging :as log]))

(defn build [context]

  (let [wallet (:wallet context)]
  (log/info "aaa" wallet)
    
    {:body-class "func--welcome-page--body"
     :title "Welcome to your new account"
     :body [:h2 [:a.func--welcome-page--continue-btn
                 {:href (routes/path :account :uid (:uid wallet))}
                 "Continue"]]
     }))
