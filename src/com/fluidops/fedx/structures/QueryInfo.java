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
package com.fluidops.fedx.structures;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.util.QueryStringUtil;



/**
 * Structure to maintain query information during evaluation, is attached to algebra nodes. 
 * Each instance is uniquely attached to the query.
 * 
 * The queryId can be used to abort tasks belonging to a particular evaluation.
 * 
 * @author Andreas Schwarte
 *
 */
public class QueryInfo {

	protected static final AtomicInteger NEXT_QUERY_ID = new AtomicInteger(1); // static id count
	
	private final int queryID;
	private final String query;
	private final QueryType queryType;
	
	protected boolean aborted = false;

	protected Set<ParallelTask<?>> scheduledSubtasks = ConcurrentHashMap.newKeySet();

	public QueryInfo(String query, QueryType queryType) {
		super();
		this.queryID = NEXT_QUERY_ID.getAndIncrement();

		this.query = query;
		this.queryType = queryType;
	}

	public QueryInfo(Resource subj, IRI pred, Value obj)
	{
		this(QueryStringUtil.toString(subj, (IRI) pred, obj), QueryType.GET_STATEMENTS);
	}

	public int getQueryID() {
		return queryID;
	}

	public String getQuery() {
		return query;
	}	
	
	public QueryType getQueryType() {
		return queryType;
	}

	/**
	 * Register a new scheduled task for this query.
	 * 
	 * @param task
	 * @throws QueryEvaluationException if the query has been aborted
	 */
	public void registerScheduledTask(ParallelTask<?> task) throws QueryEvaluationException {
		if (aborted) {
			throw new QueryEvaluationException("Query is aborted, cannot accept new tasks");
		}
		scheduledSubtasks.add(task);
	}

	/**
	 * Mark the query as aborted and abort all scheduled (future) tasks known at
	 * this point in time.
	 * 
	 */
	public void abort() {
		if (aborted) {
			return;
		}
		aborted = true;

		abortScheduledTasks();
	}

	/**
	 * Abort any scheduled future tasks
	 */
	protected void abortScheduledTasks() {

		for (ParallelTask<?> task : scheduledSubtasks) {
			task.cancel();
		}

		scheduledSubtasks.clear();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + queryID;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryInfo other = (QueryInfo) obj;
		if (queryID != other.queryID)
			return false;
		return true;
	}	
	
	
}
