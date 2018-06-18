package local;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class TestSPARQLValues {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {


		ValueFactory vf = SimpleValueFactory.getInstance();
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection conn = repo.getConnection();
		
		IRI s1 = vf.createIRI("http://example.org/s1");
		IRI s2 = vf.createIRI("http://example.org/s2");
		IRI p = vf.createIRI("http://example.org/p");
		conn.add(vf.createStatement(s1, p, vf.createLiteral("o1")));
		conn.add(vf.createStatement(s2, p, vf.createLiteral("o2")));
		
		
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, 
				"SELECT ?v ?__index WHERE { " +
				" ?s ?p ?v" +
				"} VALUES (?s ?__index) { " +
				" ( <" + s1.stringValue() + "> 1) "  +
				" ( <" + s2.stringValue() + "> 2) "  +
				"}"				
				);
		
		TupleQueryResult qRes = query.evaluate();
		while (qRes.hasNext())
			System.out.println(qRes.next());
		
		System.out.println("Test succeeded");
		
		conn.close();
		repo.shutDown();
	}

}
