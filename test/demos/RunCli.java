package demos;

import com.fluidops.fedx.CLI;

public class RunCli {

	
	public static void main(String[] args) {
		
//		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//			+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
//			+ "SELECT ?President ?Party WHERE {\n"
//			+ "?President rdf:type <http://dbpedia.org/class/yago/PresidentsOfTheUnitedStates> .\n"
//			+ "?President dbpedia-owl:party <http://dbpedia.org/resource/Democratic_Party_%28United_States%29> . }";
		
//		String dataConfig = "local/dataSourceConfig.ttl";
		
		//CLI.main(new String[] {"-verbose", "2", "-s", "http://dbpedia.org/sparql", "-s", "http://api.talis.com/stores/nytimes/services/sparql", "-q", q} );
	
		//"-verbose", "2",
		CLI.main( new String[] {  "-d", "examples\\DBPediaDrugbankKEGG.ttl", "@q", "examples\\q3.txt"} );
	}
}
