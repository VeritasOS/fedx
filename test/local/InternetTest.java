package local;

import java.io.File;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.FedXBaseTest;
import com.fluidops.fedx.FedXRule;

public class InternetTest extends FedXBaseTest {

	@Rule
	public FedXRule fedx = new FedXRule(new File("test/tests/internet/DBpediaSemanticWebDogFood.ttl"));
	
	@Test
	public void testQ1() throws Exception {
		enableDebug();
		TupleQueryResult res = runSelectQuery( readResourceAsString("/tests/internet/q1.rq"));
		int count=0;
		while (res.hasNext())
			System.out.println((count++) + " => " + res.next());		
	}
		

	private void enableDebug() {
		fedx.setConfig("debugQueryPlan", "true");
	}
	
	private TupleQueryResult runSelectQuery(String queryString) throws Exception {
		Repository repo = fedx.getRepository();
		RepositoryConnection conn = repo.getConnection();
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		return query.evaluate();
	}
}
