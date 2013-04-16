(ns com.palletops.aws.instance-poller-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.aws.api :as api]
   [com.palletops.aws.instance-poller :as poller]))

(deftest add-instances-test
  (let [config (atom {})]
    (poller/add-instances
     {:config config}
     {"1" [{:xx 1}]})
    (is (= {:instance-id-map {"1" [{:xx 1}]}}
           @config))))

(deftest update-poller-test
  (let [instance {:instance-id "1"}]
    (is (= {:com.palletops.aws.instance-poller/notifiable
            [{:xx 1 :instance instance}]
            :instance-id-map {}}
           (#'poller/update-poller
            {:instance-id-map {"1" [{:xx 1}]}}
            {:reservations [{:instances [{:instance-id "1"}]}]}))
        "default notify-when")
    (let [entry {:xx 1 :notify-when-f (constantly false)}]
      (is (= {:com.palletops.aws.instance-poller/notifiable []
              :instance-id-map {"1" [entry]}}
             (#'poller/update-poller
              {:instance-id-map {"1" [entry]}}
              {:reservations [{:instances [instance]}]}))
          "false notify-when"))
    (let [entry {:xx 1 :notify-when-f (constantly true)}]
      (is (= {:com.palletops.aws.instance-poller/notifiable
              [(assoc entry :instance instance)]
              :instance-id-map {}}
             (#'poller/update-poller
              {:instance-id-map {"1" [entry]}}
              {:reservations [{:instances [instance]}]}))
          "true notify-when"))
    (let [entry {:xx 1 :notify-when-f #(not= "pending" (-> % :state :name))}
          instance
          {:state {:name "pending", :code 0}
           :instance-id "i-75f2d90c"}]
      (is (= {:com.palletops.aws.instance-poller/notifiable []
              :instance-id-map {"i-75f2d90c" [entry]}}
             (#'poller/update-poller
              {:instance-id-map {"i-75f2d90c" [entry]}}
              {:reservations [{:instances [instance]}]}))
          "example"))))

(deftest start-stop-test
  (let [a (api/start {})
        p (poller/start {:api-channel (:channel a)})]
    (is (:channel a))
    (is (map? @(:config p)))
    (poller/stop p)
    (api/stop a)))
