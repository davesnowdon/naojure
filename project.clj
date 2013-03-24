(defproject naojure "0.0.1-SNAPSHOT"
  :description "Clojure wrapper for Aldebaran Robotics java NAOQI binding. Depends on the Aldebaran jar file being installed in a local repo and the shared library being in the dynamic library load path"
  :url "https://github.com/davesnowdon/naojure"
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.aldebaran/jnaoqi "1.14.0"]
                 [com.aldebaran/jnaoqi-native-deps "1.14.0"]])
