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
In order to get leiningen to load native dependencies you can pack them
into a jar file and install them in a local repository. I create a
directory called maven_repository at the top level of a project.

In a staging area I then create the following directory structure

<pre>
native/
native/linux/
native/linux/x86_64/
native/linux/x86_64/libjnaoqi.so
native/macosx/
native/macosx/x86_64/
native/macosx/x86_64/libjnaoqi.jnilib
native/windows/
native/windows/x86/
native/windows/x86/jnaoqi.dll
</pre>

I then run the following script to create a jar file containing the native
dependencies and depoy both jars to the project's local repository.

<pre lang="shell"><code>
#! /bin/bash
REPO=*PATH TO YOUR PROJECT HERE>*/maven_repository

jar -cMf jnaoqi-native-deps-1.14.0.jar native

mvn deploy:deploy-file \
                         -Dfile=jnaoqi-native-deps-1.14.0.jar \
                         -DgroupId=com.aldebaran \
                         -DartifactId=jnaoqi-native-deps \
                         -Dversion=1.14.0 \
                         -Dpackaging=jar \
                         -Durl=file:${REPO}

mvn deploy:deploy-file \
                         -Dfile=jnaoqi-1.14.0.218.jar \
                         -DgroupId=com.aldebaran \
                         -DartifactId=jnaoqi \
                         -Dversion=1.14.0 \
                         -Dpackaging=jar \
                         -Durl=file:${REPO}
</code></pre>

Running "lein deps" should then install the java jar and it should then be
possible to run "lein repl" and use the Aldebaran classes.
