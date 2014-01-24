(defproject com.cognitect/fluxion "0.1.0-SNAPSHOT"
  :description "Asynchronous metric collection, aggregation, and distribution."
  :url "http://github.com/relevance/fluxion"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :profiles {:dev {:dependencies [[org.clojure/test.generative "0.5.0"]]
                   :aliases {"test" ["run" "-m" "clojure.test.generative.runner" "test"]}
                   :jvm-opts ["-Dclojure.test.generative.msec=500000" "-Dclojure.test.generative.threads=10"]}})
