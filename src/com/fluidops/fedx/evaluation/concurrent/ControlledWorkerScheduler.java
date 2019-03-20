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
package com.fluidops.fedx.evaluation.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.evaluation.join.ControlledWorkerBoundJoin;
import com.fluidops.fedx.evaluation.join.ControlledWorkerJoin;
import com.fluidops.fedx.evaluation.union.ControlledWorkerUnion;
import com.fluidops.fedx.exception.ExceptionUtil;
import com.fluidops.fedx.exception.FedXRuntimeException;



/**
 * ControlledWorkerScheduler is a task scheduler that uses a FIFO queue for managing
 * its process. Each instance has a pool with a fixed number of worker threads. Once
 * notified a worker picks the next task from the queue and executes it. The results
 * is then returned to the controlling instance retrieved from the task.
 * 
 * 
 * @author Andreas Schwarte
 * 
 * @see ControlledWorkerUnion
 * @see ControlledWorkerJoin
 * @see ControlledWorkerBoundJoin
 */
public class ControlledWorkerScheduler<T> implements Scheduler<T> {

	protected static final Logger log = LoggerFactory.getLogger(ControlledWorkerScheduler.class);

	
	protected ExecutorService executor;

	protected LinkedBlockingQueue<Runnable> _taskQueue = new LinkedBlockingQueue<>();



	protected int nWorkers;
	protected String name;
	
		
	/**
	 * Construct a new instance with 20 workers.
	 */
	public ControlledWorkerScheduler() {
		this(20, "FedX Worker");
	}

	
	/**
	 * Construct a new instance with the specified number of workers and the
	 * given name.
	 * 
	 * @param nWorkers
	 * @param name
	 */
	public ControlledWorkerScheduler(int nWorkers, String name) {
		this.nWorkers = nWorkers;
		this.name = name;
		initWorkerThreads();
	}
	
	
	/**
	 * Schedule the specified parallel task.
	 * 	
	 * @param task
	 * 			the task to schedule
	 */
	@Override
	public void schedule(ParallelTask<T> task) {
		
		WorkerRunnable runnable = new WorkerRunnable(task);

		Future<?> future = executor.submit(runnable);

		// register the future to the task
		if (task instanceof ParallelTaskBase<?>) {
			((ParallelTaskBase<?>) task).setScheduledFuture(future);
		}
		task.getQueryInfo().registerScheduledTask(task);

		// TODO rejected execution exception?
		
	}	
	
	
	/**
	 * Schedule the given tasks and inform about finish using the same lock, i.e.
	 * all tasks are scheduled one after the other.
	 * @param tasks
	 * @param control
	 */
	public void scheduleAll(List<ParallelTask<T>> tasks, ParallelExecutor<T> control) {
		for (ParallelTask<T> task : tasks)
		{
			schedule(task);
		}
		
	}
	
	public int getTotalNumberOfWorkers() {
		return nWorkers;
	}
	
	public int getNumberOfIdleWorkers() {
		// TODO
		return -1;
	}
	
	public int getNumberOfTasks() {
		return _taskQueue.size();
	}
	
	protected void initWorkerThreads() {

		executor = new ThreadPoolExecutor(Math.min(10, nWorkers / 2), nWorkers, 30L, TimeUnit.SECONDS, _taskQueue,
				new NamingThreadFactory(name));
	}
	
	@Override
	public void abort() {
		log.info("Aborting workers of " + name + ".");

		executor.shutdownNow();
		try
		{
			executor.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			throw new FedXRuntimeException(e);
		}
	}

	
	

	@Override
	public void done() {
		/* not needed here, implementations call informFinish(control) to notify done status */		
	}


	@Override
	public void handleResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* not needed here since the result is passed directly to the control instance */		
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	@Override
	public void informFinish() {
		throw new RuntimeException("Unsupported Operation for this scheduler!");	
	}
	
	/**
	 * Inform this scheduler that the specified control instance will no longer
	 * submit tasks.
	 * 
	 * @param control
	 */
	public void informFinish(ParallelExecutor<T> control) {
		
		// TODO
	}
	

	@Override
	public boolean isRunning() {
		/* Note: this scheduler can only determine runtime for a given control instance! */
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	
	/**
	 * Determine if there are still task running or queued for the specified control.
	 * 
	 * @param control
	 * 
	 * @return
	 * 		true, if there are unfinished tasks, false otherwise
	 */
	public boolean isRunning(ParallelExecutor<T> control) {
		return true; // TODO
	}
	
	
	@Override
	public void toss(Exception e) {
		/* not needed here: exceptions are directly tossed to the controlling instance */		
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	
	
	protected class WorkerRunnable implements Runnable {

		protected final ParallelTask<T> task;
		
		protected boolean inTask = false;

		protected boolean aborted = false;
		
		public WorkerRunnable(ParallelTask<T> task)
		{
			super();
			this.task = task;
		}


		@Override
		public void run()
		{
			if (aborted)
			{
				return;
			}

			ParallelExecutor<T> taskControl = task.getControl();
			
			try {
				inTask = true;
				if (log.isTraceEnabled())
				{
					log.trace("Performing task " + task.toString() + " in " + Thread.currentThread().getName());
				}
				CloseableIteration<T, QueryEvaluationException> res = task.performTask();
				inTask = false;
				taskControl.addResult(res);

				taskControl.done();		// in most cases this is a no-op
			} catch (Throwable t) {
				if (aborted)
				{
					return;
				}
				log.debug("Exception encountered while evaluating task (" + t.getClass().getSimpleName() + "): " + t.getMessage());
				taskControl.toss(ExceptionUtil.toException(t));
			}
			
		}
		
		public void abort()
		{
			this.aborted = true;
		}
	}
	
	
	
	/**
	 * Structure to maintain the status for a given control instance.
	 * 
	 * @author Andreas Schwarte
	 */
	protected class ControlStatus {
		public int waiting;
		public boolean done;
		public ControlStatus(int waiting, boolean done) {
			this.waiting = waiting;
			this.done = done;
		}
	}
	
	


	@Override
	public void shutdown()
	{
		executor.shutdown();
		try
		{
			executor.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			throw new FedXRuntimeException(e);
		}

	}
}
