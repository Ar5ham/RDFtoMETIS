package cs.uga.edu.DataProviders;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

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

		/*			STEP 1			
		 * VirtGraph(String graphName, String _url_hostlist, String user, String password) */
		VirtGraph graph = new VirtGraph ("http://www.cs.uga.edu#", url, "dba", "dba");

		/*			STEP 2			*/


		/*			STEP 3			*/
		/*		Select all data in virtuoso	*/
		Query sparql = QueryFactory.create("SELECT * FROM <" + "http://www.cs.uga.edu#>" + " WHERE { GRAPH ?graph { ?s ?p ?o } } limit 100");

		/*			STEP 4			*/
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, graph);
		
		ResultSet results = vqe.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
		    RDFNode gr = result.get("graph");
		    RDFNode s = result.get("s");
		    RDFNode p = result.get("p");
		    RDFNode o = result.get("o");
		    System.out.println(graph + " { " + s + " " + p + " " + o + " . }");
		}

	}

}
