(ns utils
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import java.util.Base64))

(def decoder (Base64/getDecoder))

(defn decode64 [s]
  (String. (.decode decoder (.getBytes s))))

(def read-config
  (memoize
   (fn read-config*
     ([]  (read-config* {}))
     ([{:keys [enviroment]}]
      (log/info "Reading config")
      (merge
       (let [config-file (io/file "config/config.edn")]
         (when (.exists config-file)
           (read-string (slurp config-file))))
       (try
         (some-> (or enviroment "FlEXIANA_CONFIG")
                 System/getenv (str/replace #"\s" "")
                 decode64 read-string)
         (catch Exception ex (log/info "Error reading enviromental string:" (.getMessage ex)))))))))
