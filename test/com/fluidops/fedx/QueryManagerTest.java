package com.fluidops.fedx;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Various tests for the junit framework
 * 
 * @author as
 *
 */
public class QueryManagerTest 
{

	@Test
	public void testPrefix() {
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
		Assert.assertTrue(QueryManager.prefixCheck.matcher(queryString).matches());				
	}
	
	@Test
	public void testPrefix2() {
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\nPREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\nSELECT ?person ?name ?publication WHERE {\r\n ?person rdf:type foaf:Person .\r\n ?person foaf:name ?name .\r\n}";
		Assert.assertTrue(QueryManager.prefixCheck.matcher(queryString).matches());	
	}
	
	@Test
	public void testPrefix3() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assert.assertTrue(prefixes.size()==1);	
		Assert.assertTrue(prefixes.contains("rdf"));
	}
	
	@Test
	public void testPrefix4() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/>";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assert.assertTrue(prefixes.size()==2);	
		Assert.assertTrue(prefixes.contains("rdf"));
		Assert.assertTrue(prefixes.contains("foaf"));
	}
	
	@Test
	public void testPrefix5() {
		/* find query prefixes */
		String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\nPREFIX foaf: <http://xmlns.com/foaf/0.1/>\r\nSELECT ?person ?name ?publication WHERE {\r\n ?person rdf:type foaf:Person .\r\n ?person foaf:name ?name .\r\n}";
		Set<String> prefixes = QueryManager.findQueryPrefixes(queryString);
		Assert.assertTrue(prefixes.size()==2);	
		Assert.assertTrue(prefixes.contains("rdf"));
		Assert.assertTrue(prefixes.contains("foaf"));
	}

}
