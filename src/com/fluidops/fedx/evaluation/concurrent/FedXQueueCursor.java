package com.fluidops.fedx.evaluation.concurrent;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.QueueCursor;

/**
 * Specialized variants of {@link QueueCursor} which avoids converting any
 * exception if it is already of type{@link QueryEvaluationException}.
 * 
 * 
 * @author Andreas Schwarte
 *
 * @param <T>
 */
public class FedXQueueCursor<T> extends QueueCursor<CloseableIteration<T, QueryEvaluationException>> {

	public FedXQueueCursor(int capacity) {
		super(capacity);
	}

	@Override
	protected QueryEvaluationException convert(Exception e) {
		if (e instanceof QueryEvaluationException) {
			return (QueryEvaluationException) e;
		}
		return super.convert(e);
	}

}
