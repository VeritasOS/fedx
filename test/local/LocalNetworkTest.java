package local;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.FedXBaseTest;
import com.fluidops.fedx.FedXRule;

public class LocalNetworkTest extends FedXBaseTest {

	@Rule
	public FedXRule fedx = new FedXRule(new File("test/tests/localnetwork/LifeScience-FedX-RemoteRepository.ttl"));
//	public FedXRule fedx = new FedXRule(new File("tests/tests/localnetwork/LifeScience-FedX-SPARQL.ttl"));
	
	@Test
	public void testLS3() throws Exception {
		enableDebug();
		TupleQueryResult res = runSelectQuery( readResourceAsString("/test/localnetwork/query-ls6-service.rq"));
		int count=0;
		while (res.hasNext())
			System.out.println((count++) + " => " + res.next());		
	}
	
	
	@Test
	public void testToCSV() throws Exception {
		enableDebug();
		TupleQueryResult res = runSelectQuery( readResourceAsString("/tests/localnetwork/query-ls7.rq"));
		
		SPARQLResultsCSVWriter writer = new SPARQLResultsCSVWriter(new FileOutputStream(new File("output-fedx.csv")));
		QueryResults.report(res, writer);
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
