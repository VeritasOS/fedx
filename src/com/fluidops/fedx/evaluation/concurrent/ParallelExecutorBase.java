/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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
package com.fluidops.fedx.evaluation.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.join.JoinExecutorBase;
import com.fluidops.fedx.evaluation.union.UnionExecutorBase;
import com.fluidops.fedx.exception.ExceptionUtil;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * Base class for common parallel executors such as {@link JoinExecutorBase} and
 * {@link UnionExecutorBase}.
 * 
 * @author Andreas Schwarte
 *
 * @param <T>
 * @see JoinExecutorBase
 * @see UnionExecutorBase
 */
public abstract class ParallelExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException>
		implements ParallelExecutor<T> {

	
	protected static final Logger log = LoggerFactory.getLogger(ParallelExecutorBase.class);

	protected static final AtomicInteger NEXT_EXECUTOR_ID = new AtomicInteger(1);
	
	/* Constants */
	protected final FederationEvalStrategy strategy;		// the evaluation strategy
	protected final int executorId; // the join id
	protected final QueryInfo queryInfo;

	/* Variables */
	protected volatile Thread evaluationThread;
	protected QueueCursor<CloseableIteration<T, QueryEvaluationException>> rightQueue = new FedXQueueCursor<T>(1024);
	protected CloseableIteration<T, QueryEvaluationException> rightIter;
	protected volatile boolean closed;
	protected boolean finished = false;

	public ParallelExecutorBase(FederationEvalStrategy strategy, QueryInfo queryInfo) throws QueryEvaluationException {
		this.strategy = strategy;
		this.executorId = NEXT_EXECUTOR_ID.getAndIncrement();
		this.queryInfo = queryInfo;
	}

	@Override
	public final void run() {
		evaluationThread = Thread.currentThread();

		if (log.isTraceEnabled())
			log.trace("Performing execution of " + getDisplayId() + ", thread: " + evaluationThread.getName());

		try {
			performExecution();
			checkTimeout();
		} catch (Throwable t) {
			toss(ExceptionUtil.toException(t));
		} finally {
			finished = true;
			evaluationThread = null;
			rightQueue.done();
		}

		if (log.isTraceEnabled())
			log.trace(getDisplayId() + " is finished.");
	}

	/**
	 * Perform the parallel execution.
	 * 
	 * Note that this method must block until the execution is completed.
	 * 
	 * @throws Exception
	 */
	protected abstract void performExecution() throws Exception;

	@Override
	public void addResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?, ?>)
			return;

		try {
			rightQueue.put(res);
		} catch (InterruptedException e) {
			throw new RuntimeException("Error adding element to right queue", e);
		}
	}

	@Override
	public void done() {
		; // no-op
	}

	@Override
	public void toss(Exception e) {
		rightQueue.toss(e);
		if (log.isTraceEnabled()) {
			log.trace("Tossing exception of executor " + getId() + ": " + e.getMessage());
		}
	}

	@Override
	public T getNextElement() throws QueryEvaluationException {
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || rightQueue.hasNext()) {
			if (rightIter == null) {
				rightIter = rightQueue.next();
			}
			if (rightIter.hasNext()) {
				return rightIter.next();
			} else {
				rightIter.close();
				rightIter = null;
			}
		}

		rightQueue.checkException();
		return null;
	}

	/**
	 * Checks whether the query execution has run into a timeout. If so, a
	 * {@link QueryInterruptedException} is thrown.
	 * 
	 * @throws QueryInterruptedException
	 */
	protected void checkTimeout() throws QueryInterruptedException {
		long maxTimeLeft = queryInfo.getMaxRemainingTimeMS();
		if (maxTimeLeft <= 0) {
			throw new QueryInterruptedException("Query evaluation has run into a timeout");
		}
	}

	@Override
	public void handleClose() throws QueryEvaluationException {

		try {
			rightQueue.close();
		} finally {

			if (rightIter != null) {
				rightIter.close();
				rightIter = null;
			}
		}
		closed = true;
	}

	/**
	 * Return true if this executor is finished or aborted
	 * 
	 * @return whether the executor is finished
	 */
	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	/**
	 * @return a unique identifier of this execution
	 */
	protected String getId() {
		return "#" + executorId + " (Query: " + queryInfo.getQueryID() + ")";
	}

	public String getDisplayId() {
		return getExecutorType() + " " + getId();
	}

	/**
	 * 
	 * @return the executor type, e.g. "Join". Default "Executor"
	 */
	protected String getExecutorType() {
		return "Executor";
	}

	@Override
	public String toString() {
		return getExecutorType() + " " + getClass().getSimpleName() + " {id: " + getId() + "}";
	}
}
