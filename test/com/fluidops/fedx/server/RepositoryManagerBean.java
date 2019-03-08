package com.fluidops.fedx.server;

import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Helper bean for managing the instance of the {@link RepositoryManager} on the
 * embedded server.
 * 
 * @author Andreas Schwarte
 *
 */
public class RepositoryManagerBean {

	private RepositoryManager repositoryManager;

	public void init() {

		SPARQLEmbeddedServer.repositoryManager = this.repositoryManager;
	}

	public void destroy() {
		SPARQLEmbeddedServer.repositoryManager = null;
	}

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

}
