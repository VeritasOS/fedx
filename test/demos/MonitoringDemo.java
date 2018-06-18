package demos;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.monitoring.MonitoringUtil;

public class MonitoringDemo {

	
	public static void main(String[] args) throws Exception {
		
		Config.initialize();
		Config.getConfig().set("enableMonitoring", "true");
		SailRepository repo = FedXFactory.initializeFederation("local/dataSourceConfig.ttl");
		
		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT ?President ?Party WHERE {\n"
			+ "?President rdf:type dbpedia-owl:President .\n"
			+ "?President dbpedia-owl:party ?Party . }";
		
		TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();
		
		int count = 0;
		while (res.hasNext()) {
			res.next();
			count++;
		}
		System.out.println("# Done, " + count + " results");
		
		MonitoringUtil.printMonitoringInformation();
		
		FederationManager.getInstance().shutDown();
		System.out.println("Done.");
		System.exit(0);
		
	}
}
