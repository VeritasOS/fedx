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
package com.fluidops.fedx.endpoint.provider;

import java.io.File;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStoreExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointClassification;
import com.fluidops.fedx.endpoint.ManagedRepositoryEndpoint;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.util.FileUtil;


/**
 * Provider for an Endpoint that uses a RDF4J {@link NativeStore} as underlying
 * repository. For optimization purposes the NativeStore is wrapped within a
 * {@link NativeStoreExt} to allow for evaluation of prepared queries without
 * prior optimization. Note that NativeStores are always classified as 'Local'.
 * 
 * <p>
 * If the repository location denotes an absolute path, the native store
 * directory must already exist. If a relative path is used, the repository is
 * created on the fly (if necessary).
 * </p>
 * 
 * @author Andreas Schwarte
 */
public class NativeStoreProvider implements EndpointProvider<NativeRepositoryInformation> {

	private static final Logger log = LoggerFactory.getLogger(NativeStoreProvider.class);

	@Override
	public Endpoint loadEndpoint(NativeRepositoryInformation repoInfo) throws FedXException {
		
		File store = new File(repoInfo.getLocation());
		if (store.isAbsolute()) {
			// if the referenced location is absolute, we make sure that the store needs to
			// exists
			if (!store.isDirectory()) {
				throw new FedXRuntimeException(
						"Store does not exist at '" + repoInfo.getLocation() + ": " + store.getAbsolutePath() + "'.");
			}

			log.debug("Loading Native store from " + store.getAbsolutePath());
		} else {

			store = FileUtil.fileInBaseDir("repositories/" + repoInfo.getLocation());
			if (store.isDirectory()) {
				log.debug("Loading existing native store from " + store.getAbsolutePath());
			} else {
				log.info("Creating and loading native store from " + store.getAbsolutePath());
				FileUtil.mkdirs(store);
			}
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

			ManagedRepositoryEndpoint res = new ManagedRepositoryEndpoint(repoInfo, repoInfo.getLocation(),
					EndpointClassification.Local, repo);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());
			
			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(), e);
		}
	}


}
