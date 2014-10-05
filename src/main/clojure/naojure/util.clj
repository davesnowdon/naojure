(ns naojure.util)

(def not-nil? (complement nil?))

(defn combine-fns
  [selector params & fns]
  {selector (->> fns
                (map #(selector (apply % params)))
                (apply merge))})

(defn combine-joint-fns
  [angle1 angle2 & fns]
  (apply combine-fns :joints [angle1 angle2] fns))

(defn combine-stiffness-fns
  [& fns]
  (apply combine-fns :stiffness [] fns))

(defn map-to-list
  [m]
  (map (fn [k] [k (m k)]) (keys m)))

(defn map-to-java-list
  [m]
  (map (fn [k] (java.util.ArrayList. (list k (m k)))) (keys m)))
