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
package com.fluidops.fedx.structures;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.TripleSourceFactory;
import com.fluidops.fedx.exception.FedXRuntimeException;


/**
 * Structure to maintain endpoint information, e.g. type, location, etc. For initialization the
 * underlying repository must be set using {@link #setRepo(Repository)}.<p>
 * 
 * All endpoints need to be added to the {@link EndpointManager}, moreover endpoints can be 
 * looked up using their id and their connection.<p>
 * 
 * An endpoint uses a Singleton for the repository connection. If by chance this connection is broken,
 * e.g. due to a SocketException, a call to {@link #repairConnection()} reinitializes the connection.<p>
 * 
 * Note: Interaction with endpoints should be done via the EndpointManager
 * 
 * @author Andreas Schwarte
 * @see EndpointManager
 */
public class Endpoint  {
	
	private static final Logger log = LoggerFactory.getLogger(Endpoint.class);
	
	/**
	 * Classify endpoints into remote or local ones.
	 * 
	 * @author Andreas Schwarte
	 */
	public static enum EndpointClassification { Local, Remote; };
	public static enum EndpointType {
		NativeStore(Arrays.asList("NativeStore", "lsail/NativeStore")), 
		SparqlEndpoint(Arrays.asList("SparqlEndpoint", "api/sparql")), 
		RemoteRepository(Arrays.asList("RemoteRepository")), 
		Other(Arrays.asList("Other"));
		
		private List<String> formatNames;
		private EndpointType(List<String> formatNames) {
			this.formatNames = formatNames;
		}	
		
		/**
		 * Returns true if the endpoint type supports the
		 * given format (e.g. mime-type). Consider as an
		 * example the SparqlEndpoint which supports
		 * format "api/sparql".
		 * @param format
		 * @return true if the endpoint supports the given format
		 */
		public boolean supportsFormat(String format) {
			return formatNames.contains(format);
		}
		
		/**
		 * returns true if the given format is supported by
		 * some repository type.
		 * 
		 * @param format
		 * @return wheter the given format is supported
		 */
		public static boolean isSupportedFormat(String format) {
			if (format==null)
				return false;
			for (EndpointType e  : values())
				if (e.supportsFormat(format))
					return true;
			return false;
		}
	}
	
	protected String id = null;										// the identifier
	protected String name = null;									// the name
	protected String endpoint = null;								// the endpoint, e.g. for SPARQL the URL
	protected EndpointType type = null;									// the type, e.g. SPARQL, NativeRepo
	protected EndpointClassification endpointClassification;		// the endpoint classification
	protected boolean writable = false;								// can this endpoint be used for write operations
		
	protected Repository repo;
	protected RepositoryConnection conn  = null;	// a Singleton RepositoryConnection for the given endpoint
	protected boolean initialized = false;			// true, iff the contained repository is initialized
	protected TripleSource tripleSource;			// the triple source, initialized when repository is set
	protected EndpointConfiguration endpointConfiguration;	// additional endpoint type specific configuration

	/**
	 * Construct a new endpoint.
	 * 
	 * @param id
	 * 			the globally unique identifier, e.g. SPARQL_dbpedia351
	 * @param name
	 * 			the name of this endpoint
	 * @param endpoint
	 * 			the endpoint, e.g. for SPARQL the URL
	 * @param type
	 * 			the type, e.g. SPARQL, NativeStore
	 */
	public Endpoint(String id, String name, String endpoint, EndpointType type, EndpointClassification endpointClassification) {
		super();
		this.id = id;
		this.name = name;
		this.endpoint = endpoint;
		this.type = type;
		this.endpointClassification = endpointClassification;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Repository getRepo() {
		return repo;
	}

	public TripleSource getTripleSource() {
		return tripleSource;
	}
	
	public EndpointClassification getEndpointClassification() {
		return endpointClassification;
	}

	public void setEndpointClassification(EndpointClassification endpointClassification) {
		this.endpointClassification = endpointClassification;
	}

	public boolean isLocal() {
		return endpointClassification==EndpointClassification.Local;
	}
	
	/**
	 * Set the underlying initialized repository.
	 * 
	 * @param repo
	 * 			the repository
	 * 
	 * @throws RepositoryException
	 */
	public void setRepo(Repository repo) throws RepositoryException {
		this.repo = repo;
	}	
		
	/**
	 * @return the writable
	 */
	public boolean isWritable() {
		return writable;
	}

	/**
	 * @param writable the writable to set
	 */
	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	/**
	 * Additional endpoint specific configuration.
	 * 
	 * @return the endpointConfiguration
	 */
	public EndpointConfiguration getEndpointConfiguration() {
		return endpointConfiguration;
	}

	/**
	 * @param endpointConfiguration the endpointConfiguration to set
	 */
	public void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
		this.endpointConfiguration = endpointConfiguration;
	}

	/**
	 * return a singleton connection object. this is valid for the whole lifetime of the
	 * underlying repository, i.e. until it is shutDown
	 * 
	 * @return the repository connection
	 * 
	 * @throws RepositoryException
	 * 			if the repository is not initialized
	 */
	public RepositoryConnection getConn() {
		if (!initialized)
			throw new FedXRuntimeException("Repository for endpoint " + id + " not initialized");
		return conn;
	}

	/**
	 *  @return
	 *  	the identifier
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the endpoint location, e.g. for SPARQL endpoints the url
	 * 
	 * @return the endpoint location
	 */
	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public EndpointType getType() {
		return type;
	}
	
	public void setType(EndpointType type) {
		this.type = type;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Returns the size of the given repository, i.e. the number of triples.
	 * 
	 * @return the size of the endpoint
	 * @throws RepositoryException
	 */
	public long size() throws RepositoryException {
		return getConn().size();
	}
	
	/**
	 * Initialize this repository.
	 * 
	 * @throws RepositoryException
	 */
	public void initialize() throws RepositoryException {
		if (repo==null)
			throw new FedXRuntimeException("Repository for endpoint " + id + " not yet specified");
		if (isInitialized())
			return;		
		tripleSource = TripleSourceFactory.tripleSourceFor(this, type);
		conn = repo.getConnection();
		initialized = true;
	}
	
	/**
	 * Repair the underlying connection, i.e. call repo.getConnection().
	 * 
	 * @return
	 * 			the new connection
	 * 
	 * @throws RepositoryException
	 * 				if a repository connection is thrown while creating the connection
	 */
	public RepositoryConnection repairConnection() throws RepositoryException {
		if (!initialized)
			throw new FedXRuntimeException("Repository for endpoint " + id + " not initialized");

		log.debug("Repairing connection for endpoint " + id );
		
		if (conn!=null) {
			try {
				conn.close();
			} catch (RepositoryException e) { 
				log.warn("Connection of endpoint " + id + " could not be closed: " + e.getMessage());
			}
		}
		conn = repo.getConnection();
		log.info("Connection for endpoint " + id + " successfully repaired.");
		return conn;
	}

	/**
	 * Shutdown this repository.
	 * 
	 * @throws RepositoryException
	 */
	public void shutDown() throws RepositoryException {
		if (repo==null)
			throw new RepositoryException("Repository for endpoint " + id + " not yet specified");
		if (!isInitialized())
			return;
		conn.close();
		conn = null;
		repo.shutDown();
		initialized = false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Endpoint other = (Endpoint) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Endpoint [id=" + id + ", name=" + name + ", type=" + type + "]";
	}		
	
	
}
