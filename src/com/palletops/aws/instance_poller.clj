(ns com.palletops.aws.instance-poller
  "A poller for instance-ids."
  (:require
   [clojure.core.async :refer [alts! chan go >! <! timeout]]
   [clojure.tools.logging :refer [log]]
   [com.palletops.awaze.ec2 :refer [describe-instances-map]]))

(defn debugf-id [fmt & args]
  (log 'aws.poller.id :debug nil (apply format fmt args)))

(defn debugf-poll [fmt & args]
  (log 'aws.poller.poll :debug nil (apply format fmt args)))

(defn tracef-poll [fmt & args]
  (log 'aws.poller.poll :trace nil (apply format fmt args)))

(defn exceptionf-poll
  [e fmt & args]
  (log 'aws.poller.poll :error e (apply format fmt args)))

(defn add-instance
  "Start polling an id. Writes the instance description to `channel`
  when the `notify-when-f` returns a truthy value when invoked on the
  instance description.  If `:remove?` is true (the default) the id is
  removed from the poller when the channel is notified."
  [{:keys [config] :as poller}
   id {:keys [channel notify-when-f remove?] :as options}]
  {:pre [id]}
  (debugf-id "add-instance %s" id)
  (swap! config update-in [:instance-id-map id] conj options))

(defn add-instances
  "Start polling an ids. `idmaps` is a map from id to a map with `:promise` and
  `:notify-when-f` keys. Notifies `promise` when the `notify-when-f` function
  returns a truthy value when called with the instance."
  [{:keys [config] :as poller} idmaps]
  {:pre [(every? string? (keys idmaps))
         (every? seq (vals idmaps))]}
  (debugf-id "add-instances %s" idmaps)
  (swap! config update-in [:instance-id-map] #(merge-with conj %1 %2) idmaps))

(defn remove-instance
  "Stop polling the specified id.
No need to call this if you pass a `notify-status` to `poll`."
  [{:keys [config] :as poller} id
   {:keys [channel notify-when-f] :as options}]
  (debugf-id "remove-instance %s" id)
  (swap! config update-in [:instance-id-map id] #(remove #{options} %)))

(defn- poll-request
  "Returns a request for the specifed credentials instance-id-map."
  [credentials instance-id-map]
  (debugf-poll "poll-request asking for %s nodes" (count instance-id-map))
  (let [instance-id-map (dissoc instance-id-map)])
  (when (seq instance-id-map)
    (describe-instances-map
     credentials {:instance-ids (keys instance-id-map)})))

(defn- remove-notifiable
  "Remove an notifiable ids that have :remove?"
  [instance-id-map notifiable]
  (reduce
   (fn [id-map {:keys [remove? instance] :or {remove? true} :as req}]
     (if remove?
       (let [id (:instance-id instance)
             id-map (update-in
                     id-map [id]
                     (fn [reqs]
                       (remove #(= % (dissoc req :instance)) reqs)))]
         (if (seq (id-map id))
           id-map
           (dissoc id-map id)))
       id-map))
   instance-id-map
   notifiable))

(defn- update-poller
  "Update poller for the response from aws."
  [config {:keys [reservations] :as results}]
  ;; deliver replies and remove from poller if delivered
  (debugf-poll "update-poller")
  (tracef-poll "update-poller %s" results)
  (let [notifiable (->>
                    reservations
                    (mapcat :instances)
                    (mapcat (fn [instance]
                              (map
                               #(assoc % :instance instance)
                               ((:instance-id-map config)
                                (:instance-id instance)))))
                    (filter (fn [{:keys [notify-when-f instance]}]
                              (if notify-when-f
                                (notify-when-f instance)
                                true))))]
    (-> config
        (update-in [:instance-id-map] remove-notifiable notifiable)
        (assoc ::notifiable notifiable))))

(defn start
  "Start a poller using the specified options.
  `:poll-interval` is in milliseconds.
  Returns a map with the poller."
  [{:keys [credentials poll-interval api-channel instance-id-map
           poll-timeout]
    :or {poller-channel (chan)
         instance-id-map (atom {})
         poll-timeout 10000
         poll-interval 2000}
    :as options}]
  {:pre [api-channel]}
  (debugf-poll "start")
  (let [shutdown (atom nil)
        config (atom {:instance-id-map {}})
        poller (assoc options
                 :config config
                 :shutdown shutdown)
        channel (chan)]
    (go
     (loop [_ true]
       ;; (debugf-poll "poll loop")
       (try
         (let [id-map (:instance-id-map @config)]
           (when (seq id-map)
             (debugf-poll "polling")
             (tracef-poll "polling %s" id-map)
             (>! api-channel
                 (assoc (poll-request credentials id-map)
                   :reply-channel channel))
             (tracef-poll "polling request sent")
             (let [[v c] (alts! [channel (timeout poll-timeout)])
                   _ (tracef-poll "polling reply %s" v)
                   updated (swap! config update-poller v)
                   _ (tracef-poll "polling updated %s" updated)
                   notifiable (::notifiable updated)]
               (debugf-poll "notifiable %s" (count notifiable))
               (doseq [{:keys [channel instance]} notifiable]
                 (tracef-poll "notify %s" instance)
                 (>! channel instance)))))
         (catch Exception e
           (exceptionf-poll e "Unexpected exception")))
       (if @shutdown
         (debugf-poll "poller finished")
         (recur (<! (timeout poll-interval))))))
    poller))

(defn stop
  "Stop polling"
  [{:keys [shutdown] :as poller}]
  (debugf-poll "stop")
  (reset! shutdown true))
