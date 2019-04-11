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
package com.fluidops.fedx.endpoint;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.endpoint.provider.RepositoryInformation;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.TripleSourceFactory;
import com.fluidops.fedx.exception.FedXRuntimeException;


/**
 * Base implementation for an {@link Endpoint}.
 * 
 * <p>
 * Provides implementation for the common behavior as well as connection
 * management. Typically a fresh {@link RepositoryConnection} is returned when
 * invoking {@link #getConnection()}, however, it is configurable that a managed
 * (singleton) connection can be used.
 * </p>
 * 
 * @author Andreas Schwarte
 * @see EndpointManager
 */
public abstract class EndpointBase implements Endpoint {
	
	protected static final Logger log = LoggerFactory.getLogger(EndpointBase.class);
	
	protected final RepositoryInformation repoInfo; // the repository information
	protected final String endpoint; // the endpoint, e.g. for SPARQL the URL
	protected EndpointClassification endpointClassification;		// the endpoint classification
	protected boolean writable = false; // can this endpoint be used for write operation
		

	private ManagedRepositoryConnection dependentConn = null; // if configured, contains the managed connection
	protected boolean initialized = false;			// true, iff the contained repository is initialized
	protected TripleSource tripleSource;			// the triple source, initialized when repository is set
	protected EndpointConfiguration endpointConfiguration;	// additional endpoint type specific configuration


	public EndpointBase(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification) {
		super();
		this.repoInfo = repoInfo;
		this.endpoint = endpoint;
		this.endpointClassification = endpointClassification;
	}
	
	@Override
	public String getName() {
		return repoInfo.getName();
	}

	@Override
	public TripleSource getTripleSource() {
		return tripleSource;
	}
	
	@Override
	public EndpointClassification getEndpointClassification() {
		return endpointClassification;
	}

	public void setEndpointClassification(EndpointClassification endpointClassification) {
		this.endpointClassification = endpointClassification;
	}

	public boolean isLocal() {
		return endpointClassification==EndpointClassification.Local;
	}
	
	@Override
	public boolean isWritable() {
		return writable;
	}

	public RepositoryInformation getRepoInfo() {
		return repoInfo;
	}

	/**
	 * @param writable the writable to set
	 */
	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	@Override
	public EndpointConfiguration getEndpointConfiguration() {
		return endpointConfiguration;
	}

	/**
	 * @param endpointConfiguration the endpointConfiguration to set
	 */
	public void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
		this.endpointConfiguration = endpointConfiguration;
	}

	@Override
	public RepositoryConnection getConnection() {
		if (!initialized)
			throw new FedXRuntimeException("Repository for endpoint " + getId() + " not initialized");
		if (dependentConn != null) {
			return this.dependentConn;
		}
		return getFreshConnection();
	}

	protected RepositoryConnection getFreshConnection() {
		return getRepository().getConnection();
	}

	@Override
	public String getId() {
		return repoInfo.getId();
	}

	@Override
	public String getEndpoint() {
		return endpoint;
	}

	public EndpointType getType() {
		return repoInfo.getType();
	}
	

	public boolean isInitialized() {
		return initialized;
	}
	
	@Override
	public long size() throws RepositoryException {
		try (RepositoryConnection conn = getConnection()) {
			return conn.size();
		}
	}
	
	@Override
	public void initialize() throws RepositoryException {
		if (isInitialized())
			return;
		Repository repo = getRepository();
		tripleSource = TripleSourceFactory.tripleSourceFor(this, getType());
		if (useSingleConnection()) {
			dependentConn = new ManagedRepositoryConnection(repo, repo.getConnection());
		}
		initialized = true;
	}

	/**
	 * Whether to reuse the same {@link RepositoryConnection} throughout the
	 * lifetime of this Endpoint.
	 * 
	 * <p>
	 * Note that the {@link RepositoryConnection} is wrapped as
	 * {@link ManagedRepositoryConnection}
	 * </p>
	 * 
	 * @return indicator whether a single connection should be used
	 * @see Config#useSingletonConnectionPerEndpoint()
	 */
	protected boolean useSingleConnection() {
		return Config.getConfig().useSingletonConnectionPerEndpoint();
	}

	@Override
	public void shutDown() throws RepositoryException {
		if (!isInitialized())
			return;
		if (dependentConn != null) {
			dependentConn.closeManagedConnection();
			dependentConn = null;
		}
		initialized = false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndpointBase other = (EndpointBase) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		if (getType() == null) {
			if (other.getType() != null)
				return false;
		} else if (!getType().equals(other.getType()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Endpoint [id=" + getId() + ", name=" + getName() + ", type=" + getType() + "]";
	}		
	
	
	/**
	 * A wrapper for managed {@link RepositoryConnection}s which makes sure that
	 * {@link #close()} is a no-op, i.e. the actual closing of the managed
	 * connection is controlled by the {@link Endpoint}.
	 * 
	 * @author Andreas Schwarte
	 *
	 */
	static class ManagedRepositoryConnection extends RepositoryConnectionWrapper {

		public ManagedRepositoryConnection(Repository repository, RepositoryConnection delegate) {
			super(repository, delegate);
		}

		@Override
		public void close() throws RepositoryException {
			// Do nothing: this repository connection is managed by FedX
		}
		
		public void closeManagedConnection() throws RepositoryException {
			this.getDelegate().close();
		}
	}
}
