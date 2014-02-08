(ns naojure.util)

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
