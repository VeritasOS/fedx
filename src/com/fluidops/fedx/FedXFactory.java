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

import org.eclipse.rdf4j.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.cache.Cache;
import com.fluidops.fedx.cache.MemoryCache;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.sail.FedXSailRepository;
import com.fluidops.fedx.statistics.Statistics;
import com.fluidops.fedx.statistics.StatisticsImpl;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

/**
 * FedX initialization factory methods for convenience: methods initialize the 
 * {@link FederationManager} and all required FedX structures. See {@link FederationManager}
 * for some a code snippet.
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXFactory {

	protected static final Logger log = LoggerFactory.getLogger(FedXFactory.class);
	
	
	
	/**
	 * Initialize the federation with the provided sparql endpoints. 
	 * 
	 * NOTE: {@link Config#initialize(String...)} needs to be invoked before.
	 * 
	 * @param sparqlEndpoints the list of SPARQL endpoints
	 * 
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link FedXSailRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXSailRepository initializeSparqlFederation(List<String> sparqlEndpoints) throws Exception {

		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		for (String url : sparqlEndpoints) {
			endpoints.add( EndpointFactory.loadSPARQLEndpoint(url));
		}
		return initializeFederation(endpoints);
	}
	

	
	/**
	 * Initialize the federation with a specified data source configuration file (*.ttl). Federation members are 
	 * constructed from the data source configuration. Sample data source configuration files can be found in the documentation.
	 * 
	 * NOTE: {@link Config#initialize(String...)} needs to be invoked before.
	 * 
	 * @param dataConfig
	 * 				the location of the data source configuration
	 * 
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link FedXSailRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXSailRepository initializeFederation(File dataConfig) throws Exception {
		String cacheLocation = Config.getConfig().getCacheLocation();
		log.info("Loading federation members from dataConfig " + dataConfig + ".");
		List<Endpoint> members = EndpointFactory.loadFederationMembers(dataConfig);
		return initializeFederation(members, cacheLocation);
	}
	
	
	
	/**
	 * Initialize the federation by providing information about the fedx configuration (c.f. {@link Config}
	 * for details on configuration parameters) and additional endpoints to add. The fedx configuration
	 * can provide information about the dataConfig to be used which may contain the default federation 
	 * members.
	 * 
	 * The Federation employs a {@link MemoryCache} which is located at {@link Config#getCacheLocation()}.
	 *  
	 * @param fedxConfig
	 * 			the location of the fedx configuration
	 * @param additionalEndpoints
	 * 			additional endpoints to be added, may be null or empty
	 *  
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link FedXSailRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXSailRepository initializeFederation(String fedxConfig, List<Endpoint> additionalEndpoints) throws FedXException {
		File file = new File(fedxConfig);
		if (!(file.isFile()))
			throw new FedXException("FedX Configuration cannot be accessed at " + fedxConfig);
		Config.initialize(file);
		return initializeFederation(additionalEndpoints);
	}
	
	
	/**
	 * Initialize the federation by providing the endpoints to add. The fedx configuration can provide information
	 * about the dataConfig to be used which may contain the default federation  members.<p>
	 * 
	 * NOTE: {@link Config#initialize(String...)} needs to be invoked before.
	 * 
	 * @param endpoints
	 * 			additional endpoints to be added, may be null or empty
	 *  
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link FedXSailRepository}
	 * 
	 * @throws Exception
	 */
	public static FedXSailRepository initializeFederation(List<Endpoint> endpoints) throws FedXException {
		
		String dataConfig = Config.getConfig().getDataConfig();
		String cacheLocation = Config.getConfig().getCacheLocation();
		List<Endpoint> members;		
		if (dataConfig==null) {
			if (endpoints!=null && endpoints.size()==0)
				log.warn("No dataConfig specified. Initializing federation without any preconfigured members.");
			members = new ArrayList<Endpoint>(5);
		} else {
			log.info("Loading federation members from dataConfig " + dataConfig + ".");
			members = EndpointFactory.loadFederationMembers(new File(dataConfig));
		}
		
		if (endpoints!=null)
			members.addAll(endpoints);
		
		return initializeFederation(members, cacheLocation);
	}
	
	
	/**
	 * Helper method to initialize the federation with a {@link MemoryCache}.
	 * 
	 * @param members
	 * @param cacheLocation
	 * @return
	 */
	private static FedXSailRepository initializeFederation(List<Endpoint> members, String cacheLocation) throws FedXException {

		Cache cache = new MemoryCache(cacheLocation);
		cache.initialize();
		Statistics statistics = new StatisticsImpl();
		
		return FederationManager.initialize(members, cache, statistics);
	}
}
