(defproject com.palletops/pallet-aws-ops "0.1.0-SNAPSHOT"
  :description "Basic operations for building on aws"
  :url "https:/github.com/pallet/pallet-aws-ops"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.palletops/awaze "0.1.0"
                  :exclusions [commons-logging]]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]])