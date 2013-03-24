# naojure

Clojure wrapper for Aldebaran Robotics java API. Allows a NAO robot to be
controlled from clojure and REPL-based experimentation

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
native/windows/x86_64/
native/windows/x86_64/jnaoqi.dll
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
