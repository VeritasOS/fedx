package com.fluidops.fedx;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class FedXRule implements BeforeEachCallback, AfterEachCallback {

	
	private final File configurationPreset;

	protected Repository repository;
		
	public FedXRule(File configurationPreset) {
		this.configurationPreset = configurationPreset;
	}
	
	public FedXRule() {
		this(null);
	}

	@Override
	public void beforeEach(ExtensionContext ctx) throws Exception {
		Config.initialize();
		List<Endpoint> endpoints;
		if (configurationPreset!=null)
			endpoints = EndpointFactory.loadFederationMembers(configurationPreset);
		else
			endpoints = Collections.<Endpoint>emptyList();
		repository = FedXFactory.initializeFederation(endpoints);
		FederationManager.getInstance().getCache().clear();
	}
	
	@Override
	public void afterEach(ExtensionContext ctx) {
		repository.shutDown();
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
		return repository;
	}
	
}
