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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.ServiceJoinIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.algebra.CheckStatementPattern;
import com.fluidops.fedx.algebra.ConjunctiveFilterExpr;
import com.fluidops.fedx.algebra.EmptyResult;
import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.FedXService;
import com.fluidops.fedx.algebra.FilterExpr;
import com.fluidops.fedx.algebra.IndependentJoinGroup;
import com.fluidops.fedx.algebra.NJoin;
import com.fluidops.fedx.algebra.NUnion;
import com.fluidops.fedx.algebra.SingleSourceQuery;
import com.fluidops.fedx.algebra.StatementSource;
import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.cache.Cache;
import com.fluidops.fedx.cache.CacheUtils;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;
import com.fluidops.fedx.evaluation.concurrent.ParallelServiceExecutor;
import com.fluidops.fedx.evaluation.join.ControlledWorkerBoundJoin;
import com.fluidops.fedx.evaluation.join.ControlledWorkerJoin;
import com.fluidops.fedx.evaluation.join.SynchronousBoundJoin;
import com.fluidops.fedx.evaluation.join.SynchronousJoin;
import com.fluidops.fedx.evaluation.union.ControlledWorkerUnion;
import com.fluidops.fedx.evaluation.union.ParallelGetStatementsTask;
import com.fluidops.fedx.evaluation.union.ParallelPreparedAlgebraUnionTask;
import com.fluidops.fedx.evaluation.union.ParallelPreparedUnionTask;
import com.fluidops.fedx.evaluation.union.ParallelUnionOperatorTask;
import com.fluidops.fedx.evaluation.union.SynchronousWorkerUnion;
import com.fluidops.fedx.evaluation.union.WorkerUnionBase;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.statistics.Statistics;
import com.fluidops.fedx.structures.QueryInfo;
import com.fluidops.fedx.util.FedXUtil;


/**
 * Base class for the Evaluation strategies.
 * 
 * @author Andreas Schwarte
 * 
 * @see SailFederationEvalStrategy
 * @see SparqlFederationEvalStrategy
 *
 */
public abstract class FederationEvalStrategy extends StrictEvaluationStrategy {
	
	private static final Logger log = LoggerFactory.getLogger(FederationEvalStrategy.class);
	
	
	protected Executor executor;
	protected Cache cache;
	protected Statistics statistics;
	
	public FederationEvalStrategy() {
		super(new org.eclipse.rdf4j.query.algebra.evaluation.TripleSource() {

			@Override
			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
					Resource subj, IRI pred, Value obj, Resource... contexts)
					throws QueryEvaluationException	{
				throw new FedXRuntimeException(
						"Federation Strategy does not support org.openrdf.query.algebra.evaluation.TripleSource#getStatements." +
						" If you encounter this exception, please report it.");
			}

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}}, DelegateFederatedServiceResolver.getInstance());	
		this.executor = FederationManager.getInstance().getExecutor();
		this.cache = FederationManager.getInstance().getCache();
		this.statistics = FederationManager.getInstance().getStatistics();
	}

	
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		
		if (expr instanceof StatementTupleExpr) {
			return ((StatementTupleExpr)expr).evaluate(bindings);
		}
				
		if (expr instanceof NJoin) {
			return evaluateNJoin((NJoin)expr, bindings);
		} 
		
		if (expr instanceof NUnion) {
			return evaluateNaryUnion((NUnion)expr, bindings);
		}
		
		if (expr instanceof ExclusiveGroup) {
			return ((ExclusiveGroup)expr).evaluate(bindings);
		}
		
		if (expr instanceof SingleSourceQuery)
			return evaluateSingleSourceQuery((SingleSourceQuery)expr, bindings);
			
		if (expr instanceof FedXService) {
			return evaluateService((FedXService)expr, bindings);
		}
		
		if (expr instanceof IndependentJoinGroup) {
			return evaluateIndependentJoinGroup((IndependentJoinGroup)expr, bindings);	// XXX
		}
		
		if(expr instanceof EmptyResult)
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		
		return super.evaluate(expr, bindings);
	}
	
	
	
	
	/**
	 * Retrieve the statements matching the provided subject, predicate and object value from the 
	 * federation members.<p>
	 * 
	 * For a bound statement, i.e. a statement with no free variables, the statement itself is 
	 * returned if some member has this statement, an empty iteration otherwise.<p>
	 * 
	 * If the statement has free variables, i.e. one of the provided arguments in <code>null</code>,
	 * the union of results from relevant statement sources is constructed.
	 * 
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @return the statement iteration
	 * 
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public CloseableIteration<Statement, QueryEvaluationException> getStatements(QueryInfo queryInfo, Resource subj,
			IRI pred, Value obj, Resource... contexts)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{

		if (contexts.length!=0)
			log.warn("Context queries are not yet supported by FedX.");
		
		List<Endpoint> members = FederationManager.getInstance().getFederation().getMembers();
		
		
		// a bound query: if at least one fed member provides results
		// return the statement, otherwise empty result
		if (subj!=null && pred!=null && obj!=null) {
			if (CacheUtils.checkCacheUpdateCache(cache, members, subj, pred, obj)) {
				return new SingletonIteration<Statement, QueryEvaluationException>(
						FedXUtil.valueFactory().createStatement(subj, pred, obj));
			}
			return new EmptyIteration<Statement, QueryEvaluationException>();
		}
		
		// form the union of results from relevant endpoints
		List<StatementSource> sources = CacheUtils.checkCacheForStatementSourcesUpdateCache(cache, members, subj, pred, obj);
		
		if (sources.size()==0)
			return new EmptyIteration<Statement, QueryEvaluationException>();
		
		if (sources.size()==1) {
			Endpoint e = EndpointManager.getEndpointManager().getEndpoint(sources.get(0).getEndpointID());
			return e.getTripleSource().getStatements(subj, pred, obj, contexts);
		}
		
		// TODO why not collect in parallel?
		WorkerUnionBase<Statement> union = new SynchronousWorkerUnion<Statement>(this, queryInfo);
		
		for (StatementSource source : sources) {
			Endpoint e = EndpointManager.getEndpointManager().getEndpoint(source.getEndpointID());
			ParallelGetStatementsTask task = new ParallelGetStatementsTask(union, e, subj, pred, obj, contexts);
			union.addTask(task);
		}
		
		// run the union in a separate thread
		executor.execute(union);
		
		// TODO distinct iteration ?
		
		return union;
	}
	
	
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateService(FedXService service, BindingSet bindings) throws QueryEvaluationException {
		
		ParallelServiceExecutor pe = new ParallelServiceExecutor(service, this, bindings);
		pe.run();		// non-blocking (blocking happens in the iterator)
		return pe;
	}
	
	
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateSingleSourceQuery(SingleSourceQuery query, BindingSet bindings) throws QueryEvaluationException {
		
		try
		{
			Endpoint source = query.getSource();		
			return source.getTripleSource().getStatements(query.getQueryString(), query.getQueryInfo().getQueryType());
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (MalformedQueryException e)	{
			throw new QueryEvaluationException(e);
		}
		
	}
	
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateNJoin(NJoin join, BindingSet bindings) throws QueryEvaluationException {
		
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluate(join.getArg(0), bindings);
		
		ControlledWorkerScheduler<BindingSet> joinScheduler = FederationManager.getInstance().getJoinScheduler();
		
		for (int i = 1, n = join.getNumberOfArguments(); i < n; i++) {

			result = executeJoin(joinScheduler, result, join.getArg(i), join.getJoinVariables(i), bindings,
					join.getQueryInfo());
		}
		return result;
	}
	
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateNaryUnion(NUnion union, BindingSet bindings) throws QueryEvaluationException {
		
		ControlledWorkerScheduler<BindingSet> unionScheduler = FederationManager.getInstance().getUnionScheduler();
		ControlledWorkerUnion<BindingSet> unionRunnable = new ControlledWorkerUnion<BindingSet>(this, unionScheduler,
				union.getQueryInfo());
		
		for (int i=0; i<union.getNumberOfArguments(); i++) {
			unionRunnable.addTask(new ParallelUnionOperatorTask(unionRunnable, this, union.getArg(i), bindings));
		}
		
		executor.execute(unionRunnable);
		
		return unionRunnable;
	}

	/**
	 * Execute the join in a separate thread using some join executor. 
	 * 
	 * Join executors are for instance:
	 * 	- {@link SynchronousJoin}
	 *  - {@link SynchronousBoundJoin}
	 *  - {@link ControlledWorkerJoin}
	 *  - {@link ControlledWorkerBoundJoin}
	 *
	 * For endpoint federation use controlled worker bound join, for local federation use
	 * controlled worker join. The other operators are there for completeness.
	 * 
	 * Use {@link FederationEvalStrategy#executor} to execute the join (it is a runnable).
	 * 
	 * @param joinScheduler
	 * @param leftIter
	 * @param rightArg
	 * @param joinVariables
	 * @param bindings
	 * @return the result
	 * @throws QueryEvaluationException
	 */
	protected abstract CloseableIteration<BindingSet, QueryEvaluationException> executeJoin(
			ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter, TupleExpr rightArg,
			Set<String> joinVariables, BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException;
	
	
	
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateExclusiveGroup(
			ExclusiveGroup group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException;

	
	
	/**
	 * Evaluate a bound join at the relevant endpoint, i.e. i.e. for a group of bindings retrieve
	 * results for the bound statement from the relevant endpoints
	 * 
	 * @param stmt
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern(StatementTupleExpr stmt, final List<BindingSet> bindings) throws QueryEvaluationException;
	
	/**
	 * Perform a grouped check at the relevant endpoints, i.e. for a group of bindings keep only 
	 * those for which at least one endpoint provides a result to the bound statement.
	 * 
	 * @param stmt
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateGroupedCheck(CheckStatementPattern stmt, final List<BindingSet> bindings) throws QueryEvaluationException;
	
	
	
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateIndependentJoinGroup(IndependentJoinGroup joinGroup, final BindingSet bindings) throws QueryEvaluationException; 
	
	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluateIndependentJoinGroup(IndependentJoinGroup joinGroup, final List<BindingSet> bindings) throws QueryEvaluationException;
		
	
	/**
	 * Evaluate a SERVICE using vectored evaluation, taking the provided bindings as input.
	 * 
	 * See {@link ControlledWorkerBoundJoin} and {@link Config#getEnableServiceAsBoundJoin()}
	 * 
	 * @param service
	 * @param bindings
	 * @return the result iteration
	 * @throws QueryEvaluationException
	 */
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateService(FedXService service, final List<BindingSet> bindings) throws QueryEvaluationException {
		
		return new ServiceJoinIterator(new CollectionIteration<BindingSet, QueryEvaluationException>(bindings), service.getService(), EmptyBindingSet.getInstance(), this);
	}
	
	@Override
	public Value evaluate(ValueExpr expr, BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		
		if (expr instanceof FilterExpr)
			return evaluate((FilterExpr)expr, bindings);
		if (expr instanceof ConjunctiveFilterExpr)
			return evaluate((ConjunctiveFilterExpr)expr, bindings);
		
		return super.evaluate(expr, bindings);
	}
	
	public Value evaluate(FilterExpr node, BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		
		Value v = evaluate(node.getExpression(), bindings);
		return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(v));
	}
	
	
	public Value evaluate(ConjunctiveFilterExpr node, BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		
		ValueExprEvaluationException error = null;
		
		for (FilterExpr expr : node.getExpressions()) {
			
			try {
				Value v = evaluate(expr.getExpression(), bindings);
				if (QueryEvaluationUtil.getEffectiveBooleanValue(v) == false) {
					return BooleanLiteral.FALSE;
				}
			} catch (ValueExprEvaluationException e) {
				error = e;
			}
		}
		
		if (error!=null)
			throw error;
		
		return BooleanLiteral.TRUE;
	}
	
	
	
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(Object preparedQuery, List<StatementSource> statementSources, QueryInfo queryInfo) throws QueryEvaluationException {
		if (preparedQuery instanceof String)
			return evaluateAtStatementSources((String)preparedQuery, statementSources, queryInfo);
		if (preparedQuery instanceof TupleExpr)
			return evaluateAtStatementSources((TupleExpr)preparedQuery, statementSources, queryInfo);
		throw new RuntimeException("Unsupported type for prepared query: " + preparedQuery.getClass().getCanonicalName());
	}
	
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(String preparedQuery, List<StatementSource> statementSources, QueryInfo queryInfo) throws QueryEvaluationException {
		
		try {
			CloseableIteration<BindingSet, QueryEvaluationException> result;
			
			if (statementSources.size() == 1) {
				Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(statementSources.get(0).getEndpointID());
				com.fluidops.fedx.evaluation.TripleSource t = ownedEndpoint.getTripleSource();
				result = t.getStatements(preparedQuery, EmptyBindingSet.getInstance(), null);
			} 
			 
			else {
				WorkerUnionBase<BindingSet> union = FederationManager.getInstance().createWorkerUnion(queryInfo);
				
				for (StatementSource source : statementSources) {
					Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(source.getEndpointID());
					union.addTask(new ParallelPreparedUnionTask(union, preparedQuery, ownedEndpoint,
							EmptyBindingSet.getInstance(), null));
				}
				
				union.run();
				result = union;
				
				// TODO we should add some DISTINCT here to have SET semantics
			}
		
			return result;
			
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}
	
	
	protected CloseableIteration<BindingSet, QueryEvaluationException> evaluateAtStatementSources(TupleExpr preparedQuery, List<StatementSource> statementSources, QueryInfo queryInfo) throws QueryEvaluationException {
		
		try {
			CloseableIteration<BindingSet, QueryEvaluationException> result;
			
			if (statementSources.size() == 1) {
				Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(statementSources.get(0).getEndpointID());
				com.fluidops.fedx.evaluation.TripleSource t = ownedEndpoint.getTripleSource();
				result = t.getStatements(preparedQuery, EmptyBindingSet.getInstance(), null);
			} 
			 
			else {			
				WorkerUnionBase<BindingSet> union = FederationManager.getInstance().createWorkerUnion(queryInfo);
				
				for (StatementSource source : statementSources) {					
					Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(source.getEndpointID());
					union.addTask(new ParallelPreparedAlgebraUnionTask(union, preparedQuery, ownedEndpoint,
							EmptyBindingSet.getInstance(), null));
				}
				
				union.run();
				result = union;
				
				// TODO we should add some DISTINCT here to have SET semantics
			}
		
			return result;
			
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

}
