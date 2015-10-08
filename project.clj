(defproject com.palletops/pallet-aws-ops "0.2.2"
  :description "Basic operations for building on aws"
  :url "https://github.com/pallet/pallet-aws-ops"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.palletops/awaze "0.1.3"
                  :exclusions [commons-logging commons-codec]]
                 [commons-codec "1.6"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [com.taoensso/timbre "3.1.6"]])
