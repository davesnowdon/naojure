(ns naojure.core
  (:require [clojure.core.async :as async
             :refer [<! >! <!! timeout chan alts! alts!! put! go]]
            [naojure.util :refer :all]
            [naojure.motion-calc :refer :all]
            [clojure.pprint :refer [pprint]]))

(def proxy-names {
                    :audio-player "ALAudioPlayer"
                    :backlight-detection "ALBacklightingDetection"
                    :battery "ALBattery"
                    :behaviour-manager "ALBehaviorManager"
                    :darkness-detection "ALDarknessDetection"
                    :leds "ALLeds"
                    :memory "ALMemory"
                    :motion "ALMotion"
                    :navigation "ALNavigation"
                    :photo-capture "ALPhotoCapture"
                    :posture "ALRobotPosture"
                    :preferences "ALPreferences"
                    :sensors "ALSensors"
                    :sonar "ALSonar"
                    :sound-detection "ALSoundDetection"
                    :speech-recognition "ALSpeechRecognition"
                    :tts "ALTextToSpeech"
                    })

(def joint-names ["HeadYaw", "HeadPitch",
                  "LShoulderPitch", "LShoulderRoll", "LElbowYaw", "LElbowRoll",
                  "LWristYaw", "LHand",
                  "LHipYawPitch", "LHipRoll", "LHipPitch",
                  "LKneePitch", "LAnklePitch", "LAnkleRoll",
                  "RHipYawPitch", "RHipRoll", "RHipPitch",
                  "RKneePitch",  "RAnklePitch", "RAnkleRoll",
                  "RShoulderPitch", "RShoulderRoll", "RElbowYaw", "RElbowRoll",
                  "RWristYaw", "RHand"])

(def postures {:crouch "Crouch"
               :lying-back "LyingBack"
               :lying-belly "LyingBelly"
               :sit "Sit"
               :sit-relax "SitRelax"
               :stand "Stand"
               :stand-init "StandInit"
               :stand-zero "StandZero"})

(def FRAME_TORSO 0)
(def FRAME_WORLD 1)
(def FRAME_ROBOT 2)

; From http://stackoverflow.com/questions/9167457/in-clojure-how-to-use-a-java-class-dynamically
;; (defn construct
;;   "Construct an instance of class from class and arguments."
;;   [klass & args]
;;   (clojure.lang.Reflector/invokeConstructor klass (into-array Object args)))

;; (defn make-proxy
;;   "Build a proxy from a robot and the symbol representing a proxy"
;;   [robot proxy-sym]
;;   (construct (Class/forName (proxy-names proxy-sym))
;;              (:hostname robot) (:port robot)))

(defn- make-proxy
  "Build a proxy from a session and the symbol for the service"
  [robot proxy-sym]
  (.service (:session robot) (proxy-names proxy-sym)))

(defn- add-proxies
  "Adds the proxies passed as symbols to the robot definition"
  [robot proxy-list]
  (merge robot
         (zipmap proxy-list (map #(make-proxy robot %)
                                 proxy-list))))

(defn- make-application
  "Creates an instance of an application object"
  []
  (com.aldebaran.qimessaging.Application.))

(defn- make-session
  "Create a session connected to a robot"
  [hostname port]
  (let [session (com.aldebaran.qimessaging.Session.)
        fut (.connect session (str "tcp://" hostname ":" port))]
    (locking fut
      (.wait fut 1000))
    session))

(defn- attach-session
  "Attach the java application and session objects required to interact with the Aldebaran API"
  [robot]
  (let [app (make-application)
        session (make-session (:hostname robot) (:port robot))]
    (assoc robot :application app :session session)))

(defn make-robot
  "Constructs a map representing a robot including any requested proxies. Most functions will construct the proxies they need on-the-fly if they are not already defined but since these are not stored it is more efficient to ask for any needed proxies in the call to make-robot."
  ([hostname] (attach-session {:hostname hostname :port 9559}))
  ([hostname port] (attach-session {:hostname hostname :port port}))
  ([hostname port proxies]
     (add-proxies (attach-session {:hostname hostname :port port}) proxies)))

(defn get-proxy
  "Gets a proxy from a robot, constructing it if necessary"
  [robot proxy]
  (let [p (proxy robot)]
    (if (nil? p) (make-proxy robot proxy) p)))

(defn future-callback-wrapper
  ([future success]
     (.addCallback future
                     (com.davesnowdon.naojure.CallbackWrapper.
                      success)
                     (into-array Object [])))

  ([future success failure]
     (.addCallback future
                     (com.davesnowdon.naojure.CallbackWrapper.
                      success failure)
                     (into-array Object [])))

  ([future success failure complete]
     (.addCallback future
                   (com.davesnowdon.naojure.CallbackWrapper.
                    success failure complete)
                   (into-array Object [])))
  )

;; function to use as placeholder in callback wrapper when no
;; action is required
(defn- do-nothing [])

(defn future-callback-on-complete
  [future complete-fn]
  (future-callback-wrapper future do-nothing do-nothing complete-fn))

(defn call-service
  "Call an operation on a service"
  ([robot proxy-sym operation]
     (.call (get-proxy robot proxy-sym) operation
            (into-array Object [])))

  ([robot proxy-sym operation params]
     (.call (get-proxy robot proxy-sym) operation
            (into-array Object params)))

  ([robot proxy-sym operation params success-fn]
     (-> (call-service robot proxy-sym operation params)
         (future-callback-wrapper success-fn)))

  ([robot proxy-sym operation params success-fn failure-fn]
     (-> (call-service robot proxy-sym operation params)
         (future-callback-wrapper success-fn failure-fn)))

  ([robot proxy-sym operation params success-fn failure-fn complete-fn]
     (-> (call-service robot proxy-sym operation params)
         (future-callback-wrapper success-fn failure-fn complete-fn)))
  )

;; event handling
(defn- event-dispatcher
  "Calls callback functions and sends data on channel. Expects atom containign map of channel to use and possibly empty sequence of functions"
  [event value event-state]
  (do
;;    (println "event-dispatcher" event)
    (doseq [cb (:callbacks @event-state)] (cb event value))
    (go (doseq [ch (:channels @event-state)] (>! ch [event value])))))

(defn callback->channel
  "Sends value on channel when function called"
  [ch]
  (fn [v] (put! ch v)))

(defn- make-event-wrapper
  "Creates a wrapper than QiMessaging can use as a callback and returns a robot with an atom holding the state in its events map"
  [robot event]
  (let [memory (get-proxy robot :memory)
        subscriber (.get (.call memory "subscriber" (into-array [event])))
        event-state (atom {:event event
                           :subscriber subscriber
                           :channels #{}
                           :callbacks #{}})
        wrapper (com.davesnowdon.naojure.InvokeWrapper.
                 event event-dispatcher event-state)]
    (do
      (swap! event-state assoc :wrapper wrapper)
      (.connect subscriber "signal::(m)" "invoke::(m)" wrapper)
      (assoc-in robot [:events event] event-state))))

(defn- get-event-state
  [robot event]
  (get-in robot [:events event]))

(defn- get-robot-with-wrapper
  "Return map of robot and event state"
  [robot event]
  (if-let [event-state (get-event-state robot event)]
    {:robot robot
     :event-state event-state}
    (let [new-robot (make-event-wrapper robot event)]
      {:robot new-robot
       :event-state (get-event-state new-robot event)})))

(defn add-event-handler
  "Arranges for the specified function to be called on event"
  [robot event handler-fn]
  (let [{new-robot :robot event-state :event-state}
        (get-robot-with-wrapper robot event)
        callbacks (:callbacks @event-state)]
    (swap! event-state assoc :callbacks (conj callbacks handler-fn))
    new-robot))

(defn remove-event-handler
  "Remove the specified function from set of callbacks for event. Channels not affected."
  [robot event handler-fn]
  (let [event-state (get-event-state robot event)
        callbacks (:callbacks @event-state)]
    (swap! event-state assoc :callbacks (disj callbacks handler-fn))
    robot))

(defn clear-event-handlers
  "Remove all callbacks for specified event. Channels not affected."
  [robot event]
  (let [event-state (get-event-state robot event)]
    (swap! event-state assoc :callbacks #{})
    robot))

(defn add-event-chan
  "Arranges values from event to be send on specified channel"
  [robot event ch]
  (let [{new-robot :robot event-state :event-state}
        (get-robot-with-wrapper robot event)
        channels (:channels @event-state)]
    (swap! event-state assoc :channels (conj channels ch))
    new-robot))

(defn remove-event-chan
  "Remove the specified channel from set of channels for event. Callbacks not affected."
  [robot event ch]
  (let [event-state (get-event-state robot event)
        channels (:channels @event-state)]
    (swap! event-state assoc :channels (disj channels ch))
    robot))

(defn clear-event-chan
  "Remove all channels for specified event. Callbacks not affected."
  [robot event]
  (let [event-state (get-event-state robot event)]
    (swap! event-state assoc :channels #{})
    robot))

;; TODO implement means to tell subscribe to stop notifying for given event
(defn clear-event
  "Stop subsribe from sending events"
  [robot event]
  (println "clear-event not implemented yet!"))

(defn start-event-loop
  "Starts the Qimessaging Application event loop - there can only be one of these running"
  [robot]
  (.start (Thread. (fn [] (.run (:application robot))))))

(defn stop-event-loop
  "Stops the Qimessaging application event loop"
  [robot]
  (.stop (:application robot)))

; behaviour management
(defn get-installed-behaviours
  "Return list of behaviours installed on robot"
  [robot]
  (->
   (call-service robot :behaviour-manager "getInstalledBehaviors" )
   (.get)))

(defn get-running-behaviours
  "Get list of running behaviours"
  [robot]
  (->
   (call-service robot :behaviour-manager "getRunningBehaviors")
   (.get)))

(defn get-default-behaviours
  "Get list of default behaviours (behaviours set to run automatically)"
  [robot]
  (->
   (call-service robot :behaviour-manager "getDefaultBehaviors" )
   (.get)))

(defn add-default-behaviour
  "Make the named behaviour auto run on robot boot"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "addDefaultBehavior" [behaviour-name]))

(defn remove-default-behaviour
  "Stop the named behaviour auto running on robot boot"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "removeDefaultBehavior" [behaviour-name]))

(defn- get-behaviour-future
  "Return Future for when behaviour will complete"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "runBehavior" [behaviour-name]))

(defmulti run-behaviour (fn [robot behaviour & args]
                          (cond
                           (= 0 (count args)) :default
                           (fn? (first args)) :callback
                           :else :channel)))

(defmethod run-behaviour :default [robot behaviour]
  (->
   (get-behaviour-future robot behaviour)
   (.get)))

(defmethod run-behaviour :callback
  ([robot behaviour success]
     (-> (get-behaviour-future robot behaviour)
         (future-callback-wrapper success)))

  ([robot behaviour success failure]
     (-> (get-behaviour-future robot behaviour)
         (future-callback-wrapper success failure)))

  ([robot behaviour success failure complete]
     (-> (get-behaviour-future robot behaviour)
         (future-callback-wrapper success failure complete)))
  )

(defmethod run-behaviour :channel
  ([robot behaviour success]
     (run-behaviour robot behaviour
                    (callback->channel success)))

  ([robot behaviour success failure]
     (run-behaviour robot behaviour
                    (callback->channel success)
                    (callback->channel failure)))

  ([robot behaviour success failure complete]
     (run-behaviour robot behaviour
                    (callback->channel success)
                    (callback->channel failure)
                    (callback->channel complete)))
  )

(defn behaviour-present
  "Is the named behaviour present?"
  [robot behaviour-name]
  (->
   (call-service robot :behaviour-manager "isBehaviorPresent"
                 [behaviour-name])
   (.get)))

(defn behaviour-running
  "Is the named behaviour running"
  [robot behaviour-name]
  (->
   (call-service robot :behaviour-manager "isBehaviorRunning"
                 [behaviour-name])
   (.get)))

; memory
(defn get-memory-keys
  "List memory keys"
  [robot]
  (->
   (call-service robot :memory "getDataListName")
   (.get)))

(defn get-memory-keys-containing
  "List memory keys containing string value"
  [robot val]
  (->
   (call-service robot :memory"getDataList"  [val])
   (.get)))

(defn get-memory-value
   "Get named value from memory"
   [robot key]
   (->
    (call-service robot :memory "getData" [key])
    (.get)))

(defn get-memory-values
   "Get values for list of named keys"
   [robot keys]
   (->
    (call-service robot :memory "getData"
                  [(to-array keys)])
    (.get)))

; posture
(defn get-posture-list
  "Get list of defined postures from robot"
  [robot]
  (->
   (call-service robot :posture "getPostureList")
   (.get)))

(defn go-to-posture
  "Move to the specified posture (use symbol). relative-speed is between zero and one"
  [robot posture relative-speed]
  (call-service robot :posture "goToPosture" [(postures posture) relative-speed]))

(defn crouch
  "Crouch"
  ([robot]
     (go-to-posture robot :crouch (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :crouch speed)))

(defn lie-on-back
  "Lie on back"
  ([robot]
     (go-to-posture robot :lying-back (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :lying-back speed)))

(defn lie-on-belly
  "Lie on belly"
  ([robot]
     (go-to-posture robot :lying-belly (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :lying-belly speed)))

(defn sit
  "Sit"
  ([robot]
     (go-to-posture robot :sit (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :sit speed)))

(defn sit-relaxed
  "Relaxed sit"
  ([robot]
     (go-to-posture robot :sit-relax (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :sit-relax speed)))

(defn stand
  "Stand up"
  ([robot]
     (go-to-posture robot :stand (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :stand speed)))

(defn stand-init
  "Stand up (init)"
  ([robot]
     (go-to-posture robot :stand-init (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :stand-init speed)))

(defn stand-zero
  "Stand up (zero)"
  ([robot]
     (go-to-posture robot :stand-zero (Float. 1.0)))
  ([robot speed]
     (go-to-posture robot :stand-zero speed)))

; motion
(defn wake-up
  "The robot wakes up: sets Motor on and, if needed, goes to initial position. For example, H25 or H21 sets the Stiffness on and keeps is current position."
  [robot]
  (call-service robot :motion "wakeUp"))

(defn relax
  "The robot rests: goes to a relax and safe position and sets Motor off. For example, H25 or H21 goes to the Crouch posture and sets the Stiffness off."
  [robot]
  (call-service robot :motion "rest"))

(defn walk
  "Walk at the specified speed. x, y, & theta represent normalised velocities from 0.0 to 1.0"
  [robot x y theta]
  (call-service robot :motion "moveToward" [x y theta]))

(defn walk-to
  "Walk to a specified location in the robot's frame of reference"
  [robot x y theta]
  (call-service robot :motion "moveTo" [x y theta]))

(defn- get-joint-angles-internal
  "Return a map of the current joint angles for a robot"
  [robot useSensors]
  (->> (call-service robot :motion "getAngles" ["Body" useSensors])
      (.get)
      (zipmap joint-names)))

(defn get-joint-angles
  ([robot] (get-joint-angles-internal robot Boolean/TRUE))
  ([robot useSensors] (get-joint-angles-internal useSensors)))

(defn get-body-names
  "Return names of joints in the specified chain"
  [robot name]
  (->
   (call-service robot :motion "getBodyNames"  [name])
   (.get)))

(defn get-joint-limits
  "Get the minAngle (rad), maxAngle (rad), maxVelocity (rad.s-1) and maxTorque (N.m). for a given joint or actuator in the body."
  [robot]
  (->>
   (call-service robot :motion "getLimits" ["Body"])
   (.get)
   (zipmap joint-names)))

(defn save-joint-limits
  "Get joint limit information and save it in robot structure"
  [robot]
  (assoc robot :joint-limits (get-joint-limits robot)))

(defn load-joint-limits
  "Get joint limits from robot map or retrieve them from NAOqi"
  [robot]
  (if-let [limits (:joint-limits robot)]
    limits
    (get-joint-limits robot)))

;; broken - can't construct variant holding array of values
(defn set-joint-angles
  "Set the named joints to absolute changes"
  [robot names angles speed]
  (call-service robot :motion "setAngles"
              [(to-array names)
              (to-array angles) speed]))

;; broken - can't construct variant holding array of values
(defn change-joint-angles
  "Relative changes to joint angles"
  [robot names changes speed]
    (call-service robot :motion "changeAngles"
                   [(to-array names)
                   (to-array changes) speed]))

(defn get-robot-position
  "Get the current position of the robot using the sensors if true"
  [robot use-sensors]
  (->
   (call-service robot :motion "getRobotPosition"  [use-sensors])
   (.get)))

(defn get-position
  "Get the current 6D position (x, y, z, wx, wy, wz) of named component, using the sensors if use-sensors is true"
  [robot name space use-sensors]
  (->
   (call-service robot :motion "getPosition" [name space use-sensors])
   (.get)))

(defn get-sensor-names
  "Get the names of the available sensors"
  [robot]
  (->
   (call-service robot :motion "getSensorNames")
   (.get)))

; text to speech
(defn say
  "Say something"
  [robot text]
  (call-service robot :tts "say" [text]))

(defn get-language
  "Get the currently defined text-to-speech language"
  [robot]
  (->
   (call-service robot :tts "getLanguage")
   (.get)))

(defn set-language
  "Set the language the robot should use"
  [robot language]
  (call-service robot :tts "setLanguage" [language]))

(defn get-volume
  "Get the current volume for the robot"
  [robot]
  (->
   (call-service robot :tts "getVolume")
   (.get)))

(defn set-volume
  "Set the volume used by the robot"
  [robot vol]
  (call-service robot :tts "setVolume" [(float vol)]))

;; motion
(defn head
  [action & params]
  (let [angle1 (get params 0 0)]
    (({:left head-left
       :right head-right
       :forward head-forward
       :up head-up
       :down down-down
       :centre head-centre
       :center head-centre
       :stiff head-stiff
       :relax head-relax
       } action) angle1)))

(defn arms
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:forward arms-forward
       :left-forward arms-left-forward
       :right-forward arms-right-forward
       :out arms-out
       :left-out arms-left-out
       :right-out arms-right-out
       :up arms-up
       :left-up arms-left-up
       :right-up arms-right-up
       :down arms-down
       :left-down arms-left-down
       :right-down arms-right-down
       :back arms-back
       :left-back arms-left-back
       :right-back arms-right-back
       :left-stiff arms-left-stiff
       :right-stiff arms-right-stiff
       :stiff arms-stiff
       :left-relax arms-left-relax
       :right-relax arms-right-relax
       :relax arms-relax
       } action) angle1 angle2)))

(defn elbows
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-bent elbows-left-bent
       :right-bent elbows-right-bent
       :bent elbows-bent
       :left-straight elbows-left-straight
       :right-straight elbows-right-straight
       :straight elbows-straight
       :left-turn-up elbows-left-turn-up
       :right-turn-up elbows-right-turn-up
       :turn-up elbows-turn-up
       :left-turn-down elbows-left-turn-down
       :right-turn-down elbows-right-turn-down
       :turn-down elbows-turn-down
       :left-turn-in elbows-left-turn-in
       :right-turn-in elbows-right-turn-in
       :turn-in elbows-turn-in
       } action) angle1 angle2)))

(defn wrists
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-centre wrists-left-centre
       :left-center wrists-left-centre
       :right-centre wrists-right-centre
       :right-center wrists-right-centre
       :centre wrists-centre
       :center wrists-centre
       :left-out wrists-left-out
       :right-out wrists-right-out
       :out wrists-out
       :left-in wrists-left-in
       :right-in wrists-right-in
       :in wrists-in
       } action) angle1 angle2)))

(defn hands
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-open hands-left-open
       :right-open hands-right-open
       :open hands-open
       :left-close hands-left-close
       :right-close hands-right-close
       :close hands-close
       } action) angle1 angle2)))

(defn legs
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-forward legs-left-forward
       :right-forward legs-right-forward
       :left-out legs-left-out
       :right-out legs-right-out
       :left-up legs-left-up
       :right-up legs-right-up
       :left-down legs-left-down
       :right-down legs-right-down
       :left-straight legs-left-straight
       :right-straight legs-right-straight
       :left-stiff legs-left-stiff
       :right-stiff legs-right-stiff
       :stiff legs-stiff
       :left-relax legs-left-relax
       :right-relax legs-right-relax
       :relax legs-relax
       } action) angle1 angle2)))

(defn knees
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-up knees-left-up
       :right-up knees-right-up
       :left-bent knees-left-bent
       :right-bent knees-right-bent
       } action) angle1 angle2)))

(defn feet
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left-point-toes feet-left-point-toes
       :right-point-toes feet-right-point-toes
       :point-toes feet-point-toes
       :left-raise-toes feet-left-raise-toes
       :right-raise-toes feet-right-raise-toes
       :raise-toes feet-raise-toes
       :left-turn-out feet-left-turn-out
       :right-turn-out feet-right-turn-out
       :turn-out feet-turn-out
       :left-turn-in feet-left-turn-in
       :right-turn-in feet-right-turn-in
       :turn-in feet-turn-in
       :left-centre feet-left-centre
       :left-center feet-left-centre
       :right-centre feet-right-centre
       :right-center feet-right-centre
       :centre feet-centre
       :center feet-centre
       } action) angle1 angle2)))

(defn- filter-actions
  "Return only the values of joint actions"
  [selector actions]
  (->> actions
       (map selector)
       (filter identity)
       (apply merge)))

(defn- clip
  [value min max]
  (cond
   (> value max) max
   (< value min) min
   :else value))

(defn- compute-joint-speed
  "Compute fraction of max speed for joint movement in radians"
  [execution-time-seconds cur-pos-rad desired-pos-rad limits]
  (let [max-change (nth limits 2)
        desired-change (Math/abs (- cur-pos-rad desired-pos-rad))
        speed (/ desired-change (* max-change execution-time-seconds))]
    (clip speed 0.01 1.0)))

(defn- motion-task
  [robot joint-name joint-pos-deg execution-time-seconds cur-joints limits]
  (let [pos-rad (Math/toRadians joint-pos-deg)
        speed-fraction (compute-joint-speed execution-time-seconds
                                            (cur-joints joint-name)
                                            pos-rad
                                            (limits joint-name))]
    (call-service robot :motion "angleInterpolationWithSpeed"
                  [joint-name
                   (java.util.ArrayList. (list (Float. pos-rad)))
                   (Float. speed-fraction)])))

(defn- do-joints
  "Takes a map of joint changes and a duration and builds a motion task for each joint with the appropriate speed"
  [robot duration joint-changes]
  (if (seq joint-changes)
    (let [cur-joints (get-joint-angles-internal robot Boolean/FALSE)
          limits (load-joint-limits robot)]
      (map (fn [[j v]] (motion-task robot j v duration cur-joints limits))
           joint-changes))))

(defn- stiffness-task
  [robot chain-name stiffness execution-time-seconds]
  (call-service robot :motion "stiffnessInterpolation"
                [chain-name (Float. stiffness)
                 (Float. execution-time-seconds)]))

(defn- do-stiffness
  "Takes a map of stiffness changes and a duration and builds tasks for"
  [robot duration stiffness-changes]
  (if (seq stiffness-changes)
    (map (fn [[c s]] (stiffness-task c s duration)) stiffness-changes)))

(defn-  parse-args
  "Parse an argument list and separate options from other params"
  [args]
  (loop [options {} params args]
    (if (seq params)
      (let [h (first params) t (rest params)]
        (if (keyword h)
          (recur (assoc options h (first t)) (rest t))
          [options params]))
      [options params])))

;; returns function that sends value to specified channel after
;; it has been called the specified number of times
(defn- put-when-zero
  [num-calls ch done-val]
  (let [counter (atom num-calls)]
    (fn [v] (if (= 0 (swap! counter dec))
             (put! ch done-val)))))

(defn- put-when-all-complete
  "Take a sequence of futures and wait for them all to complete"
  [futures done-chan]
  (if (seq futures)
    (let [complete-fn (put-when-zero (count futures) done-chan true)]
      (dorun (map #(future-callback-on-complete % complete-fn) futures))
      done-chan)))

;; TODO ability to use channel or callback on completion
;; TODO handle tasks other than motion
(defn donao
  "Accepts a number of parameters specifying robot actions and executes them. Options can be specified by preceding them with keywords before the list of actions. For example (donao :duration 1 :channel 2 ...)"
  [robot & args]
  (let [[options actions] (parse-args args)
        duration (:duration options 1.0)
        callback (:callback options)
        wait-chan (:wait-channel options)
        wait-timeout (:wait-timeout options)
        done-chan (:done-channel options)
        done-timeout (:done-timeout options)
        is-blocking (and (nil? done-chan) (nil? callback))
        joint-changes (filter-actions :joints actions)
        stiffness-changes (filter-actions :stiffness actions)
        go-result-chan
        (go
         ;; wait to be triggered
         (if wait-chan
           (let [channels
                 (if (wait-timeout)
                   [wait-chan (timeout wait-timeout)]
                   [wait-chan])]
             (alts! channels :priority true)))

         ;; wait to trigger tasks until they are required
         (let [stiffness-tasks (do-stiffness robot duration
                                             stiffness-changes)
               joint-tasks (do-joints robot duration joint-changes)
               all-tasks (concat stiffness-tasks joint-tasks)]
           (cond
            ;; send true on done-chan when all tasks done
            (not (nil? done-chan))
            (put-when-all-complete all-tasks done-chan)

            ;; invoke callback when all tasks done
            (not (nil? callback))
            (do (<! (put-when-all-complete all-tasks (chan)))
                (callback true))

            ;; wait for all tasks to complete before returning
            :else
            (<! (put-when-all-complete all-tasks (chan)))))
         )]
    (if is-blocking
      (if done-timeout
        (alts!! [go-result-chan (timeout done-timeout)] :priority true)
        (<!! go-result-chan)))))
