UIMA Rest Server
==
A Spring Boot application exposing UIMA-Stream-Server's functionality as a RESTful Service

**Installation**

To compile a usable executable, simply run `mvn clean install -P EXECUTABLE` on the parent directory. 
The resulting executable `./UIMA-Server-REST/target/UIMA-Server-Core-1.0-SNAPSHOT.jar` can be directly run 
via `java -jar <JVM_ARGS> UIMA-Server-Core-1.0-SNAPSHOT.jar` alongside the appropriate JVM_ARGS as desired

To create the requisite workspace, simply launch the server once without any plugins loaded. The working directory will
be populated and will resemble the following structure:

```
<working/execution directory>
|- libs
|- plugins 
```  

and quit once these directories are created

UIMA-Stream-Server plugins should be placed into the `plugins` directory, while any supporting libraries 
(e.g. unshaded dependencies) should be placed into the `libs` directory. Generally, plugin distributions should 
document what goes where for their end-users. If the implemented pipeline refers to relative paths,
they will use your working/execution directory as the working path.

To control the level of parallelism (e.g. amount of simultaneous processing requests that can be handled at once) 
used for each respective UIMA pipeline, add `-Duima.streams.%pipeline%.threads=#` to your JVM_ARGS, 
replacing `%pipeline%` with the appropriate pipeline name (this is typically defined by the plugin that is being used 
and should be provided as part of its respective documentation)<br/>
**Note**: This should never exceed the number of CPU cores available on the server, and should be 
configured based on anticipated traffic load for each individual pipeline.



  
