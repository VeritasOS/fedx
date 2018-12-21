package com.fluidops.fedx.misc;

import java.util.Arrays;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.SPARQLBaseTest;

public class PrefixTests extends SPARQLBaseTest {

	
	@Test
	public void test1() throws Exception {
		
		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl", "/tests/data/data4.ttl"));

		QueryManager qm = FederationManager.getInstance().getQueryManager();
		qm.addPrefixDeclaration("foaf", "http://xmlns.com/foaf/0.1/");
		qm.addPrefixDeclaration("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
					
		execute("/tests/prefix/query.rq", "/tests/prefix/query.srx", false);			
		
		qm.addPrefixDeclaration("foaf", null);
		qm.addPrefixDeclaration("rdf", null);
		
	}

	
	@Test
	public void test2() throws Exception {
		
		/* test select query retrieving all persons, missing prefix, malformed query exception */
		try {
			prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl", "/tests/data/data4.ttl"));
			execute("/tests/prefix/query.rq", "/tests/prefix/query.srx", false);
		} catch (MalformedQueryException m) {
			// this exception is expected			
			return;			
		} 
	}
	
	
	@Test
	public void test3() throws Exception {
		
		/* test select query retrieving all persons - duplicated prefix definition (in the query + qm)*/
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl", "/tests/data/data4.ttl"));

		QueryManager qm = FederationManager.getInstance().getQueryManager();
		qm.addPrefixDeclaration("foaf", "http://xmlns.com/foaf/0.1/");
					
		execute("/tests/prefix/query2.rq", "/tests/prefix/query2.srx", false);			
		
		qm.addPrefixDeclaration("foaf", null);
		qm.addPrefixDeclaration("rdf", null);
	}
}
