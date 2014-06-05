package cs.uga.edu.util;

import virtuoso.jena.driver.VirtGraph;

public class VirtuosoJenaDAO {
	
	public  static final String url = "jdbc:virtuoso://localhost:1111"; 
	public static final String user = "dba"; 
	public static final String pass = "dba";  
	
	
	/*private String url ;
	private static final String user = "dba"; 
	private static final String pass = "dba"; 
	public VirtuosoJenaDAO() {
		this.url = "jdbc:virtuoso://localhost:1111";
	}

	public VirtuosoJenaDAO(String url) {
		this.url = url; 
	}

	public VirtGraph getVirtGraph() {
		new VirtuosoJenaDAO(); 
		try {
			return new VirtGraph (url, "dba", "dba");
		} catch (Exception e) {
			System.err.println("Could not connect to the database!\n" + e.getMessage() );
		}

		return null; 
	}

	public VirtGraph getVirtGraph(String url) {
		new VirtuosoJenaDAO(url); 
		try {
			return new VirtGraph (url, "dba", "dba");
		} catch (Exception e) {
			System.err.println("Could not connect to the database!\n" + e.getMessage() );
		}

		return null; 
	}*/

}
