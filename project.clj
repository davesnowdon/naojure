(defproject naojure "0.2.2"
  :description "Clojure wrapper for Aldebaran Robotics java NAOQI binding. Depends on the Aldebaran jar (which includes native dependencies) being installed in local repo"
  :url "https://github.com/davesnowdon/naojure"
  :license {:name "GNU Lesser General Public License 2.1"
            :url "http://www.gnu.org/licenses/lgpl-2.1.html"
            :distribution :repo}
  :min-lein-version "2.0.0"
  :source-paths ["src" "src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["test" "src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [com.aldebaran/qimessaging "1.22.2"]])
