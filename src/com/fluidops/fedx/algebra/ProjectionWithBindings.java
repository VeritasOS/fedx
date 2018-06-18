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
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

public class ProjectionWithBindings extends Projection {

	private static final long serialVersionUID = -1395931366720892447L;

	private final List<Binding> additionalBindings;
	
	public ProjectionWithBindings(TupleExpr arg, ProjectionElemList elements, List<Binding> additionalBindings) {
		super(arg, elements);
		this.additionalBindings = additionalBindings;
	}
	
	public List<Binding> getAdditionalBindings() {
		return this.additionalBindings;
	}
	
	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X {
		if (additionalBindings.size()>0)
			AdditionalBindingsNode.visit(visitor, additionalBindings);
		super.visitChildren(visitor);		
	}
}
