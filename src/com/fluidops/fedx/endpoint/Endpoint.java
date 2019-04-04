/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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
package com.fluidops.fedx.endpoint;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.structures.Endpoint.EndpointClassification;

public interface Endpoint {

	// TODO document and refactor
	
	public Repository getRepo();
	
	public TripleSource getTripleSource();
	
	public EndpointClassification getEndpointClassification();

	public boolean isWritable();

	public RepositoryConnection getConn();

	public String getId();
	
	public long size() throws RepositoryException;

	public void initialize() throws RepositoryException;

	public void shutDown() throws RepositoryException;
}
