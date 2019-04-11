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
package com.fluidops.fedx.exception;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.endpoint.Endpoint;

/**
 * Convenience functions to handle exceptions.
 * 
 * @author Andreas Schwarte
 *
 */
public class ExceptionUtil {

	protected static final Logger log = LoggerFactory.getLogger(ExceptionUtil.class);
	
	/**
	 * Regex pattern to identify http error codes from the title of the returned document:
	 * 
	 * <code>
	 * Matcher m = httpErrorPattern.matcher("[..] <title>503 Service Unavailable</title> [..]");
	 * if (m.matches()) {
	 * 		System.out.println("HTTP Error: " + m.group(1);
	 * }
	 * </code>
	 */
	protected static final Pattern httpErrorPattern = Pattern.compile(".*<title>(.*)</title>.*", Pattern.DOTALL);
	
	
	/**
	 * Trace the exception source within the exceptions to identify the originating endpoint. The message
	 * of the provided exception is adapted to "@ endpoint.getId() - %orginalMessage".<p>
	 * 
	 * Note that in addition HTTP error codes are extracted from the title, if the exception resulted from
	 * an HTTP error, such as for instance "503 Service unavailable"
	 * 
	 * @param endpoint the the endpoint
	 * @param ex
	 * 			the exception
	 * @param additionalInfo
	 * 			additional information that might be helpful, e.g. the subquery
	 * 
	 * @return
	 * 		 	a modified exception with endpoint source
	 */
	public static QueryEvaluationException traceExceptionSource(Endpoint endpoint, Throwable ex,
			String additionalInfo) {
		
		
		String eID;
		
		if (endpoint==null) {
			log.warn("No endpoint found for connection, probably changed from different thread.");
			eID = "unknown";
		} else {
			eID = endpoint.getId();
		}
		
		// check for http error code (heuristic)
		String message = ex.getMessage();
		message = message==null ? "n/a" : message;
		Matcher m = httpErrorPattern.matcher(message);
		if (m.matches()) {
			log.debug("HTTP error detected for endpoint " + eID + ":\n" + message);
			message = "HTTP Error: " + m.group(1);
		} else {
			log.trace("No http error found");
		}

		if (!(ex instanceof QueryEvaluationException)) {
			message += ". Original exception type: " + ex.getClass().getName();
		}
		
		QueryEvaluationException res = new QueryEvaluationException("@ " + eID + " - " + message + ". " + additionalInfo, ex.getCause());
		res.setStackTrace(ex.getStackTrace());
		return res;
	}
	
	
	/**
	 * Repair the connection and then trace the exception source.
	 * 
	 * @param endpoint
	 * @param ex
	 * @return the exception
	 */
	public static QueryEvaluationException traceExceptionSourceAndRepair(Endpoint endpoint, Throwable ex,
			String additionalInfo) {
		return traceExceptionSource(endpoint, ex, additionalInfo);
	}

	
	/**
	 * Return the exception in a convenient representation, i.e. '%msg% (%CLASS%): %ex.getMessage()%'
	 * 
	 * @param msg
	 * @param ex
	 * 
	 * @return
	 * 		the exception in a convenient representation
	 */
	public static String getExceptionString(String msg, Throwable ex) {
		return msg + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
	}
	
	
	/**
	 * If possible change the message text of the specified exception. This is only possible
	 * if the provided exception has a public constructor with String and Throwable as argument.
	 * The new message is set to 'msgPrefix. ex.getMessage()', all other exception elements 
	 * remain the same.
	 * 
	 * @param <E>
	 * @param msgPrefix
	 * @param ex
	 * @param exClazz
	 * 
	 * @return the updated exception
	 */
	public static <E extends Exception> E changeExceptionMessage(String msgPrefix, E ex, Class<E> exClazz) {
		
		Constructor<E> constructor = null;
		
		try {
			// try to find the constructor 'public Exception(String, Throwable)'
			constructor = exClazz.getConstructor(new Class<?>[] {String.class, Throwable.class});
		} catch (SecurityException e) {
			log.warn("Cannot change the message of exception class " + exClazz.getCanonicalName() + " due to SecurityException: " + e.getMessage());
			return ex;
		} catch (NoSuchMethodException e) {
			log.warn("Cannot change the message of exception class " + exClazz.getCanonicalName() + ": Constructor <String, Throwable> not found.");
			return ex;
		}
		
		
		E newEx;
		try {
			newEx = constructor.newInstance(new Object[] {msgPrefix + "." + ex.getMessage(), ex.getCause()});
		} catch (Exception e) {
			log.warn("Cannot change the message of exception class " + exClazz.getCanonicalName() + " due to " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return ex;
		}
		newEx.setStackTrace(ex.getStackTrace());
		
		return newEx;
	}

	/**
	 * Converts the {@link Throwable} to an {@link Exception}. If it is already of
	 * type exception, it is returned as is. Otherwise, we create a new exception,
	 * and attache the stack trace and a meaningful message.
	 * 
	 * @param t
	 * @return the {@link Exception}
	 */
	public static Exception toException(Throwable t) {
		Exception e;
		if (t instanceof TimeoutException) {
			e = new QueryInterruptedException("Query evaluation has run into a timeout.", t);
		} else if (t instanceof Exception) {
			e = (Exception) t;
		} else {
			e = new Exception("" + t.getMessage() + ". Original type: " + t.getClass());
			e.setStackTrace(t.getStackTrace());
		}
		return e;
	}
}
