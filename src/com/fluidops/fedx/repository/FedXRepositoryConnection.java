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
package com.fluidops.fedx.repository;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailBooleanQuery;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;
import org.eclipse.rdf4j.sail.SailConnection;

import com.fluidops.fedx.structures.FedXBooleanQuery;
import com.fluidops.fedx.structures.FedXGraphQuery;
import com.fluidops.fedx.structures.FedXTupleQuery;
import com.fluidops.fedx.structures.QueryType;
import com.fluidops.fedx.util.FedXUtil;
import com.google.common.collect.Sets;

/**
 * A special {@link SailRepositoryConnection} which adds the original query
 * string as binding to the returned query. The binding name is defined by
 * {@link #BINDING_ORIGINAL_QUERY} and is added to all query instances returned
 * by the available prepare methods.
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXRepositoryConnection extends SailRepositoryConnection
{

	/**
	 * We add a binding to each parsed query mapping the original query
	 * in order to send the original query to the endpoint if there is
	 * only a single federation member is relevant for this query.
	 */
	public static final String BINDING_ORIGINAL_QUERY = "__originalQuery";
	public static final String BINDING_ORIGINAL_QUERY_TYPE = "__originalQueryType";
	public static final String BINDING_ORIGINAL_MAX_EXECUTION_TIME = "__originalQueryMaxExecutionTime";
	
	/**
	 * The number of bindings in the external binding set that are added by FedX.
	 * 
	 * @see #BINDING_ORIGINAL_QUERY
	 * @see #BINDING_ORIGINAL_QUERY_TYPE
	 * @see #BINDING_ORIGINAL_MAX_EXECUTION_TIME
	 */
	public static final Set<String> FEDX_BINDINGS = Collections.unmodifiableSet(
			Sets.newHashSet(BINDING_ORIGINAL_QUERY, BINDING_ORIGINAL_QUERY_TYPE, BINDING_ORIGINAL_MAX_EXECUTION_TIME));

	protected FedXRepositoryConnection(SailRepository repository,
			SailConnection sailConnection) {
		super(repository, sailConnection);
	}

	@Override
	public SailQuery prepareQuery(QueryLanguage ql, String queryString,
			String baseURI) throws MalformedQueryException
	{
		SailQuery q = super.prepareQuery(ql, queryString, baseURI);
		if (q instanceof SailTupleQuery) {
			insertOriginalQueryString(q, queryString, QueryType.SELECT);
			q = new FedXTupleQuery((SailTupleQuery) q);
		}
		else if (q instanceof GraphQuery) {
			insertOriginalQueryString(q, queryString, QueryType.CONSTRUCT);
			q = new FedXGraphQuery((SailGraphQuery) q);
		} else if (q instanceof SailBooleanQuery) {
			insertOriginalQueryString(q, queryString, QueryType.ASK);
			q = new FedXBooleanQuery((SailBooleanQuery) q);
		}
		return q;
	}

	@Override
	public FedXTupleQuery prepareTupleQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException
	{
		SailTupleQuery q = super.prepareTupleQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, QueryType.SELECT);
		return new FedXTupleQuery(q);
	}

	@Override
	public FedXGraphQuery prepareGraphQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException
	{
		SailGraphQuery q = super.prepareGraphQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, QueryType.CONSTRUCT);
		return new FedXGraphQuery(q);
	}

	@Override
	public SailBooleanQuery prepareBooleanQuery(QueryLanguage ql,
			String queryString, String baseURI) throws MalformedQueryException
	{
		SailBooleanQuery q= super.prepareBooleanQuery(ql, queryString, baseURI);
		insertOriginalQueryString(q, queryString, QueryType.ASK);
		return new FedXBooleanQuery(q);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException
	{
		return super.prepareUpdate(ql, update, baseURI);
	}

	private void insertOriginalQueryString(SailQuery query, String queryString, QueryType qt) {
		query.setBinding(BINDING_ORIGINAL_QUERY, FedXUtil.literal(queryString));
		query.setBinding(BINDING_ORIGINAL_QUERY_TYPE, FedXUtil.literal(qt.name()));
	}
}
