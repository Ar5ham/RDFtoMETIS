package cs.uga.edu.util;

import org.miv.util.InvalidArgumentException;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtInfGraph;

public class VirtuosoJenaDAO {
	
//	public  static final String url = "jdbc:virtuoso://localhost:1111"; 
//	public static final String user = "dba"; 
//	public static final String pass = "dba";  
	
	
	private String url ;
	private final String user = "dba"; 
	private final String pass = "dba";
	private String ruleSet; 
	private String graphName; 
	
	
	/**
	 * 
	 */
	public VirtuosoJenaDAO() {
		this.url = "jdbc:virtuoso://localhost:1111";
	}

	/**
	 * @param url
	 */
	public VirtuosoJenaDAO(String url) {
		this.url = url; 
	}

	/**
	 * @return
	 */
	public VirtGraph getVirtGraph() {
		new VirtuosoJenaDAO(); 
		try {
			return new VirtGraph (url, "dba", "dba");
		} catch (Exception e) {
			System.err.println("Could not connect to the database!\n" + e.getMessage() );
		}

		return null; 
	}

	/**
	 * @param url
	 * @return
	 */
	public VirtGraph getVirtGraph(String url) {
		new VirtuosoJenaDAO(url); 
		try {
			return new VirtGraph (url, "dba", "dba");
		} catch (Exception e) {
			System.err.println("Could not connect to the database!\n" + e.getMessage() );
		}

		return null; 
	}
	
	/**
	 * @param _ruleSet
	 * @param useSameAs
	 * @param graphName
	 * @param url_hostlist
	 * @return
	 */
	public InfModel getInfModel(String _ruleSet, String _graphName)
	{
		new VirtuosoJenaDAO(); 
		
		if(_ruleSet == null || _ruleSet.length() == 0)
			throw new InvalidArgumentException("ruleSet argument can't be null or emptt string."); 
		this.ruleSet = _ruleSet; 
		
		if(_graphName == null || _graphName.length() == 0)
			throw new InvalidArgumentException("ruleSet argument can't be null or emptt string."); 
		this.graphName = _graphName; 
		
		VirtInfGraph infGraph  = new VirtInfGraph(this.ruleSet, false, this.graphName,this.url ,this.user, this.pass ); 
		InfModel model = ModelFactory.createInfModel(infGraph);
		
		return model;
	}
	
	

}
