package demos;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import com.fluidops.fedx.util.FedXUtil;

public class HttpRepoTest {

	
	protected static HTTPRepository repo;
	
	public static void selectQuery() throws Exception {
		
		TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, 
				"SELECT DISTINCT ?Company WHERE { " +
				"?Company <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://sw.opencyc.org/2008/06/10/concept/en/Business> . " +
				"}");
		
		TupleQueryResult res = query.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}		
	}
	
	
	public static void askQuery() throws Exception {
		
		BooleanQuery query = repo.getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?Company <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://sw.opencyc.org/2008/06/10/concept/en/Business> . }");
		
		boolean res = query.evaluate();
		
		System.out.println("Result: " + res);
		
	}
	
	
	public static void getStatements() throws Exception {
		
		Resource subj = null;
		IRI pred = FedXUtil.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource obj = FedXUtil.iri("http://sw.opencyc.org/2008/06/10/concept/en/Business");
			
			
		RepositoryResult<Statement> res = repo.getConnection().getStatements(subj, pred, obj, true);
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}	
	}
	
	
	public static void main(String[] args) throws Exception {
		

		
		repo = new HTTPRepository("http://factforge.net/sparql");
		repo.initialize();		
		
//		selectQuery();	
		getStatements();
		askQuery();
		
		
		System.out.println("Done");
		repo.shutDown();
	}
	

}
