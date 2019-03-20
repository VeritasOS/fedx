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
package com.fluidops.fedx;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.exception.ExceptionUtil;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.structures.Endpoint;


/**
 * EndpointManager is the singleton instance that manages available {@link Endpoint}s. 
 * Particular endpoints can be looked up by their id and connection and all relevant 
 * information can be used.
 * 
 * @author Andreas Schwarte
 *
 */
public class EndpointManager {
	
	protected static final Logger log = LoggerFactory.getLogger(EndpointManager.class);
	
	/*
	 * TODO
	 * we probably need to make this class thread safe! => synchronized access
	 */
	
	protected static EndpointManager instance = null;
	
	/**
	 * @return
	 * 		return the singleton instance of the EndpointManager
	 */
	public static EndpointManager getEndpointManager() {
		if (instance==null)
			throw new FedXRuntimeException("EndpointManager not yet initialized, initialize() must be invoked before use.");
		return instance;
	}
	
	/**
	 * Initialize the singleton endpoint manager without any endpoints
	 */
	public static void initialize() {
		initialize(null);
	}
	
	/**
	 * Initialize the singleton endpoint manager with the provided endpoints
	 * 
	 * @param endpoints
	 */
	public static synchronized void initialize(List<Endpoint> endpoints) {
		if (instance!=null)
			throw new FedXRuntimeException("Endpoint Manager already initialized.");
		instance = new EndpointManager(endpoints);
	}
			
	
	// map enpoint ids and connections to the corresponding endpoint
	protected HashMap<String, Endpoint> endpoints = new HashMap<String, Endpoint>();
	protected HashMap<RepositoryConnection, Endpoint> connToEndpoint = new HashMap<RepositoryConnection, Endpoint>();
	
	protected boolean inRepair = false;
	protected Long lastRepaired = -1L;
	
	/**
	 * Construct an EndpointManager without any endpoints
	 */
	private EndpointManager() {
		this(null);
	}
	
	/**
	 * Construct an EndpointManager with the provided endpoints
	 * 
	 * @param endpoints
	 */
	private EndpointManager(List<Endpoint> endpoints) {
		init(endpoints);
	}	
	
	/**
	 * Initialize the endpoint mapping with the provided endpoints
	 * 
	 * @param _endpoints
	 * 				a list of (initialized) endpoints or null
	 */
	private void init(List<Endpoint> _endpoints) {
		if (_endpoints!=null)
			for (Endpoint e : _endpoints)
				addEndpoint(e);
	}

	/**
	 * Add the (initialized) endpoint to this endpoint manager to be used by
	 * the {@link FederationManager}.
	 * 
	 * @param e
	 * 			the endpoint
	 */
	public void addEndpoint(Endpoint e) {
		endpoints.put(e.getId(), e);
		connToEndpoint.put(e.getConn(), e);
	}
	
	/**
	 * Repair the connection of the endpoint corresponding to the provided connection.
	 * 
	 * @param conn
	 * @return the new RepositoryConnection
	 * 
	 * @throws FedXException
	 * 
	 */
	public RepositoryConnection repairConnection(RepositoryConnection conn) throws RepositoryException {
		Endpoint e = connToEndpoint.remove(conn);
		
		if (e==null) {
			log.warn("No endpoint found for provided connection, probably already repaired in another thread.");
			return null;		// TODO check if this makes sense
		}
		
		try {
			RepositoryConnection res = e.repairConnection();
			connToEndpoint.put(res, e);
			return res;
		} catch (RepositoryException ex) {
			throw ExceptionUtil.changeExceptionMessage("Connection of endpoint " + e.getId() + " could not be repaired", ex, RepositoryException.class);
		}
	}
	
	
	public void repairAllConnections() throws RepositoryException {
		
				
		synchronized (this) {
			
			// if there was a repair in the previous 3 seconds, abort
			if (System.currentTimeMillis()-lastRepaired<3000)
				return;
			
			log.info("Repairing all connections after error.");
			
			try {
				inRepair = true;
				for (Endpoint e : endpoints.values()) {
					connToEndpoint.remove(e.getConn());
					RepositoryConnection newConn = e.repairConnection();
					connToEndpoint.put(newConn, e);
				}
				lastRepaired=System.currentTimeMillis();
			} catch (RepositoryException r) {
				throw ExceptionUtil.changeExceptionMessage("Connection of at least one endpoint could not be repaired.", r, RepositoryException.class);
			} finally {
				inRepair = false;
			}
		}
	}
	
	
	/**
	 * Remove the provided endpoint from this endpoint manager to be used by
	 * the {@link FederationManager}. In addition, this method unregisters
	 * the {@link FederatedService} from Sesame
	 *  
	 * @param e
	 * 			the endpoint
	 * 
	 * @throws NoSuchElementException
	 * 			if there is no mapping for some endpoint id
	 */
	protected void removeEndpoint(Endpoint e) throws NoSuchElementException{
		if (!endpoints.containsKey(e.getId()))
			throw new NoSuchElementException("No endpoint avalaible for id " + e.getId());
		endpoints.remove(e.getId());
		connToEndpoint.remove(e.getConn());
	}
	
	
	
	/**
	 * @return
	 * 		a collection of available endpoints in this endpoint manager
	 */
	public Collection<Endpoint> getAvailableEndpoints() {
		return endpoints.values();
	}
	
	/**
	 * @param endpointID
	 * @return
	 * 		the endpoint corresponding to the provided id or null
	 */
	public Endpoint getEndpoint(String endpointID) {
		return endpoints.get(endpointID);
	}
	
	public Endpoint getEndpoint(RepositoryConnection conn) {
		return connToEndpoint.get(conn);
	}
	
	/**
	 * Return the Endpoint for the provided endpoint url, if it exists. Otherwise
	 * return null.
	 * 
	 * @param endpointUrl
	 * @return the endpoint by its URL
	 */
	public Endpoint getEndpointByUrl(String endpointUrl) {
		for (Endpoint e : endpoints.values()) {
			if (e.getEndpoint().equals(endpointUrl))
				return e;
		}
		return null;
	}
	
	public Endpoint getEndpointByName(String endpointName) {
		for (Endpoint e : endpoints.values()) {
			if (e.getName().equals(endpointName))
				return e;
		}
		return null;
	}
	
	/**
	 * @param endpointIDs
	 * @return
	 * 		a list of endpoints corresponding to the provided ids
	 * 
	 * @throws NoSuchElementException
	 * 			if there is no mapping for some endpoint id
	 */
	public List<Endpoint> getEndpoints(Set<String> endpointIDs) throws NoSuchElementException {
		List<Endpoint> res = new ArrayList<Endpoint>();
		for (String endpointID : endpointIDs) {
			Endpoint e = endpoints.get(endpointID);
			if (e==null)
				throw new NoSuchElementException("No endpoint found for " + endpointID + ".");
			res.add(e);
		}
		return res;
	}
	
	/**
	 * Shutdown the endpoint manager, called from {@link FederationManager#shutDown()}
	 */
	protected synchronized void shutDown() {
		instance=null;
	}
}
