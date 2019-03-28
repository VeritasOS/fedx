package com.fluidops.fedx.evaluation.concurrent;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(FedXQueueCursor.class);

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

	@Override
	public void handleClose() throws QueryEvaluationException {
		done();

		try {
			// consume all remaining elements from the queue
			while (hasNext()) {
				try {
					log.trace("Attempting to close non consumed inner iteration.");
					CloseableIteration<T, QueryEvaluationException> closable = next();
					closable.close();
				} catch (Exception e) {
					log.debug("Failed to closed inner iteration: " + e.getMessage());
					log.trace("Details: ", e);
				}
			}
		} finally {
			super.handleClose();
		}
	}

}
