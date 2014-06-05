package cs.uga.edu.DataProviders;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javafarm.demo.Sysout;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import cs.uga.edu.util.VirtuosoJenaDAO;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
/**
 * Executes a SPARQL query against a virtuoso url and prints results.
 */
public class VirtuosoSPARQLTest  {
	private VirtGraph set ; 
	private VirtGraph vg ;
	private VirtuosoQueryExecution vqe ; 

	
	public VirtuosoSPARQLTest() {

		//VirtGraph set ; 
		List<String> queries = readQueries("Queries.json"); 
		set = new VirtGraph (VirtuosoJenaDAO.url, VirtuosoJenaDAO.user, VirtuosoJenaDAO.pass);
		
		for(int i = 0; i < queries.size(); i++)
		{	
			vqe = VirtuosoQueryExecutionFactory.create(QueryFactory.create(queries.get(i)),set) ; 
			ExecuteQueries(); 
		}
	}


	public void ExecuteQueries()
	{

		ResultSet rs = vqe.execSelect();
		while (rs.hasNext()) {
			QuerySolution result = rs.nextSolution();

			//TODO: Do some validation?! 
			System.out.println(result.toString()); 
			
			//			RDFNode s = result.get("s");
			//			RDFNode p = result.get("p");
			//			RDFNode o = result.get("o");
			//			System.out.println(s + " " + p + " " + o + " . ");
		}

	}

	public List<String> readQueries(String path)
	{
		List<String> qs  = new ArrayList<>();  
		try {
			JsonReader reader = new JsonReader(new FileReader(path));
			Gson gson = new Gson();

			reader.beginArray();
			while(reader.hasNext())
			{
				qs.add(reader.nextString()); 
			}
			reader.endArray();

		} catch (Exception e) {
			System.err.println("Can't read the file!" + e.getMessage());
		}
		return qs; 
	}


	public static void main(String[] args) {
		new VirtuosoSPARQLTest().ExecuteQueries(); 

		/*String url;
		if(args.length == 0)
			url = "jdbc:virtuoso://128.192.62.244:1111";
		else
			url = args[0];

					STEP 1			
		VirtGraph set = new VirtGraph (url, "dba", "dba");

					STEP 2			


					STEP 3			
				Select all data in virtuoso	
		Query sparql = QueryFactory.create("SELECT * FROM <http://www.cs.uga.edu#> WHERE {?s ?p ?o } limit 100");

					STEP 4			
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, set);

		ResultSet results = vqe.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.nextSolution();
//			RDFNode graph = result.get("graph");
			RDFNode s = result.get("s");
			RDFNode p = result.get("p");
			RDFNode o = result.get("o");
			System.out.println(s + " " + p + " " + o + " . ");
		}*/
	}





}
