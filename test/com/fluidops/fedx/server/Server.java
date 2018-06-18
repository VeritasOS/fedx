package com.fluidops.fedx.server;

import java.util.List;

import org.eclipse.rdf4j.repository.Repository;

import com.fluidops.fedx.structures.Endpoint;

/**
 * Interface for the server:
 * 
 * {@link SPARQLEmbeddedServer} and {@link NativeStoreServer}
 * 
 * @author as
 *
 */
public interface Server {

	public List<Repository> initialize(int nRepositories) throws Exception;
	
	public void shutdown() throws Exception;
	
	public Endpoint loadEndpoint(int i) throws Exception;
}
