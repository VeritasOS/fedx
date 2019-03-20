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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.evaluation.concurrent.FedXQueueCursor;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.exception.ExceptionUtil;


/**
 * Base class for any parallel union executor.
 * 
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this 
 * class is applicable for pipelining when used in a different thread (access to shared
 * variables is synchronized).
 * 
 * @author Andreas Schwarte
 *
 */
public abstract class UnionExecutorBase<T> extends LookAheadIteration<T, QueryEvaluationException> implements ParallelExecutor<T> {

	private static final Logger log = LoggerFactory.getLogger(UnionExecutorBase.class);
	protected static final AtomicInteger NEXT_UNION_ID = new AtomicInteger(1);
	
	/* Constants */
	protected final int unionId;							// the union id
	
	/* Variables */
	protected volatile boolean closed;
	protected boolean finished = true;
	
	protected QueueCursor<CloseableIteration<T, QueryEvaluationException>> result = new FedXQueueCursor<T>(1024);
	protected CloseableIteration<T, QueryEvaluationException> rightIter;
	
	
	public UnionExecutorBase() {
		this.unionId = NEXT_UNION_ID.getAndIncrement();
	}
	

	@Override
	public final void run() {

		try {
			union();
		} catch (Throwable t) {
			toss(ExceptionUtil.toException(t));
		} finally {
			finished=true;
			result.done();
		}
		
	}
	

	/**
	 * 
	 * Note: this method must block until the union is executed completely. Otherwise
	 * the result queue is marked as committed while this isn't the case. The blocking
	 * behavior in general is no problem: If you need concurrent access to the result
	 * (i.e. pipelining) just run the union in a separate thread. Access to the result
	 * iteration is synchronized.
	 * 
	 * @throws Exception
	 */
	protected abstract void union() throws Exception;
	
	
	@Override
	public void addResult(CloseableIteration<T, QueryEvaluationException> res)  {
		/* optimization: avoid adding empty results */
		if (res instanceof EmptyIteration<?,?>)
			return;
		
		try {
			result.put(res);
		} catch (InterruptedException e) {
			throw new RuntimeException("Error adding element to result queue", e);
		}

	}
		
	@Override
	public void done() {
		;	// no-op
	}
	
	@Override
	public void toss(Exception e) {
		log.warn("Error executing union operator: " + e.getMessage());
		result.toss(e);
	}
	
	
	@Override
	public T getNextElement() throws QueryEvaluationException	{
		// TODO check if we need to protect rightQueue from synchronized access
		// wasn't done in the original implementation either
		// if we see any weird behavior check here !!

		while (rightIter != null || result.hasNext()) {
			if (rightIter == null) {
				rightIter = result.next();
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
		
		if (rightIter != null) {
			rightIter.close();
			rightIter = null;
		}

	}
	
	/**
	 * Return true if this executor is finished or aborted
	 * 
	 * @return whether the executor is finished
	 */
	public boolean isFinished() {
		synchronized (this) {
			return finished;
		}
	}
}
