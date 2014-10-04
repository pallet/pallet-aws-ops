(ns com.palletops.aws.vpc
  "Manage VPCs."
  (:require
   [clojure.core.async :refer [<! >! chan put!]]
   [clojure.string :refer [join split]]
   [com.palletops.awaze.ec2 :as ec2]
   [com.palletops.aws.api :as aws]
   [com.palletops.aws.core :refer [go-try]]
   [taoensso.timbre :refer [debugf warnf infof tracef]]))


;;; Note AWS normalises cidr-blocks internally, so something
;;; like 10.0.0.1/20 appears as 10.0.0.0/20

(defn normalise-cidr
  "Normalise a cidr-block"
  [cidr-block]
  (let [comps (->> (split cidr-block #"[/.]") (map #(Long/parseLong %)) vec)
        bits (last comps)
        segs (subvec comps 0 4)
        addr (reduce (fn [v seg] (+ (* v 256) seg)) 0 segs)
        mask (bit-shift-left -1 (- 32 bits))
        v (bit-and addr mask)
        prefix (reduce
                (fn [s n]
                  (conj s (bit-and 255 (bit-shift-right v (* 8 n)))))
                nil (range 4))]
    (str (join "." prefix) "/" bits)))


(defn describe-vpc
  "Describe the vpc with the given cidr-block."
  [api credentials {:keys [cidr-block] :as vpc} ch]
  {:pre [(= cidr-block (normalise-cidr cidr-block))]}
  (go-try ch
    (let [c (chan)]
      (aws/submit api (ec2/describe-vpcs-map credentials {}) c)
      (let [vpcs (<! c)]
        (if-let [vpc (->> (:vpcs vpcs)
                          (filter #(= cidr-block (:cidr-block %)))
                          first)]
          (>! ch {:vpc vpc})
          (>! ch {}))))))

(defn ensure-vpc
  "Ensure a vpc exists with the given cidr-block."
  [api credentials {:keys [instance-tenancy cidr-block] :as vpc} ch]
  {:pre [(= cidr-block (normalise-cidr cidr-block))]}
  (go-try ch
    (let [c (chan)]
      (describe-vpc api credentials vpc c)
      (let [existing-vpc (<! c)]
        (if (:vpc-id (:vpc existing-vpc))
          (>! ch existing-vpc)
          (aws/submit api (ec2/create-vpc-map credentials vpc) ch))))))

(defn destroy-vpc
  "Destroy the vpc with the given cidr-block."
  [api credentials {:keys [cidr-block] :as vpc} ch]
  {:pre [(= cidr-block (normalise-cidr cidr-block))]}
  (go-try ch
    (let [c (chan)]
      (describe-vpc api credentials vpc c)
      (let [existing-vpc (<! c)]
        (if-let [id (:vpc-id (:vpc existing-vpc))]
          (aws/submit
           api
           (ec2/delete-vpc-map credentials {:vpc-id id})
           ch)
          (>! ch {:warning :no-such-vpc-found}))))))

(defn vpc-graph
  "Return a graph of vpc connectivity."
  [api credentials ch]
  (go-try ch
    (let [c-gw (chan)
          c-vpcs (chan)]
      (aws/submit api (ec2/describe-internet-gateways-map credentials {}) c-gw)
      (aws/submit api (ec2/describe-vpcs-map credentials {}) c-vpcs)
      (let [gws (<! c-gw)
            vpcs (<! c-vpcs)]
        (>! ch {:gws gws :vpcs vpcs})))))
