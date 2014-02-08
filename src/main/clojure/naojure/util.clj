(ns naojure.util)

(defn combine-joint-fns
  [angle1 angle2 & fns]
  {:joints (->> fns
                (map #(:joints ( % angle1 angle2)))
                (apply merge))})
