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
package com.fluidops.fedx.config;

import java.util.Collections;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.structures.Endpoint;

/**
 * A {@link SailFactory} that initializes FedX Sails based on 
 * the provided configuration data.
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXSailFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "fluidops:FedX";
	
	
	@Override
	public SailImplConfig getConfig() {
		return new FedXSailConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException	{
		
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}
		
		if (!(config instanceof FedXSailConfig)) {
			throw new SailConfigException("FedXSail config expected, was " + config.getClass().getCanonicalName());
		}	
		
		FedXSailConfig fedXSailConfig = (FedXSailConfig)config;
		String fedxConfig = fedXSailConfig.getFedxConfig();
		
		if (fedxConfig==null)
			throw new SailConfigException("FedX Sail Configuration must not be null");
		
		try	{
			FedXFactory.initializeFederation(fedxConfig, Collections.<Endpoint>emptyList());
		} catch (FedXException e) {
			throw new SailConfigException(e);
		}
		
		return FederationManager.getInstance().getFederation();
	}

	/**
	 * Returns the Sail's type: <tt>fluidops:FedX</tt>.
	 */
	@Override
	public String getSailType()	{
		return SAIL_TYPE;
	}

}
