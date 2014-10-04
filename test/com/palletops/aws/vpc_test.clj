(ns com.palletops.aws.vpc-test
  (:require
   [clojure.core.async :refer [<!! chan]]
   [clojure.test :refer :all]
   [com.palletops.aws.api :as api]
   [com.palletops.aws.vpc :refer :all]
   [pallet.compute :refer [service-properties]]
   [pallet.crates.test-nodes :as test-nodes]
   [pallet.test-env
    :refer [*compute-service* *node-spec-meta* test-env]]
   [pallet.test-env.project :as project]))

(test-env test-nodes/node-specs project/project)

(deftest ^:support vpc
  (let [{:keys [credentials] :as props} (service-properties *compute-service*)
        c (chan)
        api (api/start {})
        cidr-block "10.1.0.0/20"]
    (try

      (testing "ensure-vpc"
        (ensure-vpc api credentials {:cidr-block cidr-block} c)
        (let [vpc (<!! c)]
          (is (not (when-let [e (:exception vpc)]
                     (throw (ex-info "ensure failed" {} e)))))
          (is (:vpc-id (:vpc vpc)) "vpc was created or exists")))

      (testing "describe-vpc"
        (describe-vpc api credentials {:cidr-block cidr-block} c)
        (let [vpc (<!! c)]
          (is (not (when-let [e (:exception vpc)]
                     (throw (ex-info "describe failed" {} e)))))
          (is (:vpc-id (:vpc vpc)) "vpc exists")
          (is (= cidr-block (:cidr-block (:vpc vpc)))
              "vpc has correct cidr-block")))

      (testing "destroy-vpc"
        (destroy-vpc api credentials {:cidr-block cidr-block} c)
        (let [rv (<!! c)]
          (is (not (when-let [e (:exception vpc)]
                     (throw (ex-info "destroy failed" {} e)))))
          (is (= {:return true} rv) "vpc deleted")))

      (finally
        (api/stop api)))))

;; (deftest ^:support vpc-graph-test
;;   (let [{:keys [credentials] :as props} (service-properties *compute-service*)
;;         c (chan)
;;         api (api/start {})]
;;     (try
;;       (ensure-vpc api credentials {:cidr-block "10.0.0.1/20"} c)
;;       (let [vpc (<!! c)]
;;         (is (not (when-let [e (:exception vpc)]
;;                    (throw e))))
;;         (is (:vpc-id vpc)))
;;       (vpc-graph api credentials c)
;;       (is (nil? (<!! c)))
;;       (destroy-vpc api credentials {:cidr-block "10.0.0.1/20"} c)
;;       (is (nil? (<!! c)))
;;       (finally
;;         (api/stop api)))))
