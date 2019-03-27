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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.structures.QueryInfo;

/**
 * Base class providing all common functionality for FedX StatementPatterns
 * 
 * @author Andreas Schwarte
 * @see StatementSourcePattern
 * @see ExclusiveStatement
 *
 */
public abstract class FedXStatementPattern extends StatementPattern implements StatementTupleExpr, FilterTuple, BoundJoinTupleExpr
{
	private static final long serialVersionUID = 6588020780262348806L;

	protected final List<StatementSource> statementSources = new ArrayList<StatementSource>();
	protected final int id;
	protected final QueryInfo queryInfo;
	protected final List<String> freeVars = new ArrayList<String>(3);
	protected final List<String> localVars = new ArrayList<String>();
	protected FilterValueExpr filterExpr = null;
	
	public FedXStatementPattern(StatementPattern node, QueryInfo queryInfo) {
		super(node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar(), node.getContextVar());
		this.id = NodeFactory.getNextId();
		this.queryInfo=queryInfo;
		initFreeVars();
	}
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X {
		super.visitChildren(visitor);
		if (localVars.size()>0)
			LocalVarsNode.visit(visitor, localVars);
		for (StatementSource s : sort(statementSources))
			s.visit(visitor);
		
		if (filterExpr!=null)
			filterExpr.visit(visitor);
	}
	
	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}
	
	protected void initFreeVars() {
		if (getSubjectVar().getValue()==null)
			freeVars.add(getSubjectVar().getName());
		if (getPredicateVar().getValue()==null)
			freeVars.add(getPredicateVar().getName());
		if (getObjectVar().getValue()==null)
			freeVars.add(getObjectVar().getName());
	}

	@Override
	public int getFreeVarCount() {
		return freeVars.size();
	}
	
	@Override
	public List<String> getFreeVars() {
		return freeVars;
	}	
	
	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}

	@Override
	public void addLocalVar(String localVar) {
		this.localVars.add(localVar);		
	}

	@Override
	public List<String> getLocalVars() {
		return localVars;	// TODO
	}
	
	@Override
	public int getId() {
		return id;
	}	

	@Override
	public boolean hasFreeVarsFor(BindingSet bindings) {
		for (String var : freeVars)
			if (!bindings.hasBinding(var))
				return true;
		return false;		
	}
	
	@Override
	public List<StatementSource> getStatementSources() {
		return statementSources;
	}
	
	public int getSourceCount() {
		return statementSources.size();
	}
	
	
	@Override
	public FilterValueExpr getFilterExpr() {
		return filterExpr;
	}

	@Override
	public boolean hasFilter() {
		return filterExpr!=null;
	}

	@Override
	public void addFilterExpr(FilterExpr expr) {

		if (filterExpr==null)
			filterExpr = expr;
		else if (filterExpr instanceof ConjunctiveFilterExpr) {
			((ConjunctiveFilterExpr)filterExpr).addExpression(expr);
		} else if (filterExpr instanceof FilterExpr){
			filterExpr = new ConjunctiveFilterExpr((FilterExpr)filterExpr, expr);
		} else {
			throw new RuntimeException("Unexpected type: " + filterExpr.getClass().getCanonicalName());
		}
	}
	
	@Override
	public void addBoundFilter(String varName, Value value) {
		
		// visit Var nodes and set value for matching var names
		if (getSubjectVar().getName().equals(varName))
			getSubjectVar().setValue(value);
		if (getPredicateVar().getName().equals(varName))
			getPredicateVar().setValue(value);
		if (getObjectVar().getName().equals(varName))
			getObjectVar().setValue(value);
		
		freeVars.remove(varName);
		
		// XXX recheck owned source if it still can deliver results, otherwise prune it
		// optimization: keep result locally for this query
		// if no free vars AND hasResults => replace by TrueNode to avoid additional remote requests
	}
	
	private List<StatementSource> sort(List<StatementSource> stmtSources) {
		List<StatementSource> res = new ArrayList<StatementSource>(stmtSources);
		Collections.sort(res, new Comparator<StatementSource>()	{
			@Override
			public int compare(StatementSource o1, StatementSource o2) 	{
				return o1.id.compareTo(o2.id);
			}			
		});
		return res;
	}
}
