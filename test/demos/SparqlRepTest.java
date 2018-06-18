package demos;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SparqlRepTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		SPARQLRepository rep = new SPARQLRepository("http://dbpedia.org/sparql");
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT * WHERE {  ?President rdf:type dbpedia-owl:President }";
		
		TupleQuery q = rep.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query, null);
		
		TupleQueryResult res = q.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
		System.out.println("Done");
		System.exit(0);
	}

}
