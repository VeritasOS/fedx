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

public class ServicePerformanceTest extends FedXBaseTest {

	@Rule
	public FedXRule fedx = new FedXRule(new File("tests/tests/localnetwork/LifeScience-FedX-RemoteRepository.ttl"));
	
	@Test
	public void testLocalServiceQuery_NonVectoredEvaluation() throws Exception {
		fedx.enableDebug();
		fedx.setConfig("optimizer.enableServiceAsBoundJoin", "false");
		
		TupleQueryResult res = runSelectQuery( readResourceAsString("/tests/localnetwork/query-localService.rq"));
		int count=0;
		while (res.hasNext())
			System.out.println((count++) + " => " + res.next());		
	}
	
	@Test
	public void testLocalServiceQuery_VectoredEvaluation() throws Exception {
		fedx.enableDebug();
		fedx.setConfig("optimizer.enableServiceAsBoundJoin", "true");
		
		TupleQueryResult res = runSelectQuery( readResourceAsString("/tests/localnetwork/query-localService.rq"));
		int count=0;
		while (res.hasNext())
			System.out.println((count++) + " => " + res.next());		
	}
	
	
	private TupleQueryResult runSelectQuery(String queryString) throws Exception {
		Repository repo = fedx.getRepository();
		RepositoryConnection conn = repo.getConnection();
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		return query.evaluate();
	}
}
