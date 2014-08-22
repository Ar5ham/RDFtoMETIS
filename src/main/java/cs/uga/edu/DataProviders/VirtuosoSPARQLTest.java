package cs.uga.edu.DataProviders;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javafarm.demo.Sysout;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;

import cs.uga.edu.util.VirtuosoJenaDAO;
/**
 * Executes a SPARQL query against a virtuoso url and prints results.
 */
public class VirtuosoSPARQLTest extends VirtuosoJenaDAO  {
	private static final boolean DEBUG = true;
	private InfModel infModel; 
	private VirtGraph vg ;
	private QueryExecution vqe ; 


	public VirtuosoSPARQLTest() {
		List<String> queries = readQueries("Queries.json"); 
		
		//infModel = getInfModel("inft", "http://www.cs.uga.edu#"); 
		
		infModel = getInfModel("inft", "http://www.cs.uga.edu#");
		
		for(int i = 0; i < queries.size(); i++)
		{	
			String q = queries.get(i); 
			vqe =  VirtuosoQueryExecutionFactory.create(q,infModel) ; 
			if(DEBUG)
			{
				System.out.println(queries.get(i));
			}
			try {
				ExecuteQueries(); 
			} catch (Exception e) {
				System.err.println("Could not execute the query " + q);
			}
			
		}
	}


	public void ExecuteQueries() throws Exception
	{
		long startTime = System.nanoTime();
		ResultSet rs = vqe.execSelect();
		long estimatedTime = System.nanoTime() - startTime;
		
		if(!rs.hasNext())
		{
			throw new Exception("No result foud for the query!"); 
		}
		
		if(DEBUG)
		{
			
			System.out.print("Time elapsed: " + estimatedTime + "ns , " + estimatedTime /1000000000.0 + " sec, " );
		}
		
		int count = 0;
		while (rs.hasNext()) {
			QuerySolution result = rs.nextSolution();
			count ++;
			
			
			//TODO: Do some validation?! 
//			List<String> rsv = rs.getResultVars(); 
			//System.out.println(result.toString()); 
		}
		if(DEBUG)
		{
			System.out.println("Triple Count: " + count);
			
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
		
		try {
			new VirtuosoSPARQLTest();
		} catch (Exception e) {
			e.printStackTrace();
		} 

//		String url;
//		if(args.length == 0)
//			url = "jdbc:virtuoso://localhost:1111";
//		else
//			url = args[0];
//
//		//		VirtGraph vrt = new VirtGraph("http://www.cs.uga.edu#",VirtuosoJenaDAO.url ,VirtuosoJenaDAO.user, VirtuosoJenaDAO.pass ) ; 
//
//		VirtInfGraph infGraph  = new VirtInfGraph("inft", false, "http://www.cs.uga.edu#",VirtuosoJenaDAO.url ,VirtuosoJenaDAO.user, VirtuosoJenaDAO.pass ); 
//		InfModel model = ModelFactory.createInfModel(infGraph);
//
//		String queryString = "prefix ub: <http://cs.uga.edu#> select * FROM <http://www.cs.uga.edu#> WHERE{ ?s ?p ?o} limit 10"; 
//
//		QueryExecution qexec =  VirtuosoQueryExecutionFactory.create(queryString, model) ;
//
//		try {
//			ResultSet rs = qexec.execSelect() ;
//			for ( ; rs.hasNext() ; ) {
//				QuerySolution result = rs.nextSolution();
//				RDFNode s = result.get("s");
//				RDFNode p = result.get("p");
//				RDFNode o = result.get("o");
//				System.out.println("<" + s + ">  <" + p + ">  <" + o + "> . ");
//			}
//		} finally {
//			qexec.close() ;
//		}
	}





}
