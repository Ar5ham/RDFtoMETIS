package cs.uga.edu.N3toMETIS;

/**
 * File: OntologyLoader.java
 */
//import ontology.io.model.ModelIndex;
import com.hp.hpl.jena.rdf.arp.ALiteral;
import com.hp.hpl.jena.rdf.arp.AResource;
import com.hp.hpl.jena.rdf.arp.ARP;
import com.hp.hpl.jena.rdf.arp.ARPErrorNumbers;
import com.hp.hpl.jena.rdf.arp.ParseException;
import com.hp.hpl.jena.rdf.arp.StatementHandler;
import com.hp.hpl.jena.rdf.arp.NamespaceHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import ontology.io.model.ModelAttribute;
//import ontology.io.model.ModelEntity;
//import ontology.io.model.ModelUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * OntologyLoader
 *
 *
 */
public class Matt_OntologyLoader implements ARPErrorNumbers {

    private static ARP arp;
    private static String xmlBase = null;
    // status: null if no errors, otherwise, it contains error description
    private String status;
//    private final ModelIndex index;
    
    //private final Map<Integer, Map<Integer, Set<Integer>>> spoIndex;
    //private final Map<Integer, Map<Integer, Set<Integer>>> posIndex;
    //private final Map<Integer, Map<Integer, Set<Integer>>> opsIndex;
    
    private final List<String> resources;

    /**
     * Default constructor
     */
    public Matt_OntologyLoader() {
        status = null;
        resources = new ArrayList<String>();
//        index = new ModelIndex();
        //spoIndex = new HashMap<Integer, Map<Integer, Set<Integer>>>();
        //posIndex = new HashMap<Integer, Map<Integer, Set<Integer>>>();
        //opsIndex = new HashMap<Integer, Map<Integer, Set<Integer>>>();
    }
    
    /*public Set<String> getProperties(){
        int type = resources.indexOf(ModelIndex.unPrefixURI("rdf:type"));
        int rdfProp = resources.indexOf(ModelIndex.unPrefixURI("rdf:Property"));
        int objProp = resources.indexOf(ModelIndex.unPrefixURI("owl:ObjectProperty"));
        int dataProp = resources.indexOf(ModelIndex.unPrefixURI("owl:DataProperty"));
        int funcProp = resources.indexOf(ModelIndex.unPrefixURI("owl:FunctionalProperty"));
        Set<String> results = new HashSet<String>();
        if(rdfProp > -1){
            for(Integer i : posIndex.get(type).get(rdfProp)){
                results.add(resources.get(i));
            }
        }
        if(objProp > -1){
            for(Integer i : posIndex.get(type).get(objProp)){
                results.add(resources.get(i));
            }
        }
        if(dataProp > -1){
            for(Integer i : posIndex.get(type).get(dataProp)){
                results.add(resources.get(i));
            }
        }
        if(funcProp > -1){
            for(Integer i : posIndex.get(type).get(funcProp)){
                results.add(resources.get(i));
            }
        }
        return results;
    }*/

    /**
     * Loads an ontology schema from File or URL
     * @param  url  the URL location to the ontology file
     * @param  errorHandler  the error handler
     */
    public void load(String url) throws IOException {
        arp = new ARP();
        arp.getHandlers().setStatementHandler(new EntityStatementHandler());
        arp.getHandlers().setNamespaceHandler(new ModelNamespaceHandler());
        process(url);
        arp.getHandlers().setStatementHandler(new RelationStatementHandler());
        process(url);
    }

    /**
     * The actual process of reading the rdf-ontology
     * @param  surl  the url
     */
    private void process(String surl) throws IOException {
        InputStream in = null;
        URL url = null;
        try {
            // try opening it as file
            File ff = new File(surl);
            in = new FileInputStream(ff);
            url = ff.toURI().toURL();
        } catch (IOException ignore) {
            // try open it as url
            try {
                url = new URL(surl);
                in = url.openStream();
            } catch (IOException e) {
                System.err.println("ARP: Failed to open: " + surl);
                System.err.println("    " + ParseException.formatMessage(ignore));
                System.err.println("    " + ParseException.formatMessage(e));
                throw new IOException();
            }
        }
        process(in, url.toExternalForm(), surl);
    }

    /*
     * The actual process of reading the rdf-ontology, using ARP
     * @param  surl  the url
     */
    private void process(InputStream in, String xmlBasex, String surl) {
        String xmlBasey = xmlBase == null ? xmlBasex : xmlBase;
        String errorMessage = null;
        try {
            arp.load(in, xmlBasey);
        } catch (IOException e) {
            errorMessage = "(IO) Exception: " + surl + ": " + ParseException.formatMessage(e);
            status = status == null ? errorMessage : status;
            System.err.println(errorMessage);
        } catch (SAXParseException e) {
            // already reported.
        } catch (SAXException sax) {
            errorMessage = "(SAX) Exception: " + surl + ": " + ParseException.formatMessage(sax);
            status = status == null ? errorMessage : status;
            System.err.println(errorMessage);
        }

    }

    /**
     * Return the status of the model.
     * Error(s) in parsing and/or logic of the ontology schema and/or instances
     * @return  the text description of the status
     */
    public String getStatus() {
        return status;
    }

    /*
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

    private class ModelNamespaceHandler implements NamespaceHandler {

        public void endPrefixMapping(String prefix){
            //System.out.println("PREFIX " + prefix + " IS GOING OUT OF SCOPE");
        }

        public void startPrefixMapping(String prefix, String uri){
            ModelIndex.addPrefixMapping(prefix, uri);
        }
    }

    public class EntityStatementHandler implements StatementHandler {

        private Map<String, String> anonIDs;
        private final String type;
        private final String func;
        
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

        public EntityStatementHandler() {
            //patterns = new PatternActivator("patterns.txt", "types.txt", this);
            anonIDs = new HashMap<String, String>();
            type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
            func = "http://www.w3.org/2002/07/owl#FunctionalProperty";
        }

        /**
         * Processes an (RDF or OWL) statement,
         * it handles the following: (X ---rdf:type--> rdf:Class),
         *                           (X ---rdfs:subClassOf--> Y),
         *                           (X ---rdf:type--> owl:ObjectProperty),
         *                           (X ---rdf:type--> owl:DatatypeProperty),
         *                           (X ---rdfs:domain --> Y)
         * @param  subject  the subject
         * @param  predicate  the predicate
         * @param  object  the object
         */
        public void statement(AResource subject, AResource predicate, AResource object) {

            String sub = subject.isAnonymous()? subject.getAnonymousID() : subject.getURI();
            String pred = predicate.isAnonymous()? predicate.getAnonymousID() : predicate.getURI();
            String obj = object.isAnonymous()? object.getAnonymousID() : object.getURI();
            
            if(!subject.isAnonymous()){
                if(pred.equals(type)){
                    String ns = ModelUtils.parseNamespace(sub);
                    if(!ns.equals("http://www.w3.org/2002/07/owl") &&
                       !ns.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns")){
                        if(equalsOne(classes, obj)){
                            ModelIndex.addEntity(sub);
                        }else if(equalsOne(props, obj)){
                            System.out.println("ATTRIBUTE: "+sub + " -- "+obj);
                            ModelIndex.addAttribute(sub);
                        }
                    }
                    
                }
            }
            //System.out.println(sub+" --"+pred+"--> "+obj);

            //patterns.activatePatterns(subUri, predUri, objUri);
        }

        /**
         * Processes an (RDF or OWL) statement involving a literal
         * @param  subject  the subject
         * @param  predicate  the predicate
         * @param  literal the literal
         */
        public void statement(AResource subject, AResource predicate, ALiteral literal) {
            
        }

    }
    
    public class RelationStatementHandler implements StatementHandler {

        private Map<String, String> anonIDs;
        private final String subclass;
        private final String domain;
        private final String range;
        private final String thing;
        private final String label;
        
        //private final Map<String, String> domainMap;
        //private final Map<String, String> rangeMap;

        public RelationStatementHandler() {
            //patterns = new PatternActivator("patterns.txt", "types.txt", this);
            anonIDs = new HashMap<String, String>();
            subclass = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
            domain = "http://www.w3.org/2000/01/rdf-schema#domain";
            range = "http://www.w3.org/2000/01/rdf-schema#range";
            thing = "http://www.w3.org/2002/07/owl#Thing";
            label = "http://www.w3.org/2000/01/rdf-schema#label";
            
            //domainMap = new HashMap<String, String>();
            //rangeMap = new HashMap<String, String>();
        }

        /**
         * Processes an (RDF or OWL) statement,
         * it handles the following: (X ---rdf:type--> rdf:Class),
         *                           (X ---rdfs:subClassOf--> Y),
         *                           (X ---rdf:type--> owl:ObjectProperty),
         *                           (X ---rdf:type--> owl:DatatypeProperty),
         *                           (X ---rdfs:domain --> Y)
         * @param  subject  the subject
         * @param  predicate  the predicate
         * @param  object  the object
         */
        public void statement(AResource subject, AResource predicate, AResource object) {

            String sub = subject.isAnonymous()? subject.getAnonymousID() : subject.getURI();
            String pred = predicate.isAnonymous()? predicate.getAnonymousID() : predicate.getURI();
            String obj = object.isAnonymous()? object.getAnonymousID() : object.getURI();
            
            if(!subject.isAnonymous() && !object.isAnonymous()){
                if(pred.equals(subclass)){
                    //String childName = ModelEntity.sanitize(ModelUtils.parseLocalName(sub));
                    //String parentName = ModelEntity.sanitize(ModelUtils.parseLocalName(obj));
                    //ModelEntity child = ModelIndex.getEntity(childName);
                    //ModelEntity parent = ModelIndex.getEntity(parentName);
                    
                    ModelEntity child = ModelIndex.getEntity(sub);
                    ModelEntity parent = ModelIndex.getEntity(obj);
                    if(child != null && parent != null)
                        child.addParent(parent);
                }else if(pred.equals(domain)){
                    System.out.println("DOMAIN: "+sub+" -- "+obj);
                    ModelAttribute attribute = ModelIndex.getAttribute(sub);
                    if(attribute != null){
                        if(obj.equals(thing)){
                            for(ModelEntity entity: ModelIndex.getEntities()){
                                entity.addAttribute(attribute);
                            }
                        }else{
                            ModelEntity entity = ModelIndex.getEntity(obj);
                            entity.addAttribute(attribute);
                        }
                    }
                }else if(pred.equals(range)){
                    System.out.println("RANGE: "+sub+" -- "+obj);
                    ModelAttribute attribute = ModelIndex.getAttribute(sub);
                    if(attribute != null){
                        String localName = ModelUtils.parseLocalName(obj).toUpperCase();
                        try{
                            //System.out.println("    SETTING TYPE TO "+localName);
                            attribute.setType(ModelAttribute.AttributeType.valueOf(localName));
                        }catch(IllegalArgumentException iae){
                            System.out.println(sub+" is being removed.");
                            ModelIndex.removeAttribute(sub);
                        }
                    }
                }
                
            }
            
        }

        /**
         * Processes an (RDF or OWL) statement involving a literal
         * @param  subject  the subject
         * @param  predicate  the predicate
         * @param  literal the literal
         */
        public void statement(AResource subject, AResource predicate, ALiteral literal) {
            
            String sub = subject.isAnonymous()? subject.getAnonymousID() : subject.getURI();
            String pred = predicate.isAnonymous()? predicate.getAnonymousID() : predicate.getURI();
            String obj = literal.toString().replaceAll(" ", "_");
            
            if(pred.equals(label)){
                System.out.println("LABEL: "+sub+" -- "+obj);
                ModelAttribute attribute = ModelIndex.getAttribute(sub);
                if(attribute != null){
                    attribute.setLabel(obj);
                }else{
                    ModelEntity entity = ModelIndex.getEntity(sub);
                    if(entity != null){
                        entity.setLabel(obj);
                    }else System.out.println("ATTRIBUTE OR ENTITY NOT FOUND: "+sub);
                }
            }
        }

    }
    
    private boolean equalsOne(String[] options, String s){
        boolean b = false;
        for(String option : options){
            b = b || s.equals(option);
        }
        return b;
    }
    
    /*public class ClassStatementHandler implements StatementHandler {

        private Map<String, String> anonIDs;
        //private PatternActivator patterns;

        public ClassStatementHandler() {
            //patterns = new PatternActivator("patterns.txt", "types.txt", this);
            anonIDs = new HashMap<String, String>();
        }

        public void statement(AResource subject, AResource predicate, AResource object) {

            String sub = subject.isAnonymous()? subject.getAnonymousID() : subject.getURI();
            String pred = predicate.isAnonymous()? predicate.getAnonymousID() : predicate.getURI();
            String obj = object.isAnonymous()? object.getAnonymousID() : object.getURI();
            
            int sIndex = resources.indexOf(sub);
            int pIndex = resources.indexOf(pred);
            int oIndex = resources.indexOf(obj);
            
            if(sIndex == -1){
                resources.add(sub);
                sIndex = resources.indexOf(sub);
            }
            if(pIndex == -1){
                resources.add(pred);
                pIndex = resources.indexOf(pred);
            }
            if(oIndex == -1){
                resources.add(obj);
                oIndex = resources.indexOf(obj);
            }
            
            Map<Integer, Set<Integer>> po = spoIndex.get(sIndex);
            Map<Integer, Set<Integer>> os = posIndex.get(pIndex);
            Map<Integer, Set<Integer>> ps = opsIndex.get(oIndex);
            
            po = po == null? new HashMap<Integer, Set<Integer>>() : po;
            os = os == null? new HashMap<Integer, Set<Integer>>() : os;
            ps = ps == null? new HashMap<Integer, Set<Integer>>() : ps;
            
            Set<Integer> poSet = po.get(pIndex);
            Set<Integer> osSet = os.get(oIndex);
            Set<Integer> psSet = ps.get(pIndex);
            
            if(poSet == null)
                poSet = new HashSet<Integer>();
            poSet.add(oIndex);
            po.put(pIndex, poSet);
            spoIndex.put(sIndex, po);
            
            if(osSet == null)
                osSet = new HashSet<Integer>();
            osSet.add(sIndex);
            os.put(oIndex, osSet);
            posIndex.put(pIndex, os);
                
            if(psSet == null)
                psSet = new HashSet<Integer>();
            psSet.add(sIndex);
            ps.put(pIndex, psSet);
            opsIndex.put(oIndex, ps);
            
            
            System.out.println(sub+" --"+pred+"--> "+obj);

            //patterns.activatePatterns(subUri, predUri, objUri);
        }
    
        public void statement(AResource subject, AResource predicate, ALiteral literal) {
            
        }

    }*/

}

