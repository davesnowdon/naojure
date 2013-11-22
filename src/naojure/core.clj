(ns naojure.core)

(def proxy-classes {
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
                    :speech-recognition "ALSpeechRecognitionProxy"
                    :tts "ALTextToSpeechProxy"
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
;;   (construct (Class/forName (proxy-classes proxy-sym))
;;              (:hostname robot) (:port robot)))

(defn make-proxy
  "Build a proxy from a session and the symbol for the service"
  [session proxy-sym]
  (.service session (proxy-classes proxy-sym)))

(defn add-proxies
  "Adds the proxies passed as symbols to the robot definition"
  [robot proxy-list]
  (merge robot
         (zipmap proxy-list (map #(make-proxy robot %)
                                 proxy-list))))

(defn make-robot
  "Constructs a map representing a robot including any requested proxies. Most functions will construct the proxies they need on-the-fly if they are not already defined but since these are not stored it is more efficient to ask for any needed proxies in the call to make-robot."
  ([hostname] {:hostname hostname :port 9559})
  ([hostname port] ( {:hostname hostname :port port}))
  ([hostname port proxies]
     (add-proxies {:hostname hostname :port port} proxies)))

(defn make-application
  "Creates an instance of an application object"
  []
  (com.aldebaran.qimessaging.Application.))


; java function to wait on the future?
(defn make-session
  "Create a session connected to a robot"
  [hostname port]
  (let [session (com.aldebaran.qimessaging.Session.)
        fut (.connect session (str "tcp://" hostname ":" port))]
        (.wait fut 1000)
        session))

(defn get-proxy
  "Gets a proxy from a robot, constructing it if necessary"
  [robot proxy]
  (let [p (proxy robot)]
    (if (nil? p) (make-proxy robot proxy) p)))

(defn call-service
  "Call an operation on a service"
  [robot proxy-sym operation params]
  (.call (get-proxy robot proxy-sym) operation (into-array params)))

;; (defn make-variant
;;   "Make instance of Aldebaran ALValue (Variant in java)"
;;   [val]
;;   (new com.aldebaran.proxy.Variant val))

; behaviour management
(defn get-installed-behaviours
  "Return list of behaviours installed on robot"
  [robot]
  (.getInstalledBehaviors (get-proxy robot :behaviour-manager)))

(defn get-running-behaviours
  "Get list of running behaviours"
  [robot]
  (.getRunningBehaviors (get-proxy robot :behaviour-manager)))

(defn get-default-behaviours
  "Get list of default behaviours (behaviours set to run automatically)"
  [robot]
  (.getDefaultBehaviors (get-proxy robot :behaviour-manager)))

(defn add-default-behaviour
  "Make the named behaviour auto run on robot boot"
  [robot behaviour-name]
  (.addDefaultBehavior (get-proxy robot :behaviour-manager) behaviour-name))

(defn remove-default-behaviour
  "Stop the named behaviour auto running on robot boot"
  [robot behaviour-name]
  (.removeDefaultBehavior (get-proxy robot :behaviour-manager) behaviour-name))

(defn run-behaviour
  "Run the named behaviour"
  [robot behaviour-name]
  (.runBehavior (get-proxy robot :behaviour-manager) behaviour-name))

(defn behaviour-present
  "Is the named behaviour present?"
  [robot behaviour-name]
  (.isBehaviorPresent (get-proxy robot :behaviour-manager) behaviour-name))

(defn behaviour-running
  "Is the named behaviour running"
  [robot behaviour-name]
  (.isBehaviorRunning (get-proxy robot :behaviour-manager) behaviour-name))

; memory
(defn get-memory-keys
  "List memory keys"
  [robot]
  (.getDataListName (get-proxy robot :memory)))

(defn get-memory-keys-containing
  "List memory keys containing string value"
  [robot val]
  (.getDataList (get-proxy robot :memory) val))

;; TODO need a way to unpack the Variant
;; currently get the following error when trying to extract values from the result
;; ClassNotFoundException com.aldebaran.proxy.Variant$typeV  java.net.URLClassLoader$1.run (URLClassLoader.java:366)
;; (defn get-memory-value
;;   "Get named value from memory"
;;   [robot key]
;;   (.getData (get-proxy robot :memory) key))

;; (defn get-memory-values
;;   "Get values for list of named keys"
;;   [robot keys]
;;   (.getData (get-proxy robot :memory)
;;             (make-variant (to-array keys))))

; posture
(defn go-to-posture
  "Move to the specified posture (use symbol). relative-speed is between zero and one"
  [robot posture relative-speed]
  (.goToPosture (get-proxy robot :posture) (postures posture) relative-speed))

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
  (.wakeUp (get-proxy robot :motion)))

(defn relax
  "The robot rests: goes to a relax and safe position and sets Motor off. For example, H25 or H21 goes to the Crouch posture and sets the Stiffness off."
  [robot]
  (.rest (get-proxy robot :motion)))

(defn walk
  "Walk at the specified speed. x, y, & theta represent normalised velocities from 0.0 to 1.0"
  [robot x y theta]
  (.moveToward (get-proxy robot :motion) x y theta))

(defn walk-to
  "Walk to a specified location in the robot's frame of reference"
  [robot x y theta]
  (.moveTo (get-proxy robot :motion) x y theta))

(defn get-joint-angles
  "Return a map of the current joint angles for a robot"
  [robot]
  (zipmap joint-names
          (.getAngles (get-proxy robot :motion)
                      (make-variant "Body") true)))
(defn get-body-names
  "Return names of joints in the specified chain"
  [robot name]
  (.getBodyNames (get-proxy robot :motion) name))

;; broken - can't construct variant holding array of values
(defn set-joint-angles
  "Set the named joints to absolute changes"
  [robot names angles speed]
  (.setAngles (get-proxy robot :motion)
              (to-array names)
              (to-array angles) speed))

;; broken - can't construct variant holding array of values
(defn change-joint-angles
  "Relative changes to joint angles"
  [robot names changes speed]
    (.changeAngles (get-proxy robot :motion)
                   (to-array names)
                   (to-array changes) speed))

(defn get-robot-position
  "Get the current position of the robot using the sensors if true"
  [robot use-sensors]
  (.getRobotPosition (get-proxy robot :motion) use-sensors))

(defn get-position
  "Get the current 6D position (x, y, z, wx, wy, wz) of named component, using the sensors if use-sensors is true"
  [robot name space use-sensors]
  (.getPosition (get-proxy robot :motion) name space use-sensors))

(defn get-sensor-names
  "Get the names of the available sensors"
  [robot]
  (.getSensorNames (get-proxy robot :motion)))

; text to speech
(defn say
  "Say something"
  [robot text]
  (.call (get-proxy robot :tts) "say" (into-array [text])))

(defn get-language
  "Get the currently defined text-to-speech language"
  [robot]
  (.getLanguage (get-proxy robot :tts)))

(defn set-language
  "Set the language the robot should use"
  [robot language]
  (.setLanguage (get-proxy robot :tts) language))

(defn get-volume
  "Get the current volume for the robot"
  [robot]
  (.getVolume (get-proxy robot :tts)))

(defn set-volume
  "Set the volume used by the robot"
  [robot vol]
  (.setVolume (get-proxy robot :tts) (float vol)))
