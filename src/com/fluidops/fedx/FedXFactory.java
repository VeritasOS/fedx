/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.cache.Cache;
import com.fluidops.fedx.cache.MemoryCache;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointFactory;
import com.fluidops.fedx.endpoint.ResolvableEndpoint;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.repository.FedXRepository;
import com.fluidops.fedx.statistics.Statistics;
import com.fluidops.fedx.statistics.StatisticsImpl;
import com.fluidops.fedx.util.FileUtil;

/**
 * FedX initialization factory methods for convenience: methods initialize the
 * {@link FederationManager} and all required FedX structures. See
 * {@link FederationManager} for some a code snippet.
 * 
 * <p>
 * Use the {@link FedXFactory#newFederation()} builder to create an advanced and
 * customized federation
 * </p>
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXFactory {

	protected static final Logger log = LoggerFactory.getLogger(FedXFactory.class);
	
	
	
	/**
	 * Initialize the federation with the provided sparql endpoints. 
	 * 
	 * NOTE: {@link Config#initialize(File)} needs to be invoked before.
	 * 
	 * @param sparqlEndpoints the list of SPARQL endpoints
	 * 
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link FedXRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXRepository initializeSparqlFederation(
			List<String> sparqlEndpoints) throws Exception {

		return newFederation().withSparqlEndpoints(sparqlEndpoints).create();
	}
	

	
	/**
	 * Initialize the federation with a specified data source configuration file
	 * (*.ttl). Federation members are constructed from the data source
	 * configuration. Sample data source configuration files can be found in the
	 * documentation.
	 * 
	 * NOTE: {@link Config#initialize(File)} needs to be invoked before.
	 * 
	 * @param dataConfig         the location of the data source configuration
	 * 
	 * @return the initialized FedX federation {@link Sail} wrapped in a
	 *         {@link FedXRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXRepository initializeFederation(File dataConfig)
			throws Exception {
		return newFederation().withMembers(dataConfig).create();
	}
	
	
	/**
	 * Initialize the federation by providing the endpoints to add. The fedx
	 * configuration can provide information about the dataConfig to be used which
	 * may contain the default federation members.
	 * <p>
	 * 
	 * NOTE: {@link Config#initialize(File)} needs to be invoked before.
	 * 
	 * @param endpoints          additional endpoints to be added, may be null or
	 *                           empty
	 * 
	 * @return the initialized FedX federation {@link Sail} wrapped in a
	 *         {@link FedXRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXRepository initializeFederation(
			List<Endpoint> endpoints) throws FedXException {
		
		return newFederation().withMembers(endpoints).create();
	}

	/**
	 * Create a new customizable FedX federation. Once all configuration is
	 * supplied, the Federation can be created using {@link #create()}
	 * 
	 * @return the {@link FedXFactory} builder
	 */
	public static FedXFactory newFederation() {
		return new FedXFactory();
	}

	protected RepositoryResolver repositoryResolver;
	protected List<Endpoint> members = new ArrayList<>();
	protected File fedxConfig;
	protected File fedxBaseDir;

	private FedXFactory() {
		
	}
	
	public FedXFactory withRepositoryResolver(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
		return this;
	}

	public FedXFactory withMembers(List<Endpoint> endpoints) {
		members.addAll(endpoints);
		return this;
	}

	public FedXFactory withMembers(File dataConfig) {
		log.info("Loading federation members from dataConfig " + dataConfig + ".");
		members.addAll(EndpointFactory.loadFederationMembers(dataConfig));
		return this;
	}
	
	public FedXFactory withSparqlEndpoint(String sparqlEndpoint) {
		members.add(EndpointFactory.loadSPARQLEndpoint(sparqlEndpoint));
		return this;
	}

	public FedXFactory withSparqlEndpoints(List<String> sparqlEndpoints) {
		for (String sparqlEndpoint : sparqlEndpoints) {
			withSparqlEndpoint(sparqlEndpoint);
		}
		return this;
	}

	public FedXFactory withResolvableEndpoint(String repositoryId) {
		members.add(EndpointFactory.loadResolvableRepository(repositoryId));
		return this;
	}

	public FedXFactory withConfigFile(File configFile) {
		if (Config.isInitialized()) {
			throw new IllegalStateException("FedX config is already initialized.");
		}
		this.fedxConfig = configFile;
		return this;
	}

	/**
	 * Configure the FedX base directory (i.e. {@link Config#getBaseDir()}) at
	 * federation construction time. Note that any explicitly configured value in
	 * {@link Config#getBaseDir()} has precedence (i.e. if a value is configured,
	 * this setting is ignored).
	 * 
	 * @param fedxBaseDir the existing fedx base directory
	 * @return the {@link FedXFactory} instance
	 */
	public FedXFactory withFedXBaseDir(File fedxBaseDir) {
		if (!fedxBaseDir.isDirectory()) {
			throw new IllegalArgumentException("Base directory does not exist: " + fedxBaseDir);
		}
		this.fedxBaseDir = fedxBaseDir;
		return this;
	}

	/**
	 * Create the federation using the provided configuration
	 * 
	 * @return the configured {@link FedXRepository}
	 */
	public FedXRepository create() {

		if (!Config.isInitialized()) {
			if (fedxConfig != null) {
				Config.initialize(fedxConfig);
			} else {
				Config.initialize();
			}
		}

		Config config = Config.getConfig();
		if (fedxBaseDir != null) {
			if (config.getBaseDir() != null) {
				log.warn("Ignoring fedx base directory, already configured as " + Config.getConfig().getBaseDir()
						+ " in fedx configuration.");
			} else {
				log.debug("Initializing FedX base directory to " + fedxBaseDir.getAbsolutePath());
				config.set("baseDir", fedxBaseDir.getAbsolutePath());
			}
		}

		// initialize defaults
		Cache cache = initializeCache();
		Statistics statistics = new StatisticsImpl();
		
		initializeMembersFromConfig();

		initializeResolvableEndpoints();

		if (members.isEmpty()) {
			log.info("Initializing federation without any pre-configured members");
		}
		return FederationManager.initialize(members, cache, statistics);
	}

	protected Cache initializeCache() {
		String location = Config.getConfig().getCacheLocation();
		File cacheLocation = FileUtil.getFileLocation(location);
		Cache cache = new MemoryCache(cacheLocation);
		cache.initialize();
		return cache;
	}

	protected void initializeMembersFromConfig() {

		String dataConfig = Config.getConfig().getDataConfig();
		if (dataConfig == null) {
			return;
		}

		File dataConfigFile = FileUtil.getFileLocation(dataConfig);
		withMembers(dataConfigFile);
	}

	protected void initializeResolvableEndpoints() {
		for (Endpoint e : members) {
			if (e instanceof ResolvableEndpoint) {
				if (repositoryResolver == null) {
					throw new IllegalStateException(
							"Repository resolver is required for a resolvable endpoint, but not configured.");
				}
				((ResolvableEndpoint) e).setRepositoryResolver(repositoryResolver);
			}
		}
	}
}
