(ns naojure.events)

(def standard-events
  ["ALAnimatedSpeech/EndOfAnimatedSpeech"
   "ALAudioSourceLocalization/SoundLocated"
   "ALBasicAwareness/HumanLost"
   "ALBasicAwareness/HumanTracked"
   "ALBasicAwareness/StimulusDetected"
   "ALBehaviorManager/BehaviorAdded"
   "ALBehaviorManager/BehaviorRemoved"
   "ALBehaviorManager/BehaviorUpdated"
   "ALBehaviorManager/BehaviorsAdded"
   "ALChestButton/DoubleClickOccurred"
   "ALChestButton/SimpleClickOccurred"
   "ALChestButton/TripleClickOccurred"
   "ALMemory/KeyAdded"
   "ALMemory/KeyRemoved"
   "ALMemory/KeyTypeChanged"
   "ALMotion/Safety/ChainVelocityClipped"
   "ALMotion/Safety/MoveFailed"
   "ALMotion/Stiffness/restFinished"
   "ALMotion/Stiffness/restStarted"
   "ALMotion/Stiffness/wakeUpFinished"
   "ALMotion/Stiffness/wakeUpStarted"
   "ALPanoramaCompass/PanoramaConfidence"
   "ALPanoramaCompass/Setup/Complete"
   "ALPanoramaCompass/Setup/Diagnosis"
   "ALSoundLocalization/SoundLocated"
   "ALSpeechRecognition/IsRunning"
   "ALSpeechRecognition/Status"
   "ALStore/SystemImageDownloaded"
   "ALStore/Updated"
   "ALSystem/RobotNameChanged"
   "ALTextToSpeech/CurrentBookMark"
   "ALTextToSpeech/CurrentSentence"
   "ALTextToSpeech/CurrentWord"
   "ALTextToSpeech/PositionOfCurrentWord"
   "ALTextToSpeech/Status"
   "ALTextToSpeech/TextDone"
   "ALTextToSpeech/TextStarted"
   "ALTracker/BlobDetected"
   "ALTracker/CloseObjectDetected"
   "ALTracker/ColorBlobDetected"
   "ActiveDiagnosisErrorChanged"
   "AutonomousLife/CompletedActivity"
   "AutonomousLife/FocusedActivity"
   "AutonomousLife/LaunchSuggestions"
   "AutonomousLife/NeedPreload"
   "AutonomousLife/NextActivity"
   "AutonomousLife/State"
   "BacklightingDetection/BacklightingDetected"
   "BarcodeReader/BarcodeDetected"
   "BatteryChargeCellVoltageMinChanged"
   "BatteryChargeChanged"
   "BatteryChargingFlagChanged"
   "BatteryDisChargingFlagChanged"
   "BatteryEmpty"
   "BatteryFullChargedFlagChanged"
   "BatteryLowDetected"
   "BatteryNearlyEmpty"
   "BatteryNotDetected"
   "BatteryPowerPluggedChanged"
   "BehaviorsRun"
   "BodyStiffnessChanged"
   "ChestButtonPressed"
   "ClientConnected"
   "ClientDisconnected"
   "CloseObjectDetection/ObjectDetected"
   "CloseObjectDetection/ObjectNotDetected"
   "DarknessDetection/DarknessDetected"
   "DeviceNoLongerHotDetected"
   "Dialog/Answered"
   "Dialog/IsStarted"
   "Dialog/LastInput"
   "Dialog/NotSpeaking10"
   "Dialog/NotSpeaking15"
   "Dialog/NotSpeaking20"
   "Dialog/NotSpeaking5"
   "Dialog/NotUnderstood"
   "Dialog/SpeakLouder"
   "EngagementZones/MovementsInZonesUpdated"
   "EngagementZones/PeopleInZonesUpdated"
   "EngagementZones/PersonApproached"
   "EngagementZones/PersonEnteredZone1"
   "EngagementZones/PersonEnteredZone2"
   "EngagementZones/PersonEnteredZone3"
   "EngagementZones/PersonMovedAway"
   "FaceDetected"
   "FrontTactilTouched"
   "footContactChanged"
   "GazeAnalysis/PeopleLookingAtRobot"
   "GazeAnalysis/PersonStartsLookingAtRobot"
   "GazeAnalysis/PersonStopsLookingAtRobot"
   "HandLeftBackTouched"
   "HandLeftLeftTouched"
   "HandLeftRightTouched"
   "HandRightBackTouched"
   "HandRightLeftTouched"
   "HandRightRightTouched"
   "HotDeviceDetected"
   "HotJointDetected"
   "LandmarkDetected"
   "LastWordRecognized"
   "LeftBumperPressed"
   "MiddleTactilTouched"
   "MovementDetection/MovementDetected"
   "MovementDetection/NoMovement"
   "NAOqiReady"
   "Navigation/SafeNavigator/AlreadyAtTarget"
   "Navigation/SafeNavigator/BlockingObstacle"
   "Navigation/SafeNavigator/DangerousObstacleDetected"
   "Navigation/SafeNavigator/Status"
   "NetworkDefaultTechnologyChanged"
   "NetworkServiceAdded"
   "NetworkServiceInputRequired"
   "NetworkServiceRemoved"
   "NetworkServiceStateChanged"
   "NetworkStateChanged"
   "NetworkTechnologyAdded"
   "NetworkTechnologyRemoved"
   "notificationAdded"
   "notificationRemoved"
   "PassiveDiagnosisErrorChanged"
   "PeoplePerception/JustArrived"
   "PeoplePerception/JustLeft"
   "PeoplePerception/PeopleDetected"
   "PeoplePerception/PopulationUpdated"
   "PictureDetected"
   "PostureChanged"
   "PostureFamilyChanged"
   "preferenceAdded"
   "preferenceChanged"
   "preferenceDomainRemoved"
   "preferenceRemoved"
   "preferenceSynchronized"
   "RearTactilTouched"
   "RightBumperPressed"
   "redBallDetected"
   "robotHasFallen"
   "robotIsWakeUp"
   "robotPoseChanged"
   "Segmentation3D/BlobTrackerUpdated"
   "Segmentation3D/SegmentationUpdated"
   "Segmentation3D/TrackedBlobNotFound"
   "SittingPeopleDetection/PersonSittingDown"
   "SittingPeopleDetection/PersonStandingUp"
   "SonarLeftDetected"
   "SonarLeftNothingDetected"
   "SonarRightDetected"
   "SonarRightNothingDetected"
   "SoundDetected"
   "SpeechDetected"
   "TemperatureDiagnosisErrorChanged"
   "TouchChanged"
   "VisualCompass/Deviation"
   "VisualCompass/FinalDeviation"
   "VisualCompass/InvalidReference"
   "VisualCompass/Match"
   "VisualCompass/MoveAbort"
   "VisualCompass/NewReferenceImageSet"
   "WavingDetection/PersonWaving"
   "WavingDetection/PersonWavingCenter"
   "WavingDetection/PersonWavingLeft"
   "WavingDetection/PersonWavingRight"
   "WordRecognized"
   "WordRecognizedAndGrammar"])
