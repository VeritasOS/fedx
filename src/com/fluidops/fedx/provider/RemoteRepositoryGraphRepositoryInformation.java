/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.provider;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import com.fluidops.fedx.structures.Endpoint.EndpointType;
import com.fluidops.fedx.util.FedXUtil;


/**
 * Graph information for Sesame SPARQLRepository initialization.
 * 
 * Format:
 * 
 * <code>
 * <%name%> fluid:store "RemoteRepository";
 * fluid:repositoryServer "%location%";
 * fluid:repositoryName "%name%"
 * 
 * <http://dbpedia> fluid:store "RemoteRepository";
 * fluid:repositoryServer "http://<host>/openrdf-sesame" ;
 * fluid:repositoryName "dbpedia" .
 * 
 * 
 * </code>
 * 
 * Note: the id is constructed from the name: http://dbpedia.org/ => remote_dbpedia.org
 * 
 * 
 * @author Andreas Schwarte
 *
 */
public class RemoteRepositoryGraphRepositoryInformation extends RepositoryInformation {

	public RemoteRepositoryGraphRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.RemoteRepository);
		initialize(graph, repNode);
	}

	public RemoteRepositoryGraphRepositoryInformation(String repositoryServer, String repositoryName) {
		super("remote_" + repositoryName, "http://"+repositoryName, repositoryServer + "/" + repositoryName, EndpointType.RemoteRepository);
		setProperty("repositoryServer", repositoryServer);
		setProperty("repositoryName", repositoryName);		
	}
	
	protected void initialize(Model graph, Resource repNode) {
		
		// name: the node's value
		setProperty("name", repNode.stringValue());

		// repositoryServer / location
		Model repositoryServer = graph.filter(repNode, FedXUtil.iri("http://fluidops.org/config#repositoryServer"),
				null);
		String repoLocation = repositoryServer.iterator().next().getObject().stringValue();
		setProperty("location", repoLocation);
		setProperty("repositoryServer", repoLocation);
		
		// repositoryName
		Model repositoryName = graph.filter(repNode, FedXUtil.iri("http://fluidops.org/config#repositoryName"), null);
		String repoName = repositoryName.iterator().next().getObject().stringValue();
		setProperty("repositoryName", repoName);
		
		// id: the name of the location
		String id = repNode.stringValue().replace("http://", "");
		id = "remote_" + id.replace("/", "_");
		setProperty("id", id);
	}
}
