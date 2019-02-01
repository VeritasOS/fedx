package com.fluidops.fedx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.TemporaryFolder;

import com.fluidops.fedx.server.NativeStoreServer;
import com.fluidops.fedx.server.SPARQLEmbeddedServer;
import com.fluidops.fedx.server.Server;
import com.fluidops.fedx.structures.Endpoint;



/**
 * Base class for any federation test, this class is self-contained with regard to testing
 * if run in a distinct JVM.
 * 
 * @author as
 *
 */
public abstract class SPARQLBaseTest extends FedXBaseTest {

	/**
	 * The repository type used for testing
	 */
	public enum REPOSITORY_TYPE { SPARQLREPOSITORY, REMOTEREPOSITORY, NATIVE; }
	
	protected static final int MAX_ENDPOINTS = 4;
	
	public static Logger log;
	

	// either of the following servers is used (depending on repositoryType)
	private static Server server;
	
	private static List<Repository> repositories;
	
	

	private static REPOSITORY_TYPE repositoryType = REPOSITORY_TYPE.SPARQLREPOSITORY;

	@RegisterExtension
	public FedXRule fedxRule = new FedXRule();


	// use Junit temporary folder (but not as Rule!)
	static TemporaryFolder tempFolder = new TemporaryFolder();
	
	@BeforeAll
	public static void initTest() throws Exception
	{
		log = Logger.getLogger(SPARQLBaseTest.class);
		
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

		repositories = server.initialize(MAX_ENDPOINTS);
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
		repositories = server.initialize(MAX_ENDPOINTS);
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
		return repositories.get(i-1);
	}
	
	
	
	protected void prepareTest(List<String> sparqlEndpointData) throws Exception {
		
		// clear federation and cache
		super.prepareTest();
		FederationManager.getInstance().removeAll();
		
		// prepare the test endpoints (i.e. load data)
		if (sparqlEndpointData.size()>MAX_ENDPOINTS)
			throw new RuntimeException("MAX_ENDPOINTs to low, " + sparqlEndpointData.size() + " repositories needed. Adjust configuration");
	
		int i=1;	// endpoint id, start with 1
		for (String s : sparqlEndpointData) {
			loadDataSet(getRepository(i++), s);
		}
		
		// configure federation
		for (i=1; i<=sparqlEndpointData.size(); i++) {
			Endpoint e = server.loadEndpoint(i);
			FederationManager.getInstance().addEndpoint(e, true);
		}
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
		InputStream dataset = SPARQLBaseTest.class.getResourceAsStream(datasetFile);
		rep.initialize();
		RepositoryConnection con = rep.getConnection();
		try {
			con.clear();
			con.add(dataset, "", Rio.getParserFormatForFileName(datasetFile).get());
		}
		finally {
			dataset.close();
			con.close();
			rep.shutDown();
			// TODO workaround for a bug in Sesame to avoid close HTTPClient
			if (rep instanceof SessionManagerDependent)
				((SessionManagerDependent) rep).setHttpClientSessionManager(null);
		}
		log.debug("dataset loaded.");
	}


	/**
	 * Execute a testcase, both queryFile and expectedResultFile must be files 
	 * 
	 * @param queryFile
	 * @param expectedResultFile
	 * @param checkOrder
	 * @throws Exception
	 */
	protected void execute(String queryFile, String expectedResultFile, boolean checkOrder) throws Exception {
		
		RepositoryConnection conn = fedxRule.getRepository().getConnection();
		super.execute(conn, queryFile, expectedResultFile, checkOrder);		
	}	
	
	protected Set<Statement> getStatements(Resource subj, IRI pred, Value obj) throws Exception {
		
		Set<Statement> res = new HashSet<Statement>();
		RepositoryResult<Statement> stmts = fedxRule.getRepository().getConnection().getStatements(subj, pred, obj, false);
		while (stmts.hasNext()) {
			res.add(stmts.next());
		}
		stmts.close();
		return res;
	}
	
	protected void ignoreForNativeStore() {
		// ignore these tests for native store
		Assumptions.assumeTrue(isSPARQLServer(), "Test is ignored for native store federation");
	}
	
	protected void assumeNativeStore() {
		Assumptions.assumeTrue(server instanceof NativeStoreServer, "Test can be executed with native store federation only.");
	}
}
