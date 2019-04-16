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
package com.fluidops.fedx.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

import com.fluidops.fedx.algebra.NJoin;
import com.fluidops.fedx.exception.OptimizationException;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Generic optimizer
 * 
 * Tasks:
 * - Collect information (hasUnion, hasFilter, hasService)
 * - Collect all statements in a list (for source selection), do not collect SERVICE expressions
 * - Collect all Join arguments and group them in the NJoin structure for easier optimization (flatten)
 * 
 * @author Andreas Schwarte
 */
public class GenericInfoOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	protected boolean hasFilter = false;
	protected boolean hasUnion = false;
	protected boolean hasService = false;
	protected long limit = -1; // set to a positive number if the main query has a limit
	protected List<StatementPattern> stmts = new ArrayList<>();

	// internal helpers
	private boolean seenProjection = false; // whether the main projection has been visited
	
	protected final QueryInfo queryInfo;
		
	public GenericInfoOptimizer(QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
	}

	public boolean hasFilter() {
		return hasFilter;
	}
	
	public boolean hasUnion() {
		return hasUnion;
	}
	
	public List<StatementPattern> getStatements() {
		return stmts;
	}
	
	public boolean hasLimit() {
		return limit > 0;
	}

	public long getLimit() {
		return limit;
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {
		
		try { 
			tupleExpr.visit(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
		
	}
	
	
	@Override
	public void meet(Union union) {
		hasUnion=true;
		super.meet(union);
	}
	
	@Override
	public void meet(Filter filter)  {
		hasFilter=true;
		super.meet(filter);
	}
	
	@Override
	public void meet(Service service) {
		hasService=true;
	}
	
	@Override
	public void meet(Join node) {
		
		/*
		 * Optimization task:
		 * 
		 * Collect all join arguments recursively and create the
		 * NJoin structure for easier join order optimization
		 */
				
		NJoin newJoin = OptimizerUtil.flattenJoin(node, queryInfo);
		newJoin.visitChildren(this);
		
		node.replaceWith(newJoin);
	}
	
	@Override
	public void meet(StatementPattern node) {
		stmts.add(node);
	}

	@Override
	public void meet(Projection node) throws OptimizationException {
		seenProjection = true;
		super.meet(node);
	}

	@Override
	public void meet(Slice node) throws OptimizationException {
		// remember the limit of the main query (i.e. outside of a projection)
		if (!seenProjection) {
			limit = node.getLimit();
		}
		super.meet(node);
	}

	public boolean hasService()	{
		return hasService;
	}
}
