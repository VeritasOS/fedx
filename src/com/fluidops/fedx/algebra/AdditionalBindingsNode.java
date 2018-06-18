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
package com.fluidops.fedx.algebra;

import java.util.List;

import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;


/**
 * Convenience AST node to print the additional values
 * of a {@link ProjectionWithBindings} node
 *  
 * @author Andreas Schwarte
 *
 */
public class AdditionalBindingsNode extends AbstractQueryModelNode
{
	private static final long serialVersionUID = -5218276034414711890L;

	private final List<Binding> additionalValues;

	public AdditionalBindingsNode(List<Binding> additionalValues) {
		super();
		this.additionalValues = additionalValues;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}	

	@Override
	public String getSignature()
	{
		StringBuilder sb = new StringBuilder(64);
		sb.append("Additional Bindings (");
		for (int i=0; i<additionalValues.size();i++) {
			sb.append(additionalValues.get(i).getName() + "=" + additionalValues.get(i).getValue().stringValue());
			if (i<additionalValues.size()-1)
				sb.append(", ");
		}
		sb.append(")");		
		return sb.toString();		
	}
	
	public static <X extends Exception> void visit(QueryModelVisitor<X> visitor, List<Binding> additionalValues) throws X {
		new AdditionalBindingsNode(additionalValues).visit(visitor);
	}
}
