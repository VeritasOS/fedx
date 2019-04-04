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
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import com.fluidops.fedx.endpoint.ManagedRepositoryEndpoint;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointClassification;


/**
 * Provider for an Endpoint that uses a RDF4J {@link HTTPRepository} as
 * underlying repository. All SPARQL endpoints are considered Remote.
 * 
 * @author Andreas Schwarte
 */
public class SPARQLHttpRepoProvider implements EndpointProvider {

	@Override
	public Endpoint loadEndpoint(RepositoryInformation repoInfo)
			throws FedXException {

		try {
			HTTPRepository repo = new HTTPRepository(repoInfo.getLocation());
			HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties().setMaxConnTotal(20)
					.setMaxConnPerRoute(20);
			((SharedHttpClientSessionManager) repo.getHttpClientSessionManager())
					.setHttpClientBuilder(httpClientBuilder);

			try {
				repo.initialize();
				ProviderUtil.checkConnectionIfConfigured(repo);
			} finally {
				repo.shutDown();
			}
			
			String location = repoInfo.getLocation();
			EndpointClassification epc = EndpointClassification.Remote;
					
			Endpoint res = new ManagedRepositoryEndpoint(repoInfo, location, epc, repo);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());
			
			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(), e);
		}
	}

}
