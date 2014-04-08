{:dev {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.5"]]
       :checkout-deps-shares [:source-paths :test-paths :resource-paths
                              :compile-path]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "0.1.3"]]
       :pallet-release {:url "https://pbors:${GH_TOKEN}@github.com/pallet/pallet-aws-ops.git",
                        :branch "master"}}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.1/api"
               :src-dir-uri "https://bitbucket.org/palletops/pallet-aws-ops/src/develop/src/"
               :src-linenum-anchor-prefix "cl-"}
       :aliases {"marg" ["marg" "-d" "doc/0.1/"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :release
 {:plugins [[lein-set-version "0.3.0"]]
  :set-version
  {:updates [{:path "README.md" :no-snapshot true}]}}
 :no-checkouts {:checkout-shares ^:replace []} ; disable checkouts
 :clojure-1.5.0 {:dependencies [[org.clojure/clojure "1.5.0"]]}}
