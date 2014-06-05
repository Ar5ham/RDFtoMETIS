package cs.uga.edu.DataProviders;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

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

public class VirtuosoSPARQLTest  {

	private VirtGraph vg ;
	private VirtuosoQueryExecution []  vqe ; 

	/**
	 * Executes a SPARQL query against a virtuoso url and prints results.
	 */
	public VirtuosoSPARQLTest() {

		VirtGraph set ; 
		List<String> queries = readQueries("Queries.json"); 
		set = new VirtGraph (VirtuosoJenaDAO.url, VirtuosoJenaDAO.user, VirtuosoJenaDAO.pass);
		for(int i = 0; i < queries.size(); i++)
		{	
			vqe [i] = VirtuosoQueryExecutionFactory.create(QueryFactory.create(queries.get(i)),set) ; 
		}
	}
	
	
	public void ExecuteQueries()
	{
		/*for(int i = 0 ; i < vqe.length; i++)
		{
			
		}*/
		
		ResultSet rs = vqe[0].execSelect();
		while (rs.hasNext()) {
			QuerySolution result = rs.nextSolution();
		   
		    RDFNode s = result.get("s");
		    RDFNode p = result.get("p");
		    RDFNode o = result.get("o");
		    System.out.println(s + " " + p + " " + o + " . ");
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

	}



}
