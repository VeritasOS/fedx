package com.fluidops.fedx.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.rules.TemporaryFolder;

import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class NativeStoreServer extends TemporaryFolder implements Server {

	private List<Repository> repositories = new ArrayList<Repository>();
	
	public List<Repository> initialize(int nRepositories) throws Exception {
		try {
			this.before();
		} catch (Throwable e) {
			throw new Exception(e);
		}
		File baseDir = newFolder();
		for (int i=1; i<=nRepositories; i++) {
			Repository repo = new SailRepository(new NativeStore(new File(baseDir, "endpoint"+i)));
			repo.initialize();
			repositories.add(repo);
			repo.shutDown();
		}
		return repositories;
	}
	
	public void shutdown() throws Exception {
		for (Repository r : repositories)
			r.shutDown();
		try {
			this.after();
		} catch (Throwable e) {
			throw new Exception(e);
		}
	}

	@Override
	public Endpoint loadEndpoint(int i) throws Exception {
		return EndpointFactory.loadNativeEndpoint(repositories.get(i-1).getDataDir().getAbsolutePath());
	}
}
