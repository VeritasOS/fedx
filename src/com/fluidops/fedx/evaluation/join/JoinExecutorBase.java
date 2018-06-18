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
package com.fluidops.fedx.evaluation.join;

import org.apache.log4j.Logger;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.QueueCursor;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Base class for any join parallel join executor. 
 * 
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this 
 * class is applicable for pipelining when used in a different thread (access to shared
 * variables is synchronized).
 * 
 * @author Andreas Schwarte
 */
public abstract class JoinExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException> implements ParallelExecutor<T> {

	public static Logger log = Logger.getLogger(JoinExecutorBase.class);
	
	protected static int NEXT_JOIN_ID = 1;
	
	/* Constants */
	protected final FederationEvalStrategy strategy;		// the evaluation strategy
	protected final TupleExpr rightArg;						// the right argument for the join
	protected final BindingSet bindings;					// the bindings
	protected final int joinId;								// the join id
	protected final QueryInfo queryInfo;
	
	/* Variables */
	protected volatile Thread evaluationThread;
	protected CloseableIteration<T, QueryEvaluationException> leftIter;
	protected CloseableIteration<T, QueryEvaluationException> rightIter;
	protected volatile boolean closed;
	protected boolean finished = false;
	
	protected QueueCursor<CloseableIteration<T, QueryEvaluationException>> rightQueue = new QueueCursor<CloseableIteration<T, QueryEvaluationException>>(1024);

	
	public JoinExecutorBase(FederationEvalStrategy strategy, CloseableIteration<T, QueryEvaluationException> leftIter, TupleExpr rightArg,
			BindingSet bindings, QueryInfo queryInfo) throws QueryEvaluationException	{
		this.strategy = strategy;
		this.leftIter = leftIter;
		this.rightArg = rightArg;
		this.bindings = bindings;
		this.joinId = NEXT_JOIN_ID++;
		this.queryInfo = queryInfo;
	}
	

	@Override
	public final void run() {
		evaluationThread = Thread.currentThread();
		

		if (log.isTraceEnabled())
			log.trace("Performing join #" + joinId + ", thread: " + evaluationThread.getName());
		
		try {
			handleBindings();
		} catch (Exception e) {
			toss(e);
		} finally {
			finished=true;
			evaluationThread = null;
			rightQueue.done();
		}
				
		if (log.isTraceEnabled())
			log.trace("Join #" + joinId + " is finished.");
	}
	
	/**
	 * Implementations must implement this method to handle bindings.
	 * 
	 * Use the following as a template
	 * <code>
	 * while (!closed && leftIter.hasNext()) {
	 * 		// your code
	 * }
	 * </code>
	 * 
	 * and add results to rightQueue. Note that addResult() is implemented synchronized
	 * and thus thread safe. In case you can guarantee sequential access, it is also
	 * possible to directly access rightQueue
	 * 
	 */
	protected abstract void handleBindings() throws Exception;
	
	
	@Override
	public void addResult(CloseableIteration<T, QueryEvaluationException> res)  {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?,?>)
			return;
		
		try {
			rightQueue.put(res);
		} catch (InterruptedException e) {
			throw new RuntimeException("Error adding element to right queue", e);
		}
	}
		
	@Override
	public void done() {
		;	// no-op
	}
	
	@Override
	public void toss(Exception e) {
		rightQueue.toss(e);
	}
	
	
	@Override
	public T getNextElement() throws QueryEvaluationException	{
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || rightQueue.hasNext()) {
			if (rightIter == null) {
				rightIter = rightQueue.next();
			}
			if (rightIter.hasNext()) {
				return rightIter.next();
			}
			else {
				rightIter.close();
				rightIter = null;
			}
		}

		return null;
	}

	
	@Override
	public void handleClose() throws QueryEvaluationException {
		closed = true;
		if (evaluationThread != null) {
			evaluationThread.interrupt();
		}
		
		if (rightIter != null) {
			rightIter.close();
			rightIter = null;
		}

		leftIter.close();
	}
	
	/**
	 * Return true if this executor is finished or aborted
	 * 
	 * @return whether the join is finished
	 */
	public boolean isFinished() {
		synchronized (this) {
			return finished;
		}
	}
	
	/**
	 * Retrieve information about this join, joinId and queryId
	 * 
	 * @return the ID
	 */
	public String getId() {
		return "ID=(id:" + joinId + "; query:" + getQueryId() + ")";
	}
	
	@Override
	public int getQueryId() {
		if (queryInfo!=null)
			return queryInfo.getQueryID();
		return -1;
	}
}
