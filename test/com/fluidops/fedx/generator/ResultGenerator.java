package com.fluidops.fedx.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.fluidops.fedx.SPARQLBaseTest;

public class ResultGenerator
{

	
	protected SailRepository repo;
	protected RepositoryConnection conn;
	
	
	
	public void run() throws Exception {
		
		initStore();
		
//		createResult("/tests/medium/", "query01");
//		createResult("/tests/medium/", "query02");
//		createResult("/tests/medium/", "query03");
//		createResult("/tests/medium/", "query04");
//		createResult("/tests/medium/", "query05");
//		createResult("/tests/medium/", "query05");
//		createResult("/tests/medium/", "query07");
//		createResult("/tests/medium/", "query08");
		createResult("/tests/medium/", "query09");
		createResult("/tests/medium/", "query10");
		createResult("/tests/medium/", "query11");
		createResult("/tests/medium/", "query12");
		
//		printResult("/tests/medium/", "query12");

	}
	
	
	/**
	 * Create the result files for queryFile (without extensions)
	 * 
	 * Resources are located on classpath.
	 * 
	 * e.g. createResult("/tests/medium/", "query01");
	 * 
	 * @param queryFile
	 */
	protected void createResult(String baseDir, String queryFile) throws Exception {
		
		String q = readQueryString(baseDir + queryFile + ".rq");
		
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();
		
		OutputStream out = new FileOutputStream(new File(queryFile + ".srx"));
		TupleQueryResultWriter qrWriter = new SPARQLResultsXMLWriter(out);
		QueryResults.report(res, qrWriter);
		out.close();
	}
	
	protected void printResult(String baseDir, String queryFile) throws Exception {
		
		String q = readQueryString(baseDir + queryFile + ".rq");
		
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
	}
	
	
	protected void initStore() throws Exception {
				
		MemoryStore mem = new MemoryStore();
		this.repo = new SailRepository(mem);
		repo.initialize();
		
		conn = repo.getConnection();
		
		String baseUri = "http://namespace.org";
		
		conn.add(ResultGenerator.class.getResourceAsStream("/tests/medium/data1.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream("/tests/medium/data2.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream("/tests/medium/data3.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream("/tests/medium/data4.ttl"), baseUri, RDFFormat.TURTLE);
	
	}
	
	
	/**
	 * Read the query string from the specified resource
	 * 
	 * @param queryResource
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	private String readQueryString(String queryFile) throws RepositoryException, IOException {
		InputStream stream = SPARQLBaseTest.class.getResourceAsStream(queryFile);
		try {
			return IOUtil.readString(new InputStreamReader(stream, "UTF-8"));
		} finally {
			stream.close();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		new ResultGenerator().run();

	}

}
