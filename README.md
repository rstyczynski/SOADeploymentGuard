#Rationale
SOA Suite 11g supports automated deployment of composites based on a file system directory. During start, SOA infrastructure will look into the specified directory to pickup SCA file and deploy it by moving into MDS database. It works well, however does not guarantee coordination of parallel deployments initialized by multiple soa-infra applications during parallel start of multiple WebLogic nodes being members of the same cluster. In the worst situation this race condition leads to MDS corruption. Everything works good, when operator starts one machine; waits until completion of first machine initialization, to start rest of nodes. The problem appears when the operator forget to check first machine state, before starting next ones. I some situations, rather rare, it leads to MDS corruption. The situation of MDS crash is a complex problem, because requires operations on a database level to clean it. 

#Solution
To eliminate possibility of MDS crash during SOA system initialization, it is required to enrich SOA Suite with automated deployment coordination. To do the trick, I've used MBean CompositeLifecycleConfig exposed by SOA, information about cluster master nodes stored in database table used by automatic migration, nature of authentication-free in cluster communication, and enterprise application startup listener. Final solution is a JEE enterprise application deployed on all cluster members hosting soa-infra. The application discovers non master cluster node to check SOA status on the master one. All discovery is done fully automatically using local and remote calls to MBeans and database call via data source. Code discovers if SSL is configured on master node to use it over not encrypted communication. In case of any error or timeout waiting for master node, waiting node if forced to shut down. Note that the application works with administrator privileges. It's configured using run-as parameter of application's life cycle listener.

#Installation
Deploy soa-infra-deployment-guard.ear with low deployment order to all members running soa-infra. 

By default 
- secondary nodes will wait up to 10 minutes for first node to initialize. 
- system will skip waiting on nond named AdminServer

Attention: Do not deploy to AdminServer as the application will block admin until master node initializes SOA stack. Not a good idea.

#Parameters
soaguardStatusMaxWait, default 600 - number seconds to wait for SOA initialization before node shut down.
soaguardSkipCheckOn, default AdminServer - name of servers to bypass awaiting logic

soaguardDelayMaster, default 0 - used to artificially delay master initialization. Useful only in testing 

#Messages reported
During normal operation messages are logged on WARN level, so you will see progress information on normally configured system. Note that application uses apache commons logging API, what on a Fusion Middleware system directs log message to diagnostic log file. You can access the file in domain's log directory of a given server, or via Enterprise Manager application. All messages are self descriptive; you will see them in logs generated by application "soa-infra-deployment-guard". Messages are:

SOA infrastructure on master node is not ready - is logged with 5 seconds intervals waiting for master node to init
Timeout waiting for SOA readiness on master server - indicates failure waiting for master node. Node will shut down.
SOA Platform on master node is running and accepting requests - indicates success waiting for master node. 
Master node detected - indicates operation on a master node. Will do nothing. Node will initialize normally.
Shut down requested. Shutting down the system.  - something went wrong. It may be timeout or error. Node is taken down.

#Debugging
Low level jmx access classes are logging exceptions on a debug level. In case of abnormal problems turn DEBUG logging for package "com.oracle.consulting.tools.jmx" Each exception is logged with stack trace.

#Next steps
Current version is based on a database used to store information about singleton services. This technique looks to be the perfect one, but other ways of discovering SOA master node may be used.

#Tests
Number of tests were performed to validate functionality
[x] no ssl communication
[x] ssl communication
[x] secondary - normal operation
[x] master - normal operation
[x] master delayed, secondary waiting 
[x] master delayed, secondary timed out - shutdown
[x] start on AdminServer

#Todo
(none)
