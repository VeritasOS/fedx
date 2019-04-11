package local;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fluidops.fedx.FedXBaseTest;
import com.fluidops.fedx.FedXRule;
import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.ExclusiveStatement;
import com.fluidops.fedx.algebra.StatementSource;
import com.fluidops.fedx.algebra.StatementSource.StatementSourceType;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointFactory;
import com.fluidops.fedx.structures.QueryInfo;
import com.fluidops.fedx.structures.QueryType;
import com.fluidops.fedx.util.FedXUtil;

public class DBpediaASKTest extends FedXBaseTest {

	@RegisterExtension
	public FedXRule fedx = new FedXRule();
	
	@Test
	public void testQ1() throws Exception {
		fedx.enableDebug();
		
		fedx.addEndpoint( EndpointFactory.loadSPARQLEndpoint("http://dbpedia", "http://dbpedia.org/sparql"));
		fedx.addEndpoint( EndpointFactory.loadSPARQLEndpoint("http://swdf", "http://data.semanticweb.org/sparql"));
		
		TupleQueryResult res = runSelectQuery( readResourceAsString("/tests/internet/q1.rq"));
		int count=0;
		while (res.hasNext())
			System.out.println((count++) + " => " + res.next());		
	}
	
	@Test
	public void test_HasStatementsUsingSELECT() throws Exception {
		fedx.enableDebug();
		
		Endpoint ep = EndpointFactory.loadSPARQLEndpoint("http://dbpedia", "http://dbpedia.org/sparql");
		ep.initialize();
		
		StatementSource owner = new StatementSource(ep.getId(), StatementSourceType.REMOTE);
		
		// "?President rdf:type dbpedia-owl:President"
		// "?President dbpedia-owl:party ?Party . }";
		QueryInfo qInfo = new QueryInfo("blub", QueryType.SELECT);
		ExclusiveStatement st1 = new ExclusiveStatement(
				new StatementPattern(new Var("President"), new Var("p1", RDF.TYPE),
						new Var("o1", FedXUtil.iri("http://dbpedia.org/ontology/President"))),
				owner, qInfo);
		ExclusiveStatement st2 = new ExclusiveStatement(
				new StatementPattern(new Var("President"),
						new Var("p1", FedXUtil.iri("http://dbpedia.org/ontology/party")), new Var("Party")),
				owner, qInfo);
	
		List<ExclusiveStatement> ownedNodes = new ArrayList<ExclusiveStatement>();
		ownedNodes.add(st1);
		ownedNodes.add(st2);
		
		ExclusiveGroup group = new ExclusiveGroup(ownedNodes, owner, qInfo);
		
		Assertions.assertTrue(ep.getTripleSource().hasStatements(group, EmptyBindingSet.getInstance()));
		
		ep.shutDown();
	}
	
	private TupleQueryResult runSelectQuery(String queryString) throws Exception {
		Repository repo = fedx.getRepository();
		RepositoryConnection conn = repo.getConnection();
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		return query.evaluate();
	}
}
