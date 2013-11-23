(defproject naojure "0.0.1-SNAPSHOT"
  :description "Clojure wrapper for Aldebaran Robotics java NAOQI binding. Depends on the Aldebaran jar file and native dependencies being installed in a local maven repo"
  :url "https://github.com/davesnowdon/naojure"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [com.aldebaran/qimessaging "1.22.1"]])
