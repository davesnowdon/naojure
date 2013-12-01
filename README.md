# naojure

Clojure wrapper for Aldebaran Robotics java API. Allows a NAO robot to be
controlled from clojure and REPL-based experimentation

## using the library
The first thing is to make a robot

<pre>
(def robot (make-robot "IP-ADDRESS-OR-HOSTNAME" 9559))
</pre>

This creates a map containing information about the robot. You can also
request that various proxies are created and added to the map also.

<pre>
(def robot (make-robot "IP-ADDRESS-OR-HOSTNAME" 9559 :motion :memory :tts))
</pre>


proxy-classes contains the full list of available symbols to create
proxies. Most functions create proxies on-the-fly if the robot map does not
have the necessary proxy, however since these are not stored it's more
efficient to create proxies up-front for any proxies you know you'll use
more than once.

Once you have a robot you can then do actions such as:

<pre>
(say robot "Hello world")
(wake-up robot)
(walk robot 1.0 0 0)
(get-joint-angles robot)
(sit-relaxed robot)
(relax robot)
</pre>


## Packaging the Aldebaran library
You need to install the Aldebaran jar file for your platform on locally.  You can use the
[lein-localrepo](https://github.com/kumarshantanu/lein-localrepo) plug
in. Run:

    lein localrepo install qimessaging-1.22.0.142-linux64.jar com.aldebaran/qimessaging 1.22.0

Running "lein deps" should then install the java jar and it should then be
possible to run "lein repl" and use the Aldebaran classes.
