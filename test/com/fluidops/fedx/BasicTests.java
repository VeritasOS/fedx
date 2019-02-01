package com.fluidops.fedx;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

public class BasicTests extends SPARQLBaseTest {


	@Test
	public void test1() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query01.rq", "/tests/basic/query01.srx", false);			
	}
	
	
	@Test
	public void test2() throws Exception {		
		/* test a basic Construct query retrieving all triples */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query02.rq", "/tests/basic/query02.ttl", false);			
	}

	
	@Test
	public void testBooleanTrueSingleSource() throws Exception{		
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03.rq", "/tests/basic/query03.srx", false);					
	}
	
	@Test
	public void testBooleanTrueMultipleSource() throws Exception{		
		/* test a basic boolean query (result true) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query03a.rq", "/tests/basic/query03.srx", false);					
	}
	
	@Test
	public void testBooleanFalse() throws Exception {		
		/* test a basic boolean query (result false) */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query04.rq", "/tests/basic/query04.srx", false);	
	}
	
	@Test
	public void testSingleSourceSelect() throws Exception {
		/* test a single source select query */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource01.rq", "/tests/basic/query_singleSource01.srx", false);	
	}
	
	@Test
	public void testSingleSourceConstruct() throws Exception {
		/* test a single source construct */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_singleSource02.rq", "/tests/basic/query_singleSource02.ttl", false);	
	}

	@Test
	public void testGetStatements() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		Set<Statement> res = getStatements(null, null, null);
		compareGraphs(res, readExpectedGraphQueryResult("/tests/basic/query02.ttl"));
	}

	@Test
	public void testValuesClause() throws Exception {
		/* test query with values clause */
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query_values.rq", "/tests/basic/query_values.srx", false);
	}
}
