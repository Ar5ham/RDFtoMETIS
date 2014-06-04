package cs.uga.edu.DataProviders;

import virtuoso.jena.driver.VirtGraph;

public class VirtuosoSPARQLTest {

	/**
	 * Executes a SPARQL query against a virtuoso url and prints results.
	 */
	public static void main(String[] args) {
		
		String url;
		if(args.length == 0)
		    url = "jdbc:virtuoso://localhost:1111";
		else
		    url = args[0];
		
		/*			STEP 1			*/
		VirtGraph graph = new VirtGraph ("http://www.cs.uga.edu#", url, "dba", "dba");
		
		
		

	}

}
