package demos;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class TestFedX {

	
	public static void main(String[] args) throws Exception {
		
		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		endpoints.add( EndpointFactory.loadSPARQLEndpoint("dbpedia", "http://dbpedia.org/sparql"));
		Config.initialize();
		Repository repo = FedXFactory.initializeFederation(endpoints);
		
		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT ?President ?Party WHERE {\n"
			+ "?President rdf:type dbpedia-owl:President .\n"
			+ "?President dbpedia-owl:party ?Party . }";
		
		TupleQuery query = QueryManager.prepareTupleQuery(q);
		try (TupleQueryResult res = query.evaluate()) {
		
			while (res.hasNext()) {
				System.out.println(res.next());
			}
		}
		
		repo.shutDown();
		System.out.println("Done.");
		System.exit(0);
		
	}
}