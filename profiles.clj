{:dev {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.5"]
                      [ch.qos.logback/logback-classic "1.1.1"]
                      [com.palletops/crates "0.1.2"]
                      [com.palletops/pallet-test-env "RELEASE"]
                      [com.palletops/pallet "0.8.0-RC.9"]]
       :checkout-deps-shares [:source-paths :test-paths :resource-paths
                              :compile-path]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "RELEASE"]
                 [com.palletops/lein-test-env "RELEASE"]]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.1/api"
               :src-dir-uri "https://bitbucket.org/palletops/pallet-aws-ops/src/develop/src/"
               :src-linenum-anchor-prefix "cl-"}
       :aliases {"marg" ["marg" "-d" "doc/0.1/"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :clojure-1.5.0 {:dependencies [[org.clojure/clojure "1.5.0"]]}
 :aws {:pallet/test-env {:test-specs [{:selector :amzn-linux-2013-092}]}
       :dependencies [[com.palletops/pallet-aws "0.2.3"]]}}
