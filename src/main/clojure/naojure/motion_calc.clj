(ns naojure.motion_calc
  (:require [naojure.util :as util :refer :all]))

(defn arms_left_forward [angle1 angle2]
  {:joints {"LShoulderPitch" (- angle1)
            "LShoulderRoll" angle2}})

(defn arms_right_forward [angle1 angle2]
  {:joints {"RShoulderPitch" (- angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms_forward [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms_left_forward arms_right_forward))

(defn arms_left_out [angle1 angle2]
  {:joints {"LShoulderPitch" (- angle1)
            "LShoulderRoll" (+ 90 angle2)}})

(defn arms_right_out [angle1 angle2]
  {:joints {"RShoulderPitch" (- angle1)
            "RShoulderRoll" (- -90 angle2)}})

(defn arms_out [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms_left_out arms_right_out))

(defn arms_left_up [angle1 angle2]
  {:joints {"LShoulderPitch" (- -90 angle1)
            "LShoulderRoll" angle2}})

(defn arms_right_up [angle1 angle2]
  {:joints {"RShoulderPitch" (- -90 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms_up [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms_left_up arms_right_up))

(defn arms_left_down [angle1 angle2]
  {:joints {"LShoulderPitch" (- 90 angle1)
            "LShoulderRoll" angle2}})

(defn arms_right_down [angle1 angle2]
  {:joints {"RShoulderPitch" (- 90 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms_down [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms_left_down arms_right_down))

(defn arms_left_back [angle1 angle2]
  {:joints {"LShoulderPitch" (- 119.5 angle1)
            "LShoulderRoll" angle2}})

(defn arms_right_back [angle1 angle2]
  {:joints {"RShoulderPitch" (- 119.5 angle1)
            "RShoulderRoll" (- angle2)}})

(defn arms_back [angle1 angle2]
  (combine-joint-fns angle1 angle2 arms_left_back arms_right_back))

(defn elbows_left_bent
  [angle1 angle2]
  {:joints {"LElbowRoll" (- -89 angle1)}})

(defn elbows_right_bent
  [angle1 angle2]
  {:joints {"RElbowRoll" (+ 89 angle1)}})

(defn elbows_bent
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows_left_bent elbows_right_bent))

(defn elbows_left_straight
  [angle1 angle2]
  {:joints {"LElbowRoll" (+ 0.5 angle1)}})

(defn elbows_right_straight
  [angle1 angle2]
  {:joints {"RElbowRoll" (- 0.5 angle1)}})

(defn elbows_straight
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows_left_straight elbows_right_straight))

(defn elbows_left_turn_up
  [angle1 angle2]
  {:joints {"LElbowYaw" (- -90 angle1)}})

(defn elbows_right_turn_up
  [angle1 angle2]
  {:joints {"RElbowYaw" (+ 90 angle1)}})

(defn elbows_turn_up
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows_left_turn_up elbows_right_turn_up))

(defn elbows_left_turn_down
  [angle1 angle2]
  {:joints {"LElbowYaw" (+ 90 angle1)}})

(defn elbows_right_turn_down
  [angle1 angle2]
  {:joints {"RElbowYaw" (- -90 angle1)}})

(defn elbows_turn_down
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows_left_turn_down elbows_right_turn_down))

(defn elbows_left_turn_in
  [angle1 angle2]
  {:joints {"LElbowYaw" angle1}})

(defn elbows_right_turn_in
  [angle1 angle2]
  {:joints {"RElbowYaw" (- angle1)}})

(defn elbows_turn_in
  [angle1 angle2]
  (combine-joint-fns angle1 angle2 elbows_left_turn_in elbows_right_turn_in))