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
package com.fluidops.fedx.algebra;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.iterator.InsertBindingsIteration;
import com.fluidops.fedx.evaluation.iterator.SingleBindingSetIteration;
import com.fluidops.fedx.exception.IllegalQueryException;
import com.fluidops.fedx.structures.QueryInfo;
import com.fluidops.fedx.util.QueryStringUtil;



/**
 * Represents a StatementPattern that can only produce results at a single endpoint, the owner.
 * 
 * @author Andreas Schwarte
 */
public class ExclusiveStatement extends FedXStatementPattern {
	
	private static final long serialVersionUID = -6963394279179263763L;

	public ExclusiveStatement(StatementPattern node, StatementSource owner, QueryInfo queryInfo) {
		super(node, queryInfo);
		statementSources.add(owner);
	}	

	public StatementSource getOwner() {
		return getStatementSources().get(0);
	}	

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			BindingSet bindings) throws QueryEvaluationException {
		
		try {
			
			Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(getOwner().getEndpointID());
			TripleSource t = ownedEndpoint.getTripleSource();
			
			/*
			 * Implementation note: for some endpoint types it is much more efficient to use prepared queries
			 * as there might be some overhead (obsolete optimization) in the native implementation. This
			 * is for instance the case for SPARQL connections. In contrast for NativeRepositories it is
			 * much more efficient to use getStatements(subj, pred, obj) instead of evaluating a prepared query.
			 */			
		
			CloseableIteration<BindingSet, QueryEvaluationException> res = null;
			if (t.usePreparedQuery()) {
				
				AtomicBoolean isEvaluated = new AtomicBoolean(false); // is filter evaluated
				String preparedQuery;
				try {
					preparedQuery = QueryStringUtil.selectQueryString(this, bindings, filterExpr, isEvaluated);
				} catch (IllegalQueryException e1) {
					// TODO there might be an issue with filters being evaluated => investigate
					/* all vars are bound, this must be handled as a check query, can occur in joins */
					if (t.hasStatements(this, bindings)) {
						res = new SingleBindingSetIteration(bindings);
						if (boundFilters != null) {
							// make sure to insert any values from FILTER expressions that are directly
							// bound in this expression
							res = new InsertBindingsIteration(res, boundFilters);
						}
						return res;
					}
					return new EmptyIteration<BindingSet, QueryEvaluationException>();
				}
								
				res = t.getStatements(preparedQuery, bindings, (isEvaluated.get() ? null : filterExpr));
				
			} else {
				res = t.getStatements(this, bindings, filterExpr);
			}

			if (boundFilters != null) {
				// make sure to insert any values from FILTER expressions that are directly
				// bound in this expression
				res = new InsertBindingsIteration(res, boundFilters);
			}

			return res;
				
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
	}
}
