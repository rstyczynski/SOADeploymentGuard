package com.oracle.consulting.tools.soa;

import weblogic.application.ApplicationLifecycleEvent;
import weblogic.application.ApplicationLifecycleListener;

public class Bootstrap extends ApplicationLifecycleListener {

	/**
	 * soa-infra-deployment-guard application is deployed with deployment order lower than soa-infra.
	 * It makes it possible to delay start of the second and rest of WebLogic servers until first one is fully initialized.
	 * 
	 * Fully initialized means that SOA stack has started up successfully, deployed all composites, and reports its readiness.
	 */
	public void preStart(ApplicationLifecycleEvent evt) {
		DeploymentGuard guard = new DeploymentGuard();
		
		guard.waitForSOA();
		
	}
}
