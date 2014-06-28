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

You can use the donao function to specify a combination of joint &
stiffness commands. The structure is similar to FluentNAO but with
clojure syntax.

<pre>
;; complete movement in 0.1 second
(nao/donao robot :duration 0.1
          (arms :out)
          (hands :left-open)
          (hands :right-close))
</pre>

donao returns a channel which a message is sent on when the action
completes

You can pass other options to donao
* :duration - adjust speed of motion tasks to complete in this number of milliseconds
* :callback -  call this function when all actions done
* :wait-channel - channel to wait on before starting action
* :wait-timeout - timeout in milliseconds, after the timeout has expired will run action even if nothing received on the wait channel.
* :done-channel - send true on this channel when all actions are complete
* :done-timeout - wait at most this number of milliseconds before returning


## Packaging the Aldebaran library
You need to install the Aldebaran jar file for your platform on locally.  You can use the
[lein-localrepo](https://github.com/kumarshantanu/lein-localrepo) plug
in. Run:

    lein localrepo install qimessaging-2.1.0.19-linux64.jar com.aldebaran/qimessaging 2.1.0

It should then be possible to run "lein repl" and use the Aldebaran
classes.
