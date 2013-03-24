(ns naojure.core)

(def proxy-classes {
                    :audio-player "com.aldebaran.proxy.ALAudioPlayerProxy"
                    :backlight-detection "com.aldebaran.proxy.ALBacklightingDetectionProxy"
                    :battery "com.aldebaran.proxy.ALBatteryProxy"
                    :behaviour-manager "com.aldebaran.proxy.ALBehaviorManagerProxy"
                    :darkness-detection "com.aldebaran.proxy.ALDarknessDetectionProxy"
                    :leds "com.aldebaran.proxy.ALLedsProxy" 
                    :memory "com.aldebaran.proxy.ALMemoryProxy"
                    :motion "com.aldebaran.proxy.ALMotionProxy"
                    :navigation "com.aldebaran.proxy.ALNavigationProxy"
                    :photo-capture "com.aldebaran.proxy.ALPhotoCaptureProxy"
                    :posture "com.aldebaran.proxy.ALRobotPostureProxy"
                    :preferences "com.aldebaran.proxy.ALPreferencesProxy"
                    :sensors "com.aldebaran.proxy.ALSensorsProxy"
                    :sonar "com.aldebaran.proxy.ALSonarProxy"
                    :sound-detection "com.aldebaran.proxy.ALSoundDetectionProxy"
                    :speech-recognition "com.aldebaran.proxy.ALSpeechRecognitionProxy"
                    :tts "com.aldebaran.proxy.ALTextToSpeechProxy"
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

(def FRAME_TORSO 0)
(def FRAME_WORLD 1)
(def FRAME_ROBOT 2)

; From http://stackoverflow.com/questions/9167457/in-clojure-how-to-use-a-java-class-dynamically
(defn construct
  "Construct an instance of class from class and arguments."
  [klass & args]
  (clojure.lang.Reflector/invokeConstructor klass (into-array Object args)))

(defn make-proxy
  "Build a proxy from a robot and the symbol representing a proxy"
  [robot proxy-sym]
  (construct (Class/forName (proxy-classes proxy-sym))
             (:hostname robot) (:port robot)))

(defn add-proxies
  "Adds the proxies passed as symbols to the robot definition"
  [robot proxy-list]
  (merge robot
         (zipmap proxy-list (map #(make-proxy robot %)
                                 proxy-list))))

(defn make-robot
  "Constructs a map representing a robot including any requested proxies"
  ([hostname] {:hostname hostname :port 9559})
  ([hostname port] {:hostname hostname :port port})
  ([hostname port proxies]
     (add-proxies {:hostname hostname :port port} proxies)))

(defn get-proxy
  "Gets a proxy from a robot, constructing it if necessary"
  [robot proxy]
  (let [p (proxy robot)]
    (if (nil? p) (make-proxy robot proxy) p)))

(defn make-variant
  "Make instance of Aldebaran ALValue (Variant in java)"
  [val]
  (new com.aldebaran.proxy.Variant val))

(defn get-joint-angles
  "Return a map of the current joint angles for a robot"
  [robot]
  (zipmap joint-names
          (.getAngles (get-proxy robot :motion)
                      (make-variant "Body") true)))

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

(defn say
  "Say something"
  [robot text]
  (.say (get-proxy robot :tts) text))

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