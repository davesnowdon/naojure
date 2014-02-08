(ns naojure.core
  (:require [clojure.core.async :as async
             :refer [<! >! timeout chan alt! put! go]]
            [naojure.util :refer :all]
            [naojure.motion_calc :refer :all]))

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
(defn arms
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:forward arms_forward
        :left_forward arms_left_forward
        :right_forward arms_right_forward
        :out arms_out
        :left_out arms_left_out
        :right_out arms_right_out
        :up arms_up
        :left_up arms_left_up
        :right_up arms_right_up
        :down arms_down
        :left_down arms_left_down
        :right_down arms_right_down
        :back arms_back
        :left_back arms_left_back
        :right_back arms_right_back
       } action) angle1 angle2)))

(defn elbows
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left_bent elbows_left_bent
       :right_bent elbows_right_bent
       :bent elbows_bent
       :left_straight elbows_left_straight
       :right_straight elbows_right_straight
       :straight elbows_straight
       :left_turn_up elbows_left_turn_up
       :right_turn_up elbows_right_turn_up
       :turn_up elbows_turn_up
       :left_turn_down elbows_left_turn_down
       :right_turn_down elbows_right_turn_down
       :turn_down elbows_turn_down
       :left_turn_in elbows_left_turn_in
       :right_turn_in elbows_right_turn_in
       :turn_in elbows_turn_in
       } action) angle1 angle2)))

(defn wrists
  [action & params]
  (let [angle1 (get params 0 0)
        angle2 (get params 1 0)]
    (({:left_centre wrists_left_centre
       :right_centre wrists_right_centre
       :centre wrists_centre
       :left_out wrists_left_out
       :right_out wrists_right_out
       :out wrists_out
       :left_in wrists_left_in
       :right_in wrists_right_in
       :in wrists_in
       } action) angle1 angle2)))

(defn- only-joint-actions
  "Return only the values of joint actions"
  [actions]
  (->> actions
       (map :joints)
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

;; TODO needs to return futures we can use to check when task completes
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

;; TODO ability to use channel or callback on completion
;; TODO handle tasks other than motion
(defn donao
  "Accepts a number of parameters specifying robot actions and executes them. Options can be specified by preceding them with keywords before the list of actions. For example (donao :duration 1 :channel 2 ...)"
  [robot & args]
  (let [[options actions] (parse-args args)
        duration (:duration options 1.0)
        callback (:callback options)
        channel (:channel options)
        joint-changes (only-joint-actions actions)
        joint-tasks (do-joints robot duration joint-changes)]
    joint-tasks))
