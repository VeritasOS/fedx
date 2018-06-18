/*
 * Copyright (C) 2008-2011, fluid Operations AG
 *
 * FedX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package local;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.fluidops.fedx.util.FedXUtil;

public class TestSPARQLConcat
{

	public static void main(String[] args) throws Exception {
		
		MemoryStore store = new MemoryStore();
		store.initialize();
		
		SailRepository repo = new SailRepository(store);
		RepositoryConnection conn = repo.getConnection();
		
		ValueFactory vf = FedXUtil.valueFactory();
		String ns = "http://example.org/";
		conn.add(vf.createStatement(vf.createIRI(ns, "str1"), vf.createIRI(ns, "str"), vf.createLiteral("Hello")));
		conn.add(vf.createStatement(vf.createIRI(ns, "str2"), vf.createIRI(ns, "str"), vf.createLiteral("World")));
		conn.commit();
		
		String query = "PREFIX : <http://example.org/> " +
				"SELECT (CONCAT(?str1, ?str2) as ?str) WHERE { " +
				":str1 ?p ?str1 . " +
				":str2 ?p ?str2 . }";
		TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		
		TupleQueryResult res = q.evaluate();
		
		while (res.hasNext()) {
			System.out.println(res.next());
		}
		
		System.out.println("Done.");
		System.exit(0);
	}
}
