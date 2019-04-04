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
package com.fluidops.fedx.endpoint;

import com.fluidops.fedx.provider.RepositoryInformation;

public abstract class EndpointBase extends com.fluidops.fedx.structures.Endpoint implements Endpoint {

	public EndpointBase(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification) {
		super(repoInfo, endpoint, endpointClassification);
	}

	// TODO refactor com.fluidops.fedx.structures.Endpoint into this class
}
