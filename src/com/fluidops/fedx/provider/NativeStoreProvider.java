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

import java.io.File;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStoreExt;

import com.fluidops.fedx.endpoint.ManagedRepositoryEndpoint;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointClassification;
import com.fluidops.fedx.util.FileUtil;


/**
 * Provider for an Endpoint that uses a RDF4J {@link NativeStore} as underlying
 * repository. For optimization purposes the NativeStore is wrapped within a
 * {@link NativeStoreExt} to allow for evaluation of prepared queries without
 * prior optimization. Note that NativeStores are always classified as 'Local'.
 * 
 * @author Andreas Schwarte
 */
public class NativeStoreProvider implements EndpointProvider {

	@Override
	public Endpoint loadEndpoint(RepositoryInformation repoInfo) throws FedXException {
		
		File store = FileUtil.getFileLocation(repoInfo.getLocation());
		
		if (!store.exists()){
			throw new FedXRuntimeException("Store does not exist at '" + repoInfo.getLocation() + ": " + store.getAbsolutePath() + "'.");
		}
		
		try {
			NativeStore ns = new NativeStoreExt(store);
			SailRepository repo = new SailRepository(ns);
			
			try {
				repo.initialize();

				ProviderUtil.checkConnectionIfConfigured(repo);
			} finally {
				repo.shutDown();
			}

			ProviderUtil.checkConnectionIfConfigured(repo);

			Endpoint res = new ManagedRepositoryEndpoint(repoInfo, repoInfo.getLocation(), EndpointClassification.Local, repo);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());
			
			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(), e);
		}
	}


}
