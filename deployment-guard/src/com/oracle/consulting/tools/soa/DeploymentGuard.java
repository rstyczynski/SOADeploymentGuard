package com.oracle.consulting.tools.soa;

import com.oracle.consulting.tools.jmx.ServerData;

import javax.management.JMX;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oracle.consulting.tools.jmx.JMXhelper;

public class DeploymentGuard {

	ServerData thisNode = new ServerData();
	ServerData masterNode = new ServerData();
	JMXhelper jmx = new JMXhelper();
	
	Log log = LogFactory.getLog(this.getClass());
	  	
	public void waitForSOA(){

		Boolean isRunningOnMaster = false;
		Boolean soaStatus = false;
		Integer soaStatusMaxWaitCnt = Integer.getInteger("soaguardStatusMaxWait", 600) / 5;
		Boolean giveup = false;
		Boolean forceShutdown = false;
		String skipCheckOnTheseNodes = System.getProperty("soaguardSkipCheckOn", "AdminServer");
				
		log.warn("SOA deployment guard started.");
		
		try {
			thisNode = jmx.getServer();
			masterNode = jmx.getClusterMaster();
			log.warn("Cluster master node:" + masterNode);
			
			isRunningOnMaster = thisNode.equals(masterNode);
		
		} catch (Exception e) {
			log.error("Error getting server name or discoverig master node name. Will shut down. Reason:" + e.getMessage(), e);
			giveup = true;
			forceShutdown = true;
		}
		
		if(! giveup) {
			if (! (skipCheckOnTheseNodes.indexOf(thisNode.name) == -1)) {
				log.warn("Server start procedure released due to blacklisted host.");	
				giveup = true;
				forceShutdown = false;				
			}
		};
		
		if (! giveup) {
			
			//execute master node check from other nodes
			if (!isRunningOnMaster ) {
				
				while (!soaStatus){
					
					soaStatusMaxWaitCnt--;
					
					try {
						soaStatus = jmx.checkSOAStatus(masterNode.getEffectiveAddress(), masterNode.getEffectivePort(), masterNode.useSSL());
					} catch (Exception e) {
						if(soaStatusMaxWaitCnt > 0) {
						
							if (e.getMessage() != null)
								log.warn("SOA infrastructure on master node is not ready. Reason:" + e.getMessage() + " Will retry " + soaStatusMaxWaitCnt + " more times");
							else
								log.warn("SOA infrastructure on master node is not ready. Will retry " + soaStatusMaxWaitCnt + " more time(s).");
						
						} else {
							
							//this the last time we see the error. Report error and force shutdown
							log.error("Timeout waiting for SOA readiness on master server. Will shut down.", e);
							forceShutdown = true;
						}
						
					}
					
					if(soaStatusMaxWaitCnt == 0) {
						break;
					}
					
					//delay 5s for each step
					try {Thread.sleep(5000); } catch (InterruptedException e) { /* ignore */ }
		
				}//wait for soa
				
				if(soaStatus) {
					log.warn("SOA Platform on master node is running and accepting requests. Server start procedure released.");
				}//soa status
				
			} else {
				
				//parameter for debug and test purposes
				Integer soaDelayMaster = Integer.getInteger("soaguardDelayMaster", 0);
				if(soaDelayMaster > 0) {
					log.warn("Master node detected. Server start procedure will be released in " + soaDelayMaster + "s . Other machines will be delayed until this node finishes its initialization.");
					try {Thread.sleep(soaDelayMaster*1000); } catch (InterruptedException e) { /*ignore*/ }
				} else {
					log.warn("Master node detected. Server start procedure released. Other machines will be delayed until this node finishes its initialization.");
				}
				
			}//running on master

		}//giveup  
		
		//shutdown?
		if(forceShutdown){
			log.warn("Shut down requested. Shuting down the system.");
			try {
				jmx.forceShutdown();
			} catch (Exception e) {
				log.error("Not possible to shutdown the server. Reason: " + e.getMessage());
			}
		}//shutdown
		
	}//waitForSOA
}
