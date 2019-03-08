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

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointClassification;

/**
 * Returns an {@link Endpoint} for an already configured {@link Repository}.
 * 
 * @author Andreas Schwarte
 *
 */
public class RepositoryProvider implements EndpointProvider {

	protected final Repository repository;

	public RepositoryProvider(Repository repository) {
		super();
		this.repository = repository;
	}

	@Override
	public Endpoint loadEndpoint(RepositoryInformation repoInfo) throws FedXException {

		try {
			if (!repository.isInitialized()) {
				repository.initialize();
			}

			ProviderUtil.checkConnectionIfConfigured(repository);

			Endpoint res = new Endpoint(repoInfo.getId(), repoInfo.getName(), repoInfo.getLocation(),
					repoInfo.getType(), EndpointClassification.Remote);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());
			res.setRepo(repository);

			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(),
					e);
		}
	}

}
