package com.oracle.consulting.tools.jmx;

public class ServerData {
	
	public String  name = "(none)";
	public Integer listenPort = -1;
	public String  listenAddress = "(none)";
	public Boolean listenPortEnabled = false;
	public Integer sslListenPort = -1;
	public String  sslListenAddress = "(none)";
	public Boolean sslListenPortEnabled = false;
	
	public ServerData() {
	}

	@Override
	public String toString() {
		return "[name=" + name + 
				", listenPort=" + listenPort + ", listenAddress=" + listenAddress +
				", listenPortEnabled = " + listenPortEnabled +
				", sslListenPort=" + sslListenPort + ", sslListenAddress=" + sslListenAddress + 
				", sslListenPortEnabled = " + sslListenPortEnabled +
				"]";
	}

	public Boolean useSSL() {
		return sslListenPortEnabled;
	}
	
	public String getEffectiveAddress() {
		if (sslListenPortEnabled) 
			return sslListenAddress;
		else 
			return listenAddress;
	}
	
	public Integer getEffectivePort() {
		if (sslListenPortEnabled) 
			return sslListenPort;
		else 
			return listenPort;
	}
	
	@Override
	public boolean equals(Object obj) {
		ServerData other = (ServerData)obj;
		
		//name is enough to distinguish node
		Boolean result = name.equals(other.name);
//				&& 
//				listenPort.equals(other.listenPort) && listenAddress.equals(other.listenAddress);
//		
		return (result);
	}

}