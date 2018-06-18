package local;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SparqlDemo2
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		SPARQLRepository rep = new SPARQLRepository("http://10.212.10.29:8099/bigdata/namespace/worldbank_270a_info_data_world-development-indicators_NY_GDP_MKTP_CD/sparql");
		rep.initialize();
		
//		String queryString = "SELECT * WHERE { ?x ?y ?z } LIMIT 100";
		String queryString = "PREFIX wbcountry: <http://worldbank.270a.info/classification/country/> " +
						"PREFIX wbindicator: <http://worldbank.270a.info/classification/indicator/> " +
						"PREFIX wbp: <http://worldbank.270a.info/property/> " +
						"PREFIX sd: <http://purl.org/linked-data/sdmx/2009/dimension#> " +
						"PREFIX sm: <http://purl.org/linked-data/sdmx/2009/measure#> " +
						"SELECT ?degdp ?cngdp ?year  WHERE {" +
						"    ?obs wbp:indicator wbindicator:NY.GDP.MKTP.CD ;" +
						"        sd:refArea wbcountry:DE ; " +
						"        sm:obsValue ?degdp ; " +
						"        sd:refPeriod ?year . " +
						"  FILTER(str(?year) = \"http://reference.data.gov.uk/id/year/2000\") " +
						" }";
								    
		TupleQuery query = rep.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		TupleQueryResult result = query.evaluate();
		
		while (result.hasNext()) {
			System.out.println(result.next());
		}
		
		System.out.println("Done");
		System.exit(0);
	}

}
