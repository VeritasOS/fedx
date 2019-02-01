package com.fluidops.fedx.write;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.SPARQLBaseTest;
import com.fluidops.fedx.structures.Endpoint;

public class WriteTest extends SPARQLBaseTest {

	
	@BeforeEach
	public void nativeStoreOnly() {
		assumeNativeStore();
	}
	
	@BeforeEach
	public void configure() {
		// allow empty federation members
		fedxRule.setConfig("validateRepositoryConnections", "false");
	}
	
	@Test
	public void testSimpleWrite() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = EndpointManager.getEndpointManager().getAvailableEndpoints().iterator();
		Endpoint ep1 = iter.next();
		ep1.setWritable(true);
		Endpoint ep2 = iter.next();
		
		Statement st = simpleStatement();
		
		RepositoryConnection conn = fedxRule.getRepository().getConnection();		
		conn.add(st);		
		
		// test that statement is returned from federation
		List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
		Assertions.assertEquals(1, stmts.size());
		Assertions.assertEquals(st, stmts.get(0));
		conn.close();
		
		// check that the statement is actually written to endpoint 1
		RepositoryConnection ep1Conn = ep1.getConn();
		stmts = Iterations.asList(ep1Conn.getStatements(null, null, null, true));
		Assertions.assertEquals(1, stmts.size());
		Assertions.assertEquals(st, stmts.get(0));
		ep1Conn.close();
		
		// check that endpoint 2 is empty
		RepositoryConnection ep2Conn = ep2.getConn();
		stmts = Iterations.asList(ep2Conn.getStatements(null, null, null, true));
		Assertions.assertEquals(0, stmts.size());
		ep1Conn.close();
	}
	
	@Test
	public void testReadOnlyFederation() throws Exception {
		
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));
		
		Assertions.assertEquals(false, fedxRule.getRepository().isWritable());
		
		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			Statement st = simpleStatement();
			RepositoryConnection conn = fedxRule.getRepository().getConnection();
			conn.add(st);
			conn.close();
		});

	}
	
	@Test
	public void testSimpleUpdateQuery() throws Exception {
		
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = EndpointManager.getEndpointManager().getAvailableEndpoints().iterator();
		Endpoint ep1 = iter.next();
		ep1.setWritable(true);
		
		RepositoryConnection conn = fedxRule.getRepository().getConnection();
		Update update = conn.prepareUpdate(QueryLanguage.SPARQL, "PREFIX : <http://example.org/> INSERT { :subject a :Person } WHERE { }");
		update.execute();
		
		// test that statement is returned from federation
		List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
		Assertions.assertEquals(1, stmts.size());
		Assertions.assertEquals(RDF.TYPE, stmts.get(0).getPredicate());
		conn.close();		
	}
	
	@Test
	public void testSimpleRemove() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Iterator<Endpoint> iter = EndpointManager.getEndpointManager().getAvailableEndpoints().iterator();
		Endpoint ep1 = iter.next();
		ep1.setWritable(true);
		
		Statement st = simpleStatement();
		
		RepositoryConnection ep1Conn = ep1.getRepo().getConnection();		
		ep1Conn.add(st);	
		ep1Conn.close();
		
		// test that statement is returned from federation
		RepositoryConnection conn = fedxRule.getRepository().getConnection();	
		List<Statement> stmts = Iterations.asList(conn.getStatements(null, null, null, true));
		Assertions.assertEquals(1, stmts.size());
		Assertions.assertEquals(st, stmts.get(0));
		
		conn.remove(st.getSubject(), null, null);
		
		Assertions.assertEquals(0, conn.size());
		
		conn.close();
		
		
	}
	
	protected Statement simpleStatement() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI subject = vf.createIRI("http://example.org/person1");
		return vf.createStatement(subject, RDF.TYPE, FOAF.PERSON);
	}
}
