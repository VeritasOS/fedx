package com.fluidops.fedx;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.rules.ExternalResource;

import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class FedXRule extends ExternalResource {

	
	private final File configurationPreset;
		
	public FedXRule(File configurationPreset) {
		this.configurationPreset = configurationPreset;
	}
	
	public FedXRule() {
		this(null);
	}

	@Override
	protected void before() throws Throwable {
		Config.initialize();
		List<Endpoint> endpoints;
		if (configurationPreset!=null)
			endpoints = EndpointFactory.loadFederationMembers(configurationPreset);
		else
			endpoints = Collections.<Endpoint>emptyList();
		FedXFactory.initializeFederation(endpoints);
		FederationManager.getInstance().getCache().clear();
	}
	
	@Override
	protected void after() {
		try {
			FederationManager.getInstance().shutDown();
		} catch (FedXException e) {
			throw new RuntimeException(e);
		}
	}

	public void addEndpoint(Endpoint e) {
		FederationManager.getInstance().addEndpoint(e);
	}
	
	public void enableDebug() {
		setConfig("debugQueryPlan", "true");
	}
	
	public void setConfig(String key, String value) {
		Config.getConfig().set(key, value);
	}

	public Repository getRepository() {
		return new SailRepository(FederationManager.getInstance().getFederation());	
	}
	
}
