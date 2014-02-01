(ns naojure.core
  (:require [clojure.core.async :as async
             :refer [<! >! timeout chan alt! go]]))

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

(def joint-names '("HeadYaw", "HeadPitch",
                   "LShoulderPitch", "LShoulderRoll", "LElbowYaw", "LElbowRoll",
                   "LWristYaw", "LHand",
                   "LHipYawPitch", "LHipRoll", "LHipPitch",
                   "LKneePitch", "LAnklePitch", "LAnkleRoll",
                   "RHipYawPitch", "RHipRoll", "RHipPitch",
                   "RKneePitch",  "RAnklePitch", "RAnkleRoll",
                   "RShoulderPitch", "RShoulderRoll", "RElbowYaw", "RElbowRoll",
                   "RWristYaw", "RHand"))

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

(defn call-service
  "Call an operation on a service"
  [robot proxy-sym operation params]
  (.call (get-proxy robot proxy-sym) operation (into-array params)))


;; event handling
(defn- event-dispatcher
  "Calls callback functions and sends data on channel. Expects atom containign map of channel to use and possibly empty sequence of functions"
  [event value event-state]
  (do
;;    (println "event-dispatcher" event)
    (doseq [cb (:callbacks @event-state)] (cb event value))
    (go (doseq [ch (:channels @event-state)] (>! ch [ event value])))))

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
  (call-service robot :behaviour-manager "getInstalledBehaviors" ))

(defn get-running-behaviours
  "Get list of running behaviours"
  [robot]
  (call-service robot :behaviour-manager "getRunningBehaviors"))

(defn get-default-behaviours
  "Get list of default behaviours (behaviours set to run automatically)"
  [robot]
  (call-service robot :behaviour-manager "getDefaultBehaviors" ))

(defn add-default-behaviour
  "Make the named behaviour auto run on robot boot"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "addDefaultBehavior" [behaviour-name]))

(defn remove-default-behaviour
  "Stop the named behaviour auto running on robot boot"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "removeDefaultBehavior" [behaviour-name]))

(defn run-behaviour
  "Run the named behaviour"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "runBehavior" [behaviour-name]))

(defn behaviour-present
  "Is the named behaviour present?"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "isBehaviorPresent" [behaviour-name]))

(defn behaviour-running
  "Is the named behaviour running"
  [robot behaviour-name]
  (call-service robot :behaviour-manager "isBehaviorRunning"  [behaviour-name]))

; memory
(defn get-memory-keys
  "List memory keys"
  [robot]
  (call-service robot :memory "getDataListName"))

(defn get-memory-keys-containing
  "List memory keys containing string value"
  [robot val]
  (call-service robot :memory"getDataList"  [val]))

(defn get-memory-value
   "Get named value from memory"
   [robot key]
   (call-service robot :memory "getData" [key]))

(defn get-memory-values
   "Get values for list of named keys"
   [robot keys]
   (call-service robot :memory "getData"
             [(to-array keys)]))

; posture
(defn go-to-posture
  "Move to the specified posture (use symbol). relative-speed is between zero and one"
  [robot posture relative-speed]
  (call-service robot :posture "goToPosture" [(postures posture) relative-speed]))

(defn crouch
  "Crouch"
  ([robot]
     (go-to-posture robot :crouch 1.0))
  ([robot speed]
     (go-to-posture robot :crouch speed)))

(defn lie-on-back
  "Lie on back"
  ([robot]
     (go-to-posture robot :lying-back 1.0))
  ([robot speed]
     (go-to-posture robot :lying-back speed)))

(defn lie-on-belly
  "Lie on belly"
  ([robot]
     (go-to-posture robot :lying-belly 1.0))
  ([robot speed]
     (go-to-posture robot :lying-belly speed)))

(defn sit
  "Sit"
  ([robot]
     (go-to-posture robot :sit 1.0))
  ([robot speed]
     (go-to-posture robot :sit speed)))

(defn sit-relaxed
  "Relaxed sit"
  ([robot]
     (go-to-posture robot :sit-relax 1.0))
  ([robot speed]
     (go-to-posture robot :sit-relax speed)))

(defn stand
  "Stand up"
  ([robot]
     (go-to-posture robot :stand 1.0))
  ([robot speed]
     (go-to-posture robot :stand speed)))

(defn stand-init
  "Stand up (init)"
  ([robot]
     (go-to-posture robot :stand-init 1.0))
  ([robot speed]
     (go-to-posture robot :stand-init speed)))

(defn stand-zero
  "Stand up (zero)"
  ([robot]
     (go-to-posture robot :stand-zero 1.0))
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

(defn get-joint-angles
  "Return a map of the current joint angles for a robot"
  [robot]
  (zipmap joint-names
          (call-service robot :motion "getAngles"
                      ["Body" true])))
(defn get-body-names
  "Return names of joints in the specified chain"
  [robot name]
  (call-service robot :motion "getBodyNames"  [name]))

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
  (call-service robot :motion "getRobotPosition"  [use-sensors]))

(defn get-position
  "Get the current 6D position (x, y, z, wx, wy, wz) of named component, using the sensors if use-sensors is true"
  [robot name space use-sensors]
  (call-service robot :motion "getPosition" [name space use-sensors]))

(defn get-sensor-names
  "Get the names of the available sensors"
  [robot]
  (call-service robot :motion "getSensorNames"))

; text to speech
(defn say
  "Say something"
  [robot text]
  (call-service robot :tts "say" [text]))

(defn get-language
  "Get the currently defined text-to-speech language"
  [robot]
  (call-service robot :tts "getLanguage"))

(defn set-language
  "Set the language the robot should use"
  [robot language]
  (call-service robot :tts "setLanguage" [language]))

(defn get-volume
  "Get the current volume for the robot"
  [robot]
  (call-service robot :tts "getVolume"))

(defn set-volume
  "Set the volume used by the robot"
  [robot vol]
  (call-service robot :tts "setVolume" [(float vol)]))

;; motion
