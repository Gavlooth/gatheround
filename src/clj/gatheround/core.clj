(ns gatheround.core
  (:gen-class)
  (:require
    #_[clojure.pprint :refer [cl-format]]
    [clojure.tools.logging :as log]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [muuntaja.core :as m]
    [reitit.ring.coercion :as coercion]
    [reitit.ring :as ring]
    [jsonista.core :refer [read-value]]
    [ring.adapter.jetty :as jetty]
    [ring.util.request :refer [body-string]]
    [utils :as u])
  (:import (org.eclipse.jetty.util.log Log)
           (org.eclipse.jetty.util.log StdErrLog)))

(defonce system {})

(System/console)
(.println System/out *out*)

;; pw = new PrintWriter(new OutputStreamWriter(myPrintStream, encoding));
(binding [*out* (new java.io.PrintWriter System/out)]
  (println "testing console binding"))


(defn wrap-body-string [handler]
 (fn [request]
     (handler (update request :body body-string))))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
       (update response  :headers   merge {"Access-Control-Allow-Headers" "Content-Type"
                                           "Access-Control-Allow-Origin" "*"}))))


(defn wrap-request-logging [handler]
  (fn [request]
    (handler (log/spy request))))

(defn wrap-response-logging [handler]
  (fn [request]
     (log/spy (handler request))))

(defn scrambled? [string1 string2]
   (let [ [matching-string string-to-match]  (mapv frequencies [string1 string2])]
     (not
       (some (fn [[k v]]
              (not
               (when-let [v2  (get matching-string k)]
                 (>=  v2 v))))
            (seq string-to-match)))))


(def router
 (ring/ring-handler
  (ring/router
   [["/gatheround"
       { :post (fn [{:keys  [body-params]}]
                (let [ {:strs [string-1 string-2]} (read-value body-params)]
                  (try
                   {:status 200
                    :body  (str (scrambled? string-1 string-2))}
                   (catch Exception exp (log/info (.getMessage exp))
                                        {:status 500 :body "Internal server error"}))))}]]
   {:data {:muuntaja m/instance
           :middleware [wrap-cors
                        muuntaja/format-middleware
                        wrap-body-string
                        coercion/coerce-exceptions-middleware]}})
  (ring/create-default-handler
    {:not-found (constantly {:status 404, :body "Not found"})
     :method-not-allowed (constantly {:status 405, :body "Method not allowed"})
     :not-acceptable (constantly {:status 406, :body "Not acceptable"})})))

(def vrouter (volatile! router))
(defn start-server [&  [opts]]
  (let [server  (jetty/run-jetty (fn [& args](apply @vrouter args))
                                 (merge {:port 9001, :join? false :async? false} opts))]
      (alter-var-root #'system assoc :web-server server)))


(defn shutdown-system []
    (when-let [web-server (:web-server system)]
      (try (.stop web-server)
           (catch Exception exp (log/info (.getMessage exp))))))

(defn -main  [& _]
  (try
    (Log/setLog (new StdErrLog))
    (shutdown-system)
    (let [config (u/read-config)]
      (start-server  (merge system config)))
    (catch Exception exp (log/info (.getMessage exp))
                         (shutdown-system)
                         (comment
                           (.addShutdownHook
                            (Runtime/getRuntime)
                            (Thread.  (shutdown-system)))))))




#_(-main)
#_( (shutdown-system))
;; (.addShutdownHook
;;  (Runtime/getRuntime)
;;  (T hread.  (shutdown-system )))
