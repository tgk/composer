(defproject composer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [overtone "0.9.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [com.stuartsierra/flow "0.1.0"]
                 [org.clojure/core.logic "0.8.7"]
                 [org.clojure/tools.namespace "0.2.3"]]
  :jvm-opts ^:replace []
  :profiles
  {:dev
   {:source-paths ["dev"]
    :dependencies [[org.clojure/java.classpath "0.2.1"]]}})
