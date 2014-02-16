(ns naojure.motion-calc
  (:require [naojure.util :as util :refer :all]))

;; head
(defn head-left [angle]
  {:joints {"HeadYaw" (+ 90.0 angle)}})

(defn head-right [angle]
  {:joints {"HeadYaw" (- -90.0 angle)}})

(defn head-forward [angle]
  {:joints {"HeadYaw" angle}})

(defn head-up [angle]
  {:joints {"HeadPitch" (- -38.0 angle)}})

(defn down-down [angle]
  {:joints {"HeadPitch" (+ 29 angle)}})

(defn head-centre [angle]
  {:joints {"HeadPitch" angle}})

(defn head-stiff [_]
  {:stiffness {"Head" 1.0}})

(defn head-relax [_]
  {:stiffness {"Head" 0.0}})


;; arms
(defn arms-left-forward [angle1 angle2]
  {:joints {"LShoulderPitch" (- angle1)
            "LShoulderRoll" angle2}})

(defn arms-right-forward [angle1 angle2]
  {:joints {"RShoulderPitch" (- angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms-forward [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms-left-forward arms-right-forward))

(defn arms-left-out [angle1 angle2]
  {:joints {"LShoulderPitch" (- angle1)
            "LShoulderRoll" (+ 90.0 angle2)}})

(defn arms-right-out [angle1 angle2]
  {:joints {"RShoulderPitch" (- angle1)
            "RShoulderRoll" (- -90.0 angle2)}})

(defn arms-out [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms-left-out arms-right-out))

(defn arms-left-up [angle1 angle2]
  {:joints {"LShoulderPitch" (- -90.0 angle1)
            "LShoulderRoll" angle2}})

(defn arms-right-up [angle1 angle2]
  {:joints {"RShoulderPitch" (- -90.0 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms-up [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms-left-up arms-right-up))

(defn arms-left-down [angle1 angle2]
  {:joints {"LShoulderPitch" (- 90.0 angle1)
            "LShoulderRoll" angle2}})

(defn arms-right-down [angle1 angle2]
  {:joints {"RShoulderPitch" (- 90.0 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms-down [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms-left-down arms-right-down))

(defn arms-left-back [angle1 angle2]
  {:joints {"LShoulderPitch" (- 119.5 angle1)
            "LShoulderRoll" angle2}})

(defn arms-right-back [angle1 angle2]
  {:joints {"RShoulderPitch" (- 119.5 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms-back [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms-left-back arms-right-back))

(defn arms-left-stiff [& _]
  {:stiffness {"LArm" 1.0}})

(defn arms-right-stiff [& _]
  {:stiffness {"RArm" 1.0}})

(defn arms-stiff [& _]
  (combine-stiffness-fns arms-left-stiff arms-right-stiff))

(defn arms-left-relax [& _]
  {:stiffness {"LArm" 0.0}})

(defn arms-right-relax [& _]
  {:stiffness {"RArm" 0.0}})

(defn arms-relax [& _]
  (combine-stiffness-fns arms-left-relax arms-right-relax))


;; elbows
(defn elbows-left-bent
  [angle1 angle2]
  {:joints {"LElbowRoll" (- -89.0 angle1)}})

(defn elbows-right-bent
  [angle1 angle2]
  {:joints {"RElbowRoll" (+ 89.0 angle1)}})

(defn elbows-bent
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows-left-bent elbows-right-bent))

(defn elbows-left-straight
  [angle1 angle2]
  {:joints {"LElbowRoll" (+ 0.5 angle1)}})

(defn elbows-right-straight
  [angle1 angle2]
  {:joints {"RElbowRoll" (- 0.5 angle1)}})

(defn elbows-straight
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows-left-straight elbows-right-straight))

(defn elbows-left-turn-up
  [angle1 angle2]
  {:joints {"LElbowYaw" (- -90.0 angle1)}})

(defn elbows-right-turn-up
  [angle1 angle2]
  {:joints {"RElbowYaw" (+ 90.0 angle1)}})

(defn elbows-turn-up
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows-left-turn-up elbows-right-turn-up))

(defn elbows-left-turn-down
  [angle1 angle2]
  {:joints {"LElbowYaw" (+ 90.0 angle1)}})

(defn elbows-right-turn-down
  [angle1 angle2]
  {:joints {"RElbowYaw" (- -90.0 angle1)}})

(defn elbows-turn-down
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows-left-turn-down elbows-right-turn-down))

(defn elbows-left-turn-in
  [angle1 angle2]
  {:joints {"LElbowYaw" angle1}})

(defn elbows-right-turn-in
  [angle1 angle2]
  {:joints {"RElbowYaw" (- angle1)}})

(defn elbows-turn-in
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows-left-turn-in elbows-right-turn-in))


;; wrists
(defn wrists-left-centre
  [angle1 angle2]
  {:joints {"LWristYaw" angle1}})

(defn wrists-right-centre
  [angle1 angle2]
  {:joints {"RWristYaw" (- angle1)}})

(defn wrists-centre
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 wrists-left-centre wrists-right-centre))

(defn wrists-left-out
  [angle1 angle2]
  {:joints {"LWristYaw" (+ 90.0 angle1)}})

(defn wrists-right-out
  [angle1 angle2]
  {:joints {"RWristYaw" (- -90.0 angle1)}})

(defn wrists-out
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 wrists-left-out wrists-right-out))

(defn wrists-left-in
  [angle1 angle2]
  {:joints {"LWristYaw" (- -90.0 angle1)}})

(defn wrists-right-in
  [angle1 angle2]
  {:joints {"RWristYaw" (+ 90.0 angle1)}})

(defn wrists-in
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 wrists-left-in wrists-right-in))


;; hands
(defn hands-left-open
  [angle1 angle2]
  {:joints {"LHand" (Math/toDegrees 1.0)}})

(defn hands-right-open
  [angle1 angle2]
  {:joints {"RHand" (Math/toDegrees 1.0)}})

(defn hands-open
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 hands-left-open hands-right-open))

(defn hands-left-close
  [angle1 angle2]
  {:joints {"LHand" (Math/toDegrees 0.0)}})

(defn hands-right-close
  [angle1 angle2]
  {:joints {"RHand" (Math/toDegrees 0.0)}})

(defn hands-close
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 hands-left-close hands-right-close))

(defn legs-left-forward
  [angle1 angle2]
  ;; TODO
  )


;; legs
(defn legs-right-forward
  [angle1 angle2]
  ;; TODO
  )

(defn legs-left-out
  [angle1 angle2]
  ;; TODO
  )

(defn legs-right-out
  [angle1 angle2]
  ;; TODO
  )

(defn legs-left-up
  [angle1 angle2]
  {:joints {"LHipPitch" (- -90.0 angle1)}})

(defn legs-right-up
  [angle1 angle2]
  {:joints {"RHipPitch" (- -90.0 angle1)}})

(defn legs-left-down
  [angle1 angle2]
  {:joints {"LHipPitch" angle1}})

(defn legs-right-down
  [angle1 angle2]
  {:joints {"RHipPitch" angle1}})

(defn legs-left-stiff  [& _]
  {:stiffness {"LLeg" 1.0}})

(defn legs-right-stiff  [& _]
  {:stiffness {"RLeg" 1.0}})

(defn legs-stiff [& _]
  (combine-stiffness-fns legs-left-stiff legs-right-stiff))

(defn legs-left-relax  [& _]
  {:stiffness {"LLeg" 0.0}})

(defn legs-right-relax  [& _]
  {:stiffness {"RLeg" 0.0}})

(defn legs-relax [& _]
  (combine-stiffness-fns legs-left-stiff legs-right-stiff))

;; knees
(defn knees-left-up
  [angle1 angle2]
  ;; TODO
  )

(defn knees-right-up
  [angle1 angle2]
  ;; TODO
  )

(defn knees-left-bent
  [angle1 angle2]
  {:joints {"LKneePitch" (+ 90.0 angle1)}})

(defn knees-right-bent
  [angle1 angle2]
  {:joints {"RKneePitch" (+ 90.0 angle1)}})

(defn knees-left-straight
  [angle1 angle2]
  {:joints {"LKneePitch" (- angle1)}})

(defn knees-right-straight
  [angle1 angle2]
  {:joints {"RKneePitch" (- angle1)}})

(defn legs-left-straight
  [angle1 angle2]
  (knees-left-straight angle1 angle2))

(defn legs-right-straight
  [angle1 angle2]
  (knees-right-straight angle1 angle2))


;; feet
(defn feet-left-point-toes
  [angle1 angle2]
  {:joints {"LAnklePitch" (+ 52.8 angle1)}})

(defn feet-right-point-toes
  [angle1 angle2]
  {:joints {"RAnklePitch" (+ 52.8 angle1)}})

(defn feet-point-toes
  [angle1 angle2]
  (combine-joint-fns angle1 angle2
                     feet-left-point-toes
                     feet-right-point-toes))

(defn feet-left-raise-toes
  [angle1 angle2]
  {:joints {"LAnklePitch" (- -68.0 angle1)}})


(defn feet-right-raise-toes
  [angle1 angle2]
  {:joints {"RAnklePitch" (- -68.0 angle1)}})

(defn feet-raise-toes
  [angle1 angle2]
  (combine-joint-fns angle1 angle2
                     feet-left-raise-toes
                     feet-right-raise-toes))

(defn feet-left-turn-out
  [angle1 angle2]
  {:joints {"LAnkleRoll" (+ 22.0 angle1)}})


(defn feet-right-turn-out
  [angle1 angle2]
  {:joints {"RAnkleRoll" (- -22.0 angle1)}})


(defn feet-turn-out
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 feet-left-turn-out feet-right-turn-out))


(defn feet-left-turn-in
  [angle1 angle2]
  {:joints {"LAnkleRoll" (- -22.8 angle1)}})


(defn feet-right-turn-in
  [angle1 angle2]
  {:joints {"RAnkleRoll" (+ 22.8 angle1)}})

(defn feet-turn-in
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 feet-left-turn-in feet-right-turn-in))

(defn feet-left-centre
  [angle1 angle2]
  {:joints {"LAnkleRoll" 0.0 "LAnklePitch" 0.0}})


(defn feet-right-centre
  [angle1 angle2]
  {:joints {"RAnkleRoll" 0.0 "RAnklePitch" 0.0}})


(defn feet-centre
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 feet-left-centre feet-right-centre))
