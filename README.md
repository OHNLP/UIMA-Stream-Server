UIMA Streaming Server
=====

This project provides support for running UIMA as a stream, that is to say as an always-online service capable of 
performing real-time processing of documents through a specified UIMA pipeline.
<br>
<br>
Additionally, this project also provides several UIMAServer implementations: these serve as headless UIMA pipelines 
that can be run on a standalone node if so desired and interacted with through a client<->server paradigm (i.e. REST)
<br>

**Threading and Parallel Programming**

UIMA Streams come with built in parallelism on a node-level: the number of threads to use to execute a given pipeline
can be set through the `-Duima.server.%name%.threads` jvm flag, where %name% represents the name assigned to the 
stream for which this setting pertains. 

Note that an individual UIMA pipeline is single-threaded: while a stream can handle multiple documents in parallel,
it will only ever use a single thread per document. Users wishing for additional parallelism are responsible for 
such an implementation within their own defined AnnotationEngines

UIMA Streaming Server does not come with routing/cloud computing support: while it is entirely possible to run multiple 
headless UIMA server instances across multiple nodes, routing traffic to the correct node for load balancing purposes
is an exercise left to the user.
