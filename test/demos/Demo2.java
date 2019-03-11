package demos;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;

import com.fluidops.fedx.FedXFactory;

public class Demo2 {

	
	public static void main(String[] args) throws Exception {
		
		if (System.getProperty("log4j.configuration")==null)
			System.setProperty("log4j.configuration", "file:local/log4j.properties");
		
		String dataConfig = "local/LifeScience-FedX-SPARQL.ttl";
		Repository repo = FedXFactory.initializeFederation(dataConfig);
		
		String q = "SELECT ?Drug ?IntDrug ?IntEffect WHERE { "
			+ "?Drug <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Drug> . "
			+ "?y <http://www.w3.org/2002/07/owl#sameAs> ?Drug . "
		    + "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/interactionDrug1> ?y . "
		    + "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/interactionDrug2> ?IntDrug . "
		    + "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/text> ?IntEffect . }";
		    
		TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
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
