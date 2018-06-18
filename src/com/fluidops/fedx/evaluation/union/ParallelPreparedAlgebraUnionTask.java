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
package com.fluidops.fedx.evaluation.union;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.algebra.FilterValueExpr;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.structures.Endpoint;

/**
 * A task implementation representing a prepared union, i.e. the prepared query is executed
 * on the provided triple source.
 * 
 * @author Andreas Schwarte
 */
public class ParallelPreparedAlgebraUnionTask implements ParallelTask<BindingSet> {
	
	protected final TripleSource tripleSource;
	protected final RepositoryConnection conn;
	protected final TupleExpr preparedQuery;
	protected final BindingSet bindings;
	protected final ParallelExecutor<BindingSet> unionControl;
	protected final FilterValueExpr filterExpr;
	
	public ParallelPreparedAlgebraUnionTask(ParallelExecutor<BindingSet> unionControl, TupleExpr preparedQuery, TripleSource tripleSource, RepositoryConnection conn, BindingSet bindings, FilterValueExpr filterExpr) {
		this.preparedQuery = preparedQuery;
		this.bindings = bindings;
		this.unionControl = unionControl;
		this.tripleSource = tripleSource;
		this.conn = conn;
		this.filterExpr = filterExpr;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {
		return tripleSource.getStatements(preparedQuery, conn, bindings, filterExpr);
	}


	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return unionControl;
	}

	public String toString() {
		Endpoint e = EndpointManager.getEndpointManager().getEndpoint(conn);
		return this.getClass().getSimpleName() + " @" + e.getId() + ": " + preparedQuery.toString();
	}
}
