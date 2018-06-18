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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import com.fluidops.fedx.util.FedXUtil;

/**
 * Defines constants for the FedX schema which is used by {@link FedXSailFactory}
 * to initialize FedX federations.
 * 
 * @author Andreas Schwarte
 */
public class FedXSailSchema {

	
	/** FedX schema namespace (<tt>http://www.fluidops.com/config/fedx#</tt>). */
	public static final String NAMESPACE = "http://www.fluidops.com/config/fedx#";
	
	/** <tt>http://www.fluidops.com/config/fedx#fedxConfig</tt>	 */
	public final static IRI fedXConfig;
	
	static {
		ValueFactory factory = FedXUtil.valueFactory();
		fedXConfig = factory.createIRI(NAMESPACE, "fedxConfig");
	}
}
