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
package com.fluidops.fedx.provider;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointClassification;
import com.fluidops.fedx.structures.EndpointConfiguration;
import com.fluidops.fedx.structures.SparqlEndpointConfiguration;


/**
 * Provider for an Endpoint that uses a Sesame {@link SPARQLRepository} as underlying
 * repository. All SPARQL endpoints are considered Remote.<p>
 * 
 * This {@link SPARQLProvider} implements special hard-coded endpoint configuration
 * for the DBpedia endpoint: the support for ASK queries is always set to false.
 * 
 * @author Andreas Schwarte
 */
public class SPARQLProvider implements EndpointProvider {

	@Override
	public Endpoint loadEndpoint(RepositoryInformation repoInfo) throws FedXException {

		try {
			SPARQLRepository repo = new SPARQLRepository(repoInfo.getLocation());
			HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties().setMaxConnTotal(20)
					.setMaxConnPerRoute(20);
			((SharedHttpClientSessionManager) repo.getHttpClientSessionManager())
					.setHttpClientBuilder(httpClientBuilder);
			repo.initialize();
			
			ProviderUtil.checkConnectionIfConfigured(repo);
			
			String location = repoInfo.getLocation();
			EndpointClassification epc = EndpointClassification.Remote;
			
			Endpoint res = new Endpoint(repoInfo.getId(), repoInfo.getName(), location, repoInfo.getType(), epc);
			EndpointConfiguration ep = manipulateEndpointConfiguration(location, repoInfo.getEndpointConfiguration());
			res.setEndpointConfiguration(ep);
			res.setRepo(repo);
			
			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(), e);
		}
	}

	/**
	 * Manipulate the endpoint configuration for certain common endpoints, e.g.
	 * DBpedia => does not support ASK queries
	 * 
	 * @param location
	 * @param ep
	 * @return
	 */
	private EndpointConfiguration manipulateEndpointConfiguration(String location, EndpointConfiguration ep) {
		
		// special hard-coded handling for DBpedia: does not support ASK
		if (location.equals("http://dbpedia.org/sparql")) {
			if (ep==null) {
				ep = new SparqlEndpointConfiguration();
			}
			if (ep instanceof SparqlEndpointConfiguration) {
				((SparqlEndpointConfiguration)ep).setSupportsASKQueries(false);
			}
		}
		
		return ep;
	}
}
