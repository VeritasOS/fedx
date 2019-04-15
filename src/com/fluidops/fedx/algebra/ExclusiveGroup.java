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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.structures.QueryInfo;



/**
 * Represents a group of statements that can only produce results at a single endpoint, the owner.
 * 
 * @author Andreas Schwarte
 *
 */
public class ExclusiveGroup extends AbstractQueryModelNode implements StatementTupleExpr, FilterTuple
{
	private static final long serialVersionUID = 9215353191021766797L;

	protected final List<ExclusiveStatement> owned = new ArrayList<ExclusiveStatement>();
	protected final ArrayList<StatementSource> owner;
	protected final Set<String> freeVars = new HashSet<String>();
	protected final String id;
	protected final transient QueryInfo queryInfo;
	protected FilterValueExpr filter = null;
	protected transient Endpoint ownedEndpoint = null;
	
		
	public ExclusiveGroup(Collection<ExclusiveStatement> ownedNodes, StatementSource owner, QueryInfo queryInfo) {
		owned.addAll(ownedNodes);
		this.owner = new ArrayList<StatementSource>(1);
		this.owner.add(owner);
		init();	// init free vars + filter expr
		this.id = NodeFactory.getNextId();
		this.queryInfo = queryInfo;
		ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(owner.getEndpointID());
	}
	
	/**
	 * Initialize free variables and filter expressions for owned children.
	 */
	protected void init() {
		HashSet<FilterExpr> conjExpr = new HashSet<FilterExpr>();
		for (ExclusiveStatement o  : owned) {
			freeVars.addAll(o.getFreeVars());
			
			if (o.hasFilter()) {
				
				FilterValueExpr expr = o.getFilterExpr();
				if (expr instanceof ConjunctiveFilterExpr) 
					conjExpr.addAll( ((ConjunctiveFilterExpr)expr).getExpressions());
				else if (expr instanceof FilterExpr)
					conjExpr.add((FilterExpr)expr);
				else 
					throw new RuntimeException("Internal Error: Unexpected filter type: " + expr.getClass().getSimpleName());
			}
		}
		
		if (conjExpr.size()==1)
			filter = conjExpr.iterator().next();
		else if (conjExpr.size()>1){
			filter = new ConjunctiveFilterExpr(conjExpr);
		}
	}

	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X {
		
		for (ExclusiveStatement s : owned) {
			s.visit(visitor);
		}
	}
	
	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}
	
	@Override
	public Set<String> getAssuredBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}
		
	@Override
	public ExclusiveGroup clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}
	
	public StatementSource getOwner() {
		return owner.get(0);
	}
	
	public Endpoint getOwnedEndpoint() {
		return ownedEndpoint;
	}

	public List<ExclusiveStatement> getStatements() {
		// XXX make a copy? (or copyOnWrite list?)
		return owned;
	}
	
	@Override
	public int getFreeVarCount() {
		return freeVars.size();
	}
	
	public Set<String> getFreeVarsSet() {
		return freeVars;
	}
	
	@Override
	public List<String> getFreeVars() {
		return new ArrayList<String>(freeVars);
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<StatementSource> getStatementSources() {
		return owner;
	}

	@Override
	public boolean hasFreeVarsFor(BindingSet bindings) {
		for (String var : freeVars)
			if (!bindings.hasBinding(var))
				return true;
		return false;		
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) throws QueryEvaluationException {
		
		try {
			// use the particular evaluation strategy for evaluation
			return FederationManager.getInstance().getStrategy().evaluateExclusiveGroup(this, bindings);
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
		
	}

	@Override
	public void addFilterExpr(FilterExpr expr) {
		/* 
		 * Note: the operation is obsolete for this class: all filters are added already
		 * in the owned children during optimization (c.f. FilterOptimizer)
		 */
		throw new UnsupportedOperationException("Operation not supported for " + ExclusiveGroup.class.getCanonicalName() + ", filters already to children during optimization.");
			
	}

	@Override
	public FilterValueExpr getFilterExpr() {
		return filter;
	}

	@Override
	public boolean hasFilter() {
		return filter!=null;
	}
	
	@Override
	public void addBoundFilter(final String varName, final Value value) {
		/* 
		 * Note: the operation is obsolete for this class: all bindings are set already
		 * in the owned children during optimization (c.f. FilterOptimizer)
		 */
		throw new UnsupportedOperationException("Operation not supported for " + ExclusiveGroup.class.getCanonicalName() + ", bindings inserted during optimization.");
	}

	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}
}
