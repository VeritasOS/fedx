package local;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SparqlDemo
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		SPARQLRepository rep = new SPARQLRepository("http://data.linkedmdb.org/sparql");
		rep.initialize();
		
		String queryString = "SELECT ?x ?title WHERE { " +
				"?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://data.linkedmdb.org/resource/movie/film> ." +
				"?x <http://www.w3.org/2000/01/rdf-schema#label> ?title }";
		TupleQuery query = rep.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		TupleQueryResult result = query.evaluate();
		
		while (result.hasNext()) {
			System.out.println(result.next());
		}
		
		System.out.println("Done");
		System.exit(0);
	}

}
