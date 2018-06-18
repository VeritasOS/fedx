/*
 * Copyright (C) 2008-2011, fluid Operations AG
 *
 * FedX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package demos;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.util.EndpointFactory;

public class DBpediaDemo {

	
	public static void main(String[] args) throws Exception {
		Config.initialize();
		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		endpoints.add( EndpointFactory.loadSPARQLEndpoint("dbpedia", "http://dbpedia.org/sparql"));
		endpoints.add( EndpointFactory.loadSPARQLEndpoint("nytimes", "http://api.talis.com/stores/nytimes/services/sparql"));

		FedXFactory.initializeFederation(endpoints);
		
		String q = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
				"prefix foaf: <http://xmlns.com/foaf/0.1/> " +
				"select distinct ?book ?bookName ?author ?pubDate where { " +
				"?book a <http://dbpedia.org/ontology/Book> . " +
				"?book foaf:name ?bookName . " +
				"?book <http://dbpedia.org/ontology/author> ?author . " +
				"OPTIONAL { ?book <http://dbpedia.org/property/pubDate> ?pubDate .  } " +
				"FILTER (?birthDate > \"1800-12-31T00:00:00\"^^xsd:dateTime) " +
				"?author a <http://dbpedia.org/class/yago/GermanPhilosophers> . " +
				"?author <http://dbpedia.org/ontology/birthDate> ?birthDate. " +
				"} LIMIT 100";
	
		
		TupleQuery query = QueryManager.prepareTupleQuery(q);
		TupleQueryResult res = query.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
		
		FederationManager.getInstance().shutDown();
		System.out.println("Done.");
		System.exit(0);
		
	}
}