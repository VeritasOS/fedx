/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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
package com.fluidops.fedx.endpoint;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.evaluation.TripleSource;

/**
 * 
 * Structure to maintain endpoint information, e.g. type, location. The
 * {@link Repository} to use can be obtained by calling {@link #getRepository()}.
 * 
 * 
 * @author Andreas Schwarte
 * @see ManagedRepositoryEndpoint
 * @see RepositoryEndpoint
 * @see ResolvableEndpoint
 *
 */
public interface Endpoint {

	
	/**
	 * 
	 * @return the initialized {@link Repository}
	 */
	public Repository getRepository();

	/**
	 * Return a singleton connection object. this is valid for the whole lifetime of
	 * the underlying {@link Endpoint} i.e. until it the Endpoint is shutDown
	 * 
	 * @return the repository connection
	 * 
	 * @throws RepositoryException if the repository is not initialized
	 */
	public RepositoryConnection getConnection();

	/**
	 * Repair the underlying connection, i.e. call repo.getConnection().
	 * 
	 * @return the new connection
	 * 
	 * @throws RepositoryException if a repository connection is thrown while
	 *                             creating the connection
	 */
	public RepositoryConnection repairConnection() throws RepositoryException;
	
	/**
	 * 
	 * @return the {@link TripleSource}
	 */
	public TripleSource getTripleSource();
	
	/**
	 * 
	 * @return the {@link EndpointClassification}
	 */
	public EndpointClassification getEndpointClassification();

	/**
	 * 
	 * @return whether this endpoint is writable
	 */
	public boolean isWritable();

	/**
	 * 
	 * @return the identifier of the federation member
	 */
	public String getId();
	
	/**
	 * 
	 * @return the name of the federation member
	 */
	public String getName();
	
	/**
	 * Get the endpoint location, e.g. for SPARQL endpoints the url
	 * 
	 * @return the endpoint location
	 */
	public String getEndpoint();
	
	/**
	 * Returns the size of the given repository, i.e. the number of triples.
	 * 
	 * @return the size of the endpoint
	 * @throws RepositoryException
	 */
	public long size() throws RepositoryException;

	/**
	 * Initialize this {@link Endpoint}
	 * 
	 * @throws RepositoryException
	 */
	public void initialize() throws RepositoryException;

	/**
	 * Shutdown this {@link Endpoint}
	 * 
	 * @throws RepositoryException
	 */
	public void shutDown() throws RepositoryException;
	
	/**
	 * 
	 * @return whether this Endpoint is initialized
	 */
	public boolean isInitialized();
	
	/**
	 * Additional endpoint specific configuration.
	 * 
	 * @return the endpointConfiguration
	 */
	public EndpointConfiguration getEndpointConfiguration();
}
