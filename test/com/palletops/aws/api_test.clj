(ns com.palletops.aws.api-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.aws.api :as api]))

(deftest start-stop-test
  (let [api (api/start {})]
    (is (:channel api))
    (api/stop api)))
