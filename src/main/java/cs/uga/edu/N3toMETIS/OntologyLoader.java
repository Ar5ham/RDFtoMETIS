package cs.uga.edu.N3toMETIS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Locator;

import com.hp.hpl.jena.rdf.arp.ALiteral;
import com.hp.hpl.jena.rdf.arp.ARP;
import com.hp.hpl.jena.rdf.arp.ARPErrorNumbers;
import com.hp.hpl.jena.rdf.arp.AResource;
import com.hp.hpl.jena.rdf.arp.NamespaceHandler;
import com.hp.hpl.jena.rdf.arp.StatementHandler;


public class OntologyLoader implements ARPErrorNumbers   {
	
	private static ARP arp;
    private static String xmlBase = null;
    private String status;
    
    
    public OntologyLoader() {
        status = null;
    }
	 
    /************************************************************************
     * Loads an ontology schema from File or URL
     * @param  url  the URL location to the ontology file
     * @param  errorHandler  the error handler
     */
    public void load(String url) throws IOException {
        arp = new ARP();
        arp.getHandlers().setStatementHandler(new EntityStatementHandler()); 
        process(url);
    }
    
    /************************************************************************
     * The actual process of reading the rdf-ontology
     * @param  surl  the url
     */
    private void process(String surl) throws IOException {
    	
    }
	 
    
    /************************************************************************
     * The actual process of reading the rdf-ontology, using ARP
     * @param  surl  the url
     */
    private void process(InputStream in, String xmlBasex, String surl) {
    	
    }
    
    
    
    /************************************************************************
     * Return the status of the model.
     * Error(s) in parsing and/or logic of the ontology schema and/or instances
     * @return  the text description of the status
     */
    public String getStatus() {
        return status;
    }
    
    
    /************************************************************************
     * Line numbers of arp parser current location
     * @return  the line number details, otherwise, empty string
     */
    private String lineNumber() {
        String ret = "";
        Locator locator = arp.getLocator();
        if (locator != null) {
            ret = "line: " + locator.getLineNumber() + ", column: " + locator.getColumnNumber();
        }
        return ret;
    }
    
    
    public class EntityStatementHandler implements StatementHandler {
    	
    	
    	 private Map<String, String> anonIDs;
         private final String type;
         private final String func;
         private final String imports; 
         
         private final String[] classes = new String[]{
             "http://www.w3.org/2000/01/rdf-schema#Class",
             "http://www.w3.org/2002/07/owl#Class"
         };
         private final String[] props = new String[]{
             "http://www.w3.org/2002/07/owl#DatatypeProperty",
             "http://www.w3.org/2002/07/owl#AnnotationProperty",
             "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"
         };
         private final String[] relations = new String[]{
             "http://www.w3.org/2002/07/owl#ObjectProperty"
         };
         
         // where does "http://www.w3.org/2002/07/owl#imports"

         public EntityStatementHandler() {
        	 
             anonIDs = new HashMap<String, String>();
             type 	 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
             func 	 = "http://www.w3.org/2002/07/owl#FunctionalProperty";
             imports = "http://www.w3.org/2002/07/owl#imports" ; 
         }


        /***************************************************************************
        * Processes an (RDF or OWL) statement,
        * it handles the following: (X ---rdf:type--> rdf:Class),
        *                           (X ---rdfs:subClassOf--> Y),
        *                           (X ---rdf:type--> owl:ObjectProperty),
        *                           (X ---rdf:type--> owl:DatatypeProperty),
        *                           (X ---rdfs:domain --> Y)
        * @param  subject  	 the subject
        * @param  predicate  the predicate
        * @param  object  	 the object
        */
        @Override
        public void statement(AResource subject, AResource predicate, AResource object) {
        	
        	String sub  = subject.isAnonymous() ? subject.getAnonymousID() : subject.getURI(); 
        	String pred = predicate.isAnonymous()? predicate.getAnonymousID() : predicate.getURI();
        	String obj  = object.isAnonymous()? object.getAnonymousID() : object.getURI();
        	
        	
        	
         }

		@Override
		public void statement(AResource subj, AResource pred, ALiteral lit) {
			// TODO Auto-generated method stub
			
		}

		
    }

}
