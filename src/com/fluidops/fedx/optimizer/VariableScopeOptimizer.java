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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.algebra.NJoin;
import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.exception.OptimizationException;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Optimizer with the following tasks:
 * 
 * 1. Find the scope of variables within groups (and store information 
 *    within the node)
 * 
 * 
 * @author as
 */
public class VariableScopeOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer
{

	private static final Logger log = LoggerFactory.getLogger(VariableScopeOptimizer.class);
	
	protected final QueryInfo queryInfo;
	protected final Set<String> globalVariables = new HashSet<String>();

	
	public VariableScopeOptimizer(QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
	}

	// TODO variables that are required in FILTER (i.e. outside of joins)

	@Override
	public void optimize(TupleExpr tupleExpr) {
		tupleExpr.visit(this);
	}

	@Override
	public void meet(Service tupleExpr) {
		// stop traversal
	}
		
	
	@Override
	public void meet(ProjectionElem node) throws OptimizationException
	{
		globalVariables.add(node.getSourceName());
		super.meet(node);
	}
	
	public void meet(Var var) throws OptimizationException {
		globalVariables.add(var.getName());
	}

	@Override
	public void meetOther(QueryModelNode node) {
		if (node instanceof StatementTupleExpr) {
			meetTupleExpression((StatementTupleExpr)node);			
		} else if (node instanceof NJoin) {
			meetNJoin((NJoin) node);
		} else {
			super.meetOther(node);
		}
	}

	
	protected void meetTupleExpression(StatementTupleExpr node) {
		
		// we only get here if this expression is a toplevel expression,
		// i.e. it is not part of a join
		for (String var : node.getFreeVars())
			if (!isProjection(var))
				node.addLocalVar(var);
	}
	
	protected void meetNJoin(NJoin node) {

		// map variable names to their parent expressions
		Map<String, List<StatementTupleExpr>> map = new HashMap<String, List<StatementTupleExpr>>();
				
		for (TupleExpr t : node.getArgs()) {
			
			// we can only deal with our expressions. In fact,
			// t should always be a StatementTupleExpr
			if (!(t instanceof StatementTupleExpr)) {
				log.warn("Encountered unexpected expressions type: " + t.getClass() + ", please report this.");
				return;
			}
			
			StatementTupleExpr st = (StatementTupleExpr)t;
			for (String var : st.getFreeVars()) {
				if (isProjection(var))
					continue;
				List<StatementTupleExpr> l = map.get(var);
				if (l==null) {
					l = new ArrayList<StatementTupleExpr>();
					map.put(var, l);
				}
				l.add(st);
			}			
		}
		
		// register the local vars to the particular expression
		for (Map.Entry<String, List<StatementTupleExpr>> e : map.entrySet()) {
			if (e.getValue().size()>1)
				continue;
			StatementTupleExpr st = e.getValue().get(0);		
			st.addLocalVar(e.getKey());
		}		
	}	
	
	
	@Override
	public void meet(LeftJoin node) throws OptimizationException {

		// we do not optimize local variables inside left joins for now
	}

	
	private boolean isProjection(String var) {
		return globalVariables.contains(var);
	}
}
