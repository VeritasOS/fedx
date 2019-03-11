package demos;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;

import com.fluidops.fedx.config.FedXSailConfig;
import com.fluidops.fedx.config.FedXSailFactory;

public class Demo8 {

	public static void main(String[] args) throws Exception {
		
		FedXSailConfig sailConfig = new FedXSailConfig("examples/fedxConfig-dataCfg.prop");
		FedXSailFactory f = new FedXSailFactory();
		Sail sail = f.getSail(sailConfig);
		SailRepository repo = new SailRepository(sail);
		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
			+ "SELECT ?President ?Party WHERE {\n"
			+ "?President rdf:type dbpedia-owl:President .\n"
			+ "?President dbpedia-owl:party ?Party . }";
		TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
		try (TupleQueryResult res = query.evaluate()) {
			while (res.hasNext()) {
				System.out.println(res.next());
			}
		}
		repo.shutDown();
		System.out.println("#Done");
		System.exit(0);
	}
}
