package demos;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import com.fluidops.fedx.server.SPARQLEmbeddedServer;

public class TestEndpoints {
	
	public static void main(String[] args) throws Exception {
		
		int MAX_ENDPOINTS = 4;
		
		// set up the server: the maximal number of endpoints must be known
		List<String> repositoryIds = new ArrayList<String>(MAX_ENDPOINTS);
		for (int i=1; i<=MAX_ENDPOINTS; i++)
			repositoryIds.add("endpoint"+i);
		SPARQLEmbeddedServer server = new SPARQLEmbeddedServer(repositoryIds, false);

		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
		
		ArrayList<RepositoryConnection> conns = new ArrayList<RepositoryConnection>(MAX_ENDPOINTS);
		for (int i=1; i<=MAX_ENDPOINTS; i++) {
			SPARQLRepository rep = new SPARQLRepository(server.getRepositoryUrl("endpoint"+i));
			conns.add(rep.getConnection());
		}
		
		
		for (int i=0; i<2; i++) {
			for (final RepositoryConnection conn : conns) {
				new Thread() {
	
					@Override
					public void run()
					{
						try
						{
							executeQuery(conn);
						}
						catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}.start();
			}

//			executeQuery(conn);
		}

	}

	
	protected static void executeQuery(RepositoryConnection conn) throws Exception {
		
		String q = "ASK { ?x ?y ?z }";
		BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, q);
		boolean res = query.evaluate();
		System.out.println("Endpoint " + (res ? "has data" : "has no data"));
	}
}
