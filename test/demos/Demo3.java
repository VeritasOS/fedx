package demos;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class Demo3 {

	
	public static void main(String[] args) throws Exception {
		
		Config.initialize();
		List<Endpoint> endpoints = new ArrayList<Endpoint>();		
		endpoints.add( EndpointFactory.loadSPARQLEndpoint("http://dbpedia", "http://dbpedia.org/sparql"));
		endpoints.add( EndpointFactory.loadSPARQLEndpoint("http://swdf", "http://data.semanticweb.org/sparql"));
		
		FedXFactory.initializeFederation(endpoints);
		
		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT ?President ?Party WHERE {\n"
			+ "?President rdf:type dbpedia-owl:President .\n"
			+ "?President dbpedia-owl:party ?Party . }";
		
		TupleQuery query = QueryManager.prepareTupleQuery(q);
		TupleQueryResult res = query.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
		
		FederationManager.getInstance().shutDown();
		System.out.println("Done.");
		System.exit(0);
		
	}
}