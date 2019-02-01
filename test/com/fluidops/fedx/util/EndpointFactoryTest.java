package com.fluidops.fedx.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fluidops.fedx.FedXRule;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.Endpoint.EndpointType;

public class EndpointFactoryTest {

	@RegisterExtension
	public FedXRule fedxRule = new FedXRule();
	
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
		
		Assertions.assertEquals("http://dbpedia.org", e.getName());
		Assertions.assertEquals("sparql_dbpedia.org_sparql", e.getId());
		Assertions.assertEquals("http://dbpedia.org/sparql", e.getEndpoint());
		Assertions.assertEquals(EndpointType.SparqlEndpoint, e.getType());
	}
	
	@Test
	@Disabled // needs to be fixed, connection timeout needs to be set
	public void testNotReachableEndpoint() throws Exception {
		
		try {
			EndpointFactory.loadSPARQLEndpoint("http://invalid.org/not_sparql");
			Assertions.fail("Expected exception that endpoint is invalid");
		} catch (Exception expected) {
			
		}
		
	}
}
