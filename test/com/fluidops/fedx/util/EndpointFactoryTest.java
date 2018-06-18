package com.fluidops.fedx.util;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.fluidops.fedx.FedXRule;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointType;

public class EndpointFactoryTest {

	@Rule
	public FedXRule fedx = new FedXRule();
	
	@Test
	public void testValidEndpoint() throws Exception {
		Endpoint e = null;
		try {
			e = EndpointFactory.loadSPARQLEndpoint("http://dbpedia.org/sparql");
		} catch (FedXException ex) {
			if (ex.getMessage().contains("Maintenance")) {
				return;		// dbpedia may be under maintenance
			}
			throw ex;
		}
		
		Assert.assertEquals("http://dbpedia.org", e.getName());
		Assert.assertEquals("sparql_dbpedia.org_sparql", e.getId());
		Assert.assertEquals("http://dbpedia.org/sparql", e.getEndpoint());
		Assert.assertEquals(EndpointType.SparqlEndpoint, e.getType());
	}
	
	@Test
	@Ignore // needs to be fixed, connection timeout needs to be set
	public void testNotReachableEndpoint() throws Exception {
		
		try {
			EndpointFactory.loadSPARQLEndpoint("http://invalid.org/not_sparql");
			Assert.fail("Expected exception that endpoint is invalid");
		} catch (Exception expected) {
			
		}
		
	}
}
