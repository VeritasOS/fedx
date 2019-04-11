package com.fluidops.fedx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.repository.RepositorySettings;
import com.fluidops.fedx.server.NativeStoreServer;
import com.fluidops.fedx.server.SPARQLEmbeddedServer;
import com.fluidops.fedx.server.Server;



/**
 * Base class for any federation test, this class is self-contained with regard to testing
 * if run in a distinct JVM.
 * 
 * @author as
 *
 */
public abstract class SPARQLServerBaseTest extends FedXBaseTest {

	/**
	 * The repository type used for testing
	 */
	public enum REPOSITORY_TYPE { SPARQLREPOSITORY, REMOTEREPOSITORY, NATIVE; }
	
	protected static final int MAX_ENDPOINTS = 4;
	
	public static Logger log;
	

	/**
	 * the server, e.g. SparqlEmbeddedServer or NativeStoreServer
	 */
	protected static Server server;
	
	
	

	private static REPOSITORY_TYPE repositoryType = REPOSITORY_TYPE.SPARQLREPOSITORY;


	@BeforeAll
	public static void initTest() throws Exception
	{
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");

		log = LoggerFactory.getLogger(SPARQLServerBaseTest.class);
		
		if (System.getProperty("repositoryType")!=null)
			repositoryType = REPOSITORY_TYPE.valueOf(System.getProperty("repositoryType"));
		
		switch (repositoryType) {
		case NATIVE:			initializeLocalNativeStores(); break;
		case REMOTEREPOSITORY:
		case SPARQLREPOSITORY:
		default: 				initializeServer();
		}
	}
	
	
	@AfterAll
	public static void afterTest() throws Exception {
		if (server!=null)
			server.shutdown();
	}
	
	@BeforeEach
	public void beforeEachTest() throws Exception {
		// reset operations counter and fail after
		for (int i = 1; i <= MAX_ENDPOINTS; i++) {
			RepositorySettings repoSettings = repoSettings(i);
			repoSettings.resetOperationsCounter();
			repoSettings.setFailAfter(-1);
		}
	}

	public boolean isSPARQLServer() {
		return server instanceof SPARQLEmbeddedServer;
	}

	/**
	 * Initialization of the embedded web server hosting an
	 * openrdf workbench. Used for remote and sparql repository
	 * setting
	 * 
	 * @throws Exception
	 */
	private static void initializeServer() throws Exception{
			
		// set up the server: the maximal number of endpoints must be known
		List<String> repositoryIds = new ArrayList<String>(MAX_ENDPOINTS);
		for (int i=1; i<=MAX_ENDPOINTS; i++)
			repositoryIds.add("endpoint"+i);
		server = new SPARQLEmbeddedServer(repositoryIds, repositoryType==REPOSITORY_TYPE.REMOTEREPOSITORY);

		server.initialize(MAX_ENDPOINTS);
	}

	/**
	 * Initialization of the embedded web server hosting an
	 * openrdf workbench. Used for remote and sparql repository
	 * setting
	 * 
	 * @throws Exception
	 */
	private static void initializeLocalNativeStores() throws Exception {
		
		server = new NativeStoreServer();
		server.initialize(MAX_ENDPOINTS);
	}

	
	/**
	 * Get the repository, initialized repositories are called
	 * 
	 * endpoint1
	 * endpoint2
	 * ..
	 * endpoint%MAX_ENDPOINTS%
	 * 
	 * @param i	the index of the repository, starting with 1
	 * @return
	 */
	protected static Repository getRepository(int i) {
		return server.getRepository(i);
	}
	
	
	
	protected List<Endpoint> prepareTest(List<String> sparqlEndpointData) throws Exception {
		
		// clear federation and cache
		super.prepareTest();
		FederationManager.getInstance().removeAll();
		
		// prepare the test endpoints (i.e. load data)
		if (sparqlEndpointData.size()>MAX_ENDPOINTS)
			throw new RuntimeException("MAX_ENDPOINTs to low, " + sparqlEndpointData.size() + " repositories needed. Adjust configuration");
	
		int i=1;	// endpoint id, start with 1
		for (String s : sparqlEndpointData) {
			loadDataSet(server.getRepository(i++), s);
		}
		
		// configure federation
		List<Endpoint> endpoints = new ArrayList<>();
		for (i=1; i<=sparqlEndpointData.size(); i++) {
			Endpoint e = server.loadEndpoint(i);
			endpoints.add(e);
			FederationManager.getInstance().addEndpoint(e, true);
		}
		return endpoints;
	}
	

	/**
	 * Load a dataset. Note: the repositories are cleared before loading data
	 * 
	 * @param rep
	 * @param datasetFile
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	protected void loadDataSet(Repository rep, String datasetFile)
		throws RDFParseException, RepositoryException, IOException
	{
		log.debug("loading dataset...");
		InputStream dataset = SPARQLServerBaseTest.class.getResourceAsStream(datasetFile);

		boolean needToShutdown = false;
		if (!rep.isInitialized()) {
			rep.initialize();
			needToShutdown = true;
		}
		RepositoryConnection con = rep.getConnection();
		try {
			con.clear();
			con.add(dataset, "", Rio.getParserFormatForFileName(datasetFile).get());
		}
		finally {
			dataset.close();
			con.close();
			if (needToShutdown) {
				rep.shutDown();
			}
		}
		log.debug("dataset loaded.");
	}


	protected void ignoreForNativeStore() {
		// ignore these tests for native store
		Assumptions.assumeTrue(isSPARQLServer(), "Test is ignored for native store federation");
	}
	
	protected void assumeNativeStore() {
		Assumptions.assumeTrue(server instanceof NativeStoreServer, "Test can be executed with native store federation only.");
	}

	protected void assumeSparqlEndpoint() {
		Assumptions.assumeTrue(repositoryType == REPOSITORY_TYPE.SPARQLREPOSITORY,
				"Test can be executed for SPARQL Repository only.");
	}

	/**
	 * Return the {@link RepositorySettings} for configuring the repository
	 * 
	 * @param endpoint the endpoint index, starting with 1
	 * @return
	 */
	protected RepositorySettings repoSettings(int endpoint) {
		return server.getRepository(endpoint);
	}
}
