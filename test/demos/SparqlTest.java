package demos;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

public class SparqlTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		
		
		
		HTTPRepository repo = new HTTPRepository("http://localhost:80/sparql");
		repo.initialize();
		
		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT ?President ?Party WHERE {\n"
			+ "?President rdf:type dbpedia-owl:President }";
		
		TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
		
		
		TupleQueryResult res = query.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
		
		
	}

}
