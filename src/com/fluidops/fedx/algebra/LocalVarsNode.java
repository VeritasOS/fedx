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

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;


/**
 * Convenience AST node to print the local variables of
 * {@link StatementTupleExpr} instances
 * 
 * @author Andreas Schwarte
 *
 */
public class LocalVarsNode extends AbstractQueryModelNode
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -757075347491900484L;
	private final List<String> localVars;

	public LocalVarsNode(List<String> localVars) {
		super();
		this.localVars = localVars;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}	

	@Override
	public String getSignature()
	{
		StringBuilder sb = new StringBuilder(64);
		sb.append("LocalVars (");
		for (int i=0; i<localVars.size();i++) {
			sb.append(localVars.get(i));
			if (i<localVars.size()-1)
				sb.append(", ");
		}
		sb.append(")");		
		return sb.toString();		
	}
	
	public static <X extends Exception> void visit(QueryModelVisitor<X> visitor, List<String> localVars) throws X {
		new LocalVarsNode(localVars).visit(visitor);
	}
}
