package com.oracle.consulting.tools.jmx;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Hashtable;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import oracle.jdbc.OracleConnection;
import weblogic.management.mbeanservers.runtime.RuntimeServiceMBean;

import oracle.as.jmx.framework.PortableMBeanFactory;

public class JMXhelper {

	  Log log = LogFactory.getLog(this.getClass());
 
	  /**
	   * Returns local runtime MBean server
	   * 
	   * @return local runtime MBean server
	   * @throws NamingException
	   */
	  public MBeanServer getRuntimeServiceMBean() throws Exception {
	      MBeanServer rts;
                 
          //Bug - in event listener this lookup was not working
	      //InitialContext ctx;
	      //ctx = new InitialContext();
	      //rts =  (MBeanServer) ctx.lookup("java:comp/env/jmx/runtime");
        
          rts = new PortableMBeanFactory().getMBeanServer();
          return (rts);  
	  }

// Does not work as expected. Shows app from domain w/o checking if it's targeted to current node....
//	  
//	  /**
//	   * Checks if named app is deployed in current managed server. Note that name is sensitive to character capitalization.
//	   * 
//	   * @param appName
//	   * @return true if app found
//	   * @throws Exception
//	   */
//	  public Boolean isAppDeployed(String appName) throws Exception{
//		  
//		  Boolean result = false;
//          
//	      try {	          
//	    	  where="Getting application list from runtime mbean server";
//	          final MBeanServer rts = getRuntimeServiceMBean();
//	        
//	          //get runtime MBean server
//		      ObjectName rt = new ObjectName(RuntimeServiceMBean.OBJECT_NAME);
//	          
//	          //get domain configuration. Works w/o Admin server - probably on local copy of configuration
//		      ObjectName dc = (ObjectName)rts.getAttribute(rt, "DomainConfiguration");
//		      
//		      ObjectName[] apps = (ObjectName[]) rts.getAttribute(dc, "AppDeployments");
//		      for (ObjectName app : apps) {
//		    	  
//		    	  System.out.println(rts.getAttribute(app, "ApplicationName"));
//		    	  
//		    	  if(appName.equals(rts.getAttribute(app, "ApplicationName"))){
//		    		  result = true;
//		    		  break;
//		    	  }
//		      }
//
//	      } catch (Exception ex) {
//			  log.debug("Error occured at:" + where, ex);
//			  throw(ex);
//		  }
//          
//          return (result);
//	  }
	  
	  /**
	   * Returns connection object to remote MBean server in the same cluster
	   * 
	   * @param hostname
	   * @param port
	   * @return connection object to remote MBean server in the same cluster
	   * @throws IOException
	   */
	  public MBeanServerConnection getRuntimeServiceMBean(String protocol, String hostname, Integer port) throws Exception {
		
	        String jndiroot = "/jndi/";
	        String mserver = "weblogic.management.mbeanservers.runtime";
	        JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname, port, jndiroot + mserver);

	        Hashtable<String, String> h = new Hashtable<String, String>();
	        h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
	        JMXConnector connector = JMXConnectorFactory.connect(serviceURL, h);
	        MBeanServerConnection connection = connector.getMBeanServerConnection();
	        
	        return(connection);
	  }
	  
	  /**
	   * Returns server name, IP address, and port for given server being configured in the cluster
	   * 
	   * @return server name, IP address, and port
	   * @throws Exception 
	   */
	  public ServerData getServer() throws Exception {
		  
          String serverName = "(none)";
	      
	      try {
		      final MBeanServer rts = getRuntimeServiceMBean();
	        
	          //get runtime mbean server
		      final ObjectName rt = new ObjectName(RuntimeServiceMBean.OBJECT_NAME);
		      
	          //get this server name
	          serverName = (String)rts.getAttribute(rt, "ServerName");
	          
	       }
	       catch (Exception ex) {
	           log.debug("Error getting server name from runtime MBean server", ex);
	           throw(ex);
	       }
	      
	      ServerData data = getServerAddress(serverName);
	      
		  return data;
	  }
	  
	  /**
	   * Returns IP address and port number for given server being configured in the cluster
	   * 
	   * @param serverName
	   * @return IP address and port number
	   * @throws Exception 
	   */
	  public ServerData getServerAddress (String serverName) throws Exception {

		  ServerData data = new ServerData();
		  data.name = serverName;
		  
		  try {
			  final MBeanServer rts = getRuntimeServiceMBean();
			  
			  //get domain configuration
		      final ObjectName dc = (ObjectName)rts.getAttribute(new ObjectName(RuntimeServiceMBean.OBJECT_NAME), "DomainConfiguration");
		      
	          //get servers
	          final ObjectName[] servers = (ObjectName[])rts.getAttribute(dc, "Servers");
	          
	          for (final ObjectName server : servers)
	          {
	        	  String thisName = (String)rts.getAttribute(server, "Name");
	              if(thisName.equals(serverName))
	              {
	            	  data.listenPort = (Integer)rts.getAttribute(server, "ListenPort");
	            	  data.listenAddress = (String)rts.getAttribute(server, "ListenAddress");
	            	  data.listenPortEnabled = (Boolean)rts.getAttribute(server, "ListenPortEnabled");

	            	  final ObjectName ssl = (ObjectName)rts.getAttribute(server,"SSL");
	            	  
	            	  data.sslListenPort = (Integer)rts.getAttribute(ssl, "ListenPort");
	            	  data.sslListenAddress = data.listenAddress;
	            	  data.sslListenPortEnabled = (Boolean)rts.getAttribute(ssl, "Enabled");
	            	  
	            	  break;
	              }
	          }		  
		  } catch (Exception ex) {
			  log.debug("Error getting server address from runtime mbean server.", ex);
			  throw(ex);
		  }
          
          return (data);
	  }
	  
	  /**
	   * Returns name, IP address, and port number of cluster master server owning service.SINGLETON_MASTER object.
	   * 
	   * @return name, IP address, and port number of cluster master
	   * @throws Exception 
	   */
	  public ServerData getClusterMaster() throws Exception{
		  
	      String clusterName = "(none)";
          String domainName = "(none)";
          String masterServer = "(none)";
          ServerData masterServerDetails = new ServerData();
          
    	  String where="(none)";
    	  
	      try {	          
	    	  where="Getting cluster configuration from runtime mbean server";
	          final MBeanServer rts = getRuntimeServiceMBean();
	        
	          //get runtime MBean server
		      ObjectName rt = new ObjectName(RuntimeServiceMBean.OBJECT_NAME);
	          
	          //get domain configuration. Works w/o Admin server - probably on local copy of configuration
		      ObjectName dc = (ObjectName)rts.getAttribute(rt, "DomainConfiguration");
		      
		      //get domain name
	          domainName = (String)rts.getAttribute(dc, "Name");
	        
	          //get cluster
	          where="Getting cluster definition";
	          ObjectName[] clusters = (ObjectName[])rts.getAttribute(dc, "Clusters");

	          //DataSourceForAutomaticMigration
	          String dsJNDI="(none)";
	          String table="(none)";
	          if (clusters.length == 0) {
	        	  throw new Exception("No cluster defined");
	          }
		          
	          //get first cluster
	          ObjectName cluster = clusters[0];
		          
	          where = "Getting data source for automatic migration";
	          //get cluster name
	          clusterName = (String)rts.getAttribute(cluster, "Name");
	             
	          //JDBCSystemResourceMBean
	          ObjectName datasource = (ObjectName)rts.getAttribute(cluster, "DataSourceForAutomaticMigration");
			  
	          //JDBCResource
	          ObjectName jdbc = (ObjectName)rts.getAttribute(datasource, "JDBCResource");
	          
	          //JDBCDataSourceParams
	          ObjectName jdbcResource = (ObjectName)rts.getAttribute(jdbc, "JDBCDataSourceParams");
	          
	          //JNDINames
	          String[] dsJNDIs = (String[])rts.getAttribute(jdbcResource, "JNDINames");
	          
	          if(dsJNDIs.length == 0) {
	        	  throw new Exception("No JNDI defined for migration data source");
	          }
	          
	          dsJNDI = dsJNDIs[0];
	          table = (String)rts.getAttribute(cluster, "AutoMigrationTableName");

	          //get data source
	          where = "Lookup data source";
		      InitialContext ctx;
	          ctx = new InitialContext();
	          DataSource ds=(javax.sql.DataSource) ctx.lookup (dsJNDI);
	          Connection conn=(OracleConnection) ds.getConnection();
	          
	          //get name of master server
	          where = "Getting master server name from database";
	          PreparedStatement stmt = conn.prepareStatement("select instance from " + table + " where server = 'service.SINGLETON_MASTER' and domainname = ? and clustername = ?");
	          stmt.setString(1, domainName);
	          stmt.setString(2, clusterName);
	          java.sql.ResultSet rs = stmt.executeQuery();
	          rs.next(); masterServer = rs.getString(1);
	          rs.close(); stmt.close(); conn.close();
	           
	          if (masterServer.indexOf("/") > 0) 
	        	  masterServer = masterServer.substring(masterServer.indexOf("/") + 1);

	          masterServerDetails = getServerAddress(masterServer);
	          
	       } catch (Exception ex) {
	           log.debug("Error identifing cluster master. Broken step:" + where, ex);
	           throw(ex);
	       }
	      
		  return masterServerDetails;
	  }
	  
	  /**
	   * Check SOA status on a given cluster member
	   * 
	   * @param hostname
	   * @param port
	   * @return SOA status on a given cluster member
	   * @throws Exception 
	   */
	  public boolean checkSOAStatus(String hostname, Integer port, Boolean useSSL) throws Exception {

		Boolean isReady = false;
		  
		try {
			MBeanServerConnection connection;
			if(!useSSL){
				connection = getRuntimeServiceMBean("t3", hostname, port);
				log.debug("JMX connectivity will use t3");
			} else {
				connection = getRuntimeServiceMBean("t3s", hostname, port);
				log.debug("JMX connectivity will use t3 over SSL (t3s)");
			}
			
		    ObjectName name = new ObjectName("oracle.soa.config:name=soa-infra,j2eeType=CompositeLifecycleConfig,Application=soa-infra");
	
		    CompositeDataSupport compositeData = (CompositeDataSupport)connection.getAttribute(name, "SOAPlatformStatus");
		      
		    isReady = (Boolean)compositeData.get("isReady");
		    
		} catch (Exception ex) {
			log.debug("Error getting SOA status from MBean.", ex);
			throw(ex);
		}
		
	    return isReady.booleanValue();
	  }
	  
	  public void forceShutdown() throws Exception {
		  
		  try {
			  
			  MBeanServer rt = getRuntimeServiceMBean();
			  ObjectName rts = new ObjectName(RuntimeServiceMBean.OBJECT_NAME);
			  
		      ObjectName serverRT = (ObjectName)rt.getAttribute(rts,"ServerRuntime");
		      rt.invoke(serverRT,"forceShutdown", null, null);
		      
			} catch (Exception ex) {
				log.debug("Error during force shutdown of current server.", ex);
				throw(ex);
			}
	  }
}
