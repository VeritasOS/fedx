package local;

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SPARQLAsk {

	
	static void testSparqlRepo()  throws Exception {
		SPARQLRepository repo = new SPARQLRepository("http://10.212.10.29:8088/openrdf-sesame/repositories/dbpedia");
		repo.initialize();
		
		RepositoryConnection conn = repo.getConnection();
		
		BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?x <http://www.w3.org/2002/07/owl#sameAs> ?o }");
		q.setIncludeInferred(false);
		
		System.out.println(q.evaluate());
		
		repo.shutDown();
	}
	
	private static void testHttpRepo()  throws Exception {
		
		Repository repo = new HTTPRepository("http://10.212.10.29:8088/openrdf-sesame", "dbpedia");
		repo.initialize();
		
		RepositoryConnection conn = repo.getConnection();
		
		BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?x <http://www.w3.org/2002/07/owl#sameAs> ?o }");
		q.setIncludeInferred(true);
		
		System.out.println("With infer=true: " + q.evaluate());
		
		q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?x <http://www.w3.org/2002/07/owl#sameAs> ?o }");
		q.setIncludeInferred(false);
		
		System.out.println("With infer=false: " + q.evaluate());
		
		repo.shutDown();
	}
	
	
	private static void testHttpRepoWithSpecialGraph()  throws Exception {
		
		Repository repo = new HTTPRepository("http://10.212.10.29:8088/openrdf-sesame", "dbpedia");
		repo.initialize();
		
		RepositoryConnection conn = repo.getConnection();
		
		BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK FROM <http://www.ontotext.com/explicit> { ?x <http://www.w3.org/2002/07/owl#sameAs> ?o }");
		q.setIncludeInferred(true);
		
		System.out.println("With infer=true: " + q.evaluate());
		
		q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK  FROM <http://www.ontotext.com/explicit> { ?x <http://www.w3.org/2002/07/owl#sameAs> ?o }");
		q.setIncludeInferred(false);
		
		System.out.println("With infer=false: " + q.evaluate());
		
		repo.shutDown();
	}

	static void testDBpediaSPARQL()  throws Exception {
		SPARQLRepository repo = new SPARQLRepository("http://dbpedia.org/sparql");
		repo.initialize();
		
		RepositoryConnection conn = repo.getConnection();
		
		BooleanQuery q = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?conference ?p ?o }");
		q.setIncludeInferred(false);
		
		System.out.println(q.evaluate());
		
		repo.shutDown();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		testHttpRepo();
		testHttpRepoWithSpecialGraph();
		testDBpediaSPARQL();
		
	}

}
