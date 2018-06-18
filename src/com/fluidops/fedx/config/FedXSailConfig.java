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

import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;


/**
 * FedX Sail config to be used for repository initialization using
 * the {@link FedXSailFactory}.
 * 
 * @author Andreas Schwarte
 */
public class FedXSailConfig extends AbstractSailImplConfig
{

	/** the location of the fedx configuration */
	private String fedxConfig;
	
	
	public FedXSailConfig() {
		super(FedXSailFactory.SAIL_TYPE);
	}
	
	public FedXSailConfig(String fedxConfig) {
		this();
		this.fedxConfig = fedxConfig;
	}

	/**
	 * @return
	 * 		the location of the FedX configuration
	 */
	public String getFedxConfig() {
		return fedxConfig;
	}

	/**
	 * Set the location of the FedX configuration
	 */
	public void setFedxConfig(String fedxConfig) {
		this.fedxConfig = fedxConfig;
	}	
	
}
