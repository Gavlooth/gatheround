
(defproject gatheround "0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "LATEST"]
                 ;; [org.clojure/core.async "1.3.610"]
                 ;;ring
                 [ring/ring-jetty-adapter "1.9.1"]
                 [ring/ring-core"1.9.1"]
                 ;; [ring/ring-json "0.5.0"]
                 ;;routing libraries
                 [metosin/reitit-core "0.5.12"]
                 [metosin/reitit-middleware "0.5.12"]
                 [metosin/reitit-ring "0.5.12"]

                 [metosin/jsonista "0.3.5"]
                 [ring/ring-json "0.5.1"]

                
                 [org.clojure/tools.logging "LATEST"]
                 [org.apache.logging.log4j/log4j-core "LATEST"]
                 [org.apache.logging.log4j/log4j-api "LATEST"]
                 [org.apache.logging.log4j/log4j-jcl "LATEST"]
                 ;; other
                 #_[commons-codec/commons-codec "1.9"]
                 #_[org.apache.commons/commons-lang3 "3.10"]
                 ;;dependecy injection
                 #_[juxt/clip "0.16.0"]
                 #_[com.fzakaria/slf4j-timbre "0.3.19"]
                 ;; matching library
                 #_[org.clojure/core.match "1.0.0"]]
                 ;;http requests
  :main ^:skip-aot gatheround.core
  ;; :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory"]
  :target-path "target/%s"
  :profiles {:cljs {:source-paths ["src/cljs"]
                    :plugins [[lein-tools-deps "0.4.5"]]
                    :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
                    :lein-tools-deps/config {:config-files [:install :user :project]}}
             :dev {:source-paths ["src/clj" "dev"]
                   :dependencies [[org.clojure/tools.namespace "LATEST"]
                                  [jumblerg/ring-cors "2.0.0"]]
                   :repl-options {:init-ns user}
                   :init (println "here we are in" *ns*)}
             :uberjar {:source-paths ["src/clj"]; "prod"
                       :main gatheround.core
                       :aot :all}})

