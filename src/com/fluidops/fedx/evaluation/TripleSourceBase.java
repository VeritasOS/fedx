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
package com.fluidops.fedx.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.evaluation.iterator.GraphToBindingSetConversionIteration;
import com.fluidops.fedx.evaluation.iterator.SingleBindingSetIteration;
import com.fluidops.fedx.monitoring.Monitoring;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.QueryType;
import com.fluidops.fedx.util.QueryStringUtil;

public abstract class TripleSourceBase implements TripleSource
{
	protected final Monitoring monitoringService;
	protected final Endpoint endpoint;

	public TripleSourceBase(Monitoring monitoring, Endpoint endpoint) {
		this.monitoringService = monitoring;
		this.endpoint = endpoint;
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, RepositoryConnection conn, QueryType queryType)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException
	{
		switch (queryType)
		{
		case SELECT:
			monitorRemoteRequest();
			TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery);
			disableInference(tQuery);
			return tQuery.evaluate();
		case CONSTRUCT:
			monitorRemoteRequest();
			GraphQuery gQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, preparedQuery);
			disableInference(gQuery);
			return new GraphToBindingSetConversionIteration(gQuery.evaluate());
		case ASK:
			monitorRemoteRequest();
			BooleanQuery bQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedQuery);
			disableInference(bQuery);
			return booleanToBindingSetIteration(bQuery.evaluate());
		default:
			throw new UnsupportedOperationException(
					"Operation not supported for query type " + queryType);
		}
	}
	

	@Override
	public boolean hasStatements(RepositoryConnection conn, Resource subj,
			IRI pred, Value obj, Resource... contexts) throws RepositoryException
	{
		return conn.hasStatement(subj, pred, obj, false, contexts);
	}
	
	
	@Override
	public boolean hasStatements(ExclusiveGroup group,
			RepositoryConnection conn, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException 	{
		
		monitorRemoteRequest();
		String preparedAskQuery = QueryStringUtil.askQueryString(group, bindings);
		return conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedAskQuery).evaluate();
	}


	protected void monitorRemoteRequest() {
		monitoringService.monitorRemoteRequest(endpoint);
	}
	
	private CloseableIteration<BindingSet, QueryEvaluationException> booleanToBindingSetIteration(boolean hasResult) {
		if (hasResult)
			return new SingleBindingSetIteration(EmptyBindingSet.getInstance());
		return new EmptyIteration<BindingSet, QueryEvaluationException>();
	}
	
	/**
	 * Set includeInference to disabled explicitly.
	 * 
	 * @param query
	 */
	protected void disableInference(Query query) {
		// set includeInferred to false explicitly
		try {
			query.setIncludeInferred(false);
		} catch (Exception e) { }
	}
	
}
