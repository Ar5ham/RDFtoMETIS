/**
 * 
 */
package cs.uga.edu.Graph;


import grph.Grph;
import grph.in_memory.InMemoryGrph;
import grph.properties.ObjectProperty;
import grph.properties.Property;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javafarm.demo.Sysout;
import book.set.Hash;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

/**
 * @author Arssham
 *
 */
public class SPARQLInMemoryGraph extends InMemoryGrph{
	
	static Logger logger = Logger.getLogger(SPARQLInMemoryGraph.class);

	/***************************************************************
	 * @param query: String representation of SPARQL query
	 * @return a set of Nodes representing subjects in the SPARQL query. 
	 */
	public static Set<Node> getAllSubjects(String query)
	{
		Query q = QueryFactory.create(query); // SPARQL 1.1

		// Remember distinct subjects in this
		final Set<Node> subjects = new HashSet<Node>();

		// This will walk through all parts of the query
		ElementWalker.walk(q.getQueryPattern(),
				// For each element...
				new ElementVisitorBase() {
			// ...when it's a block of triples...
			@Override
			public void visit(ElementPathBlock el) {
				// ...go through all the triples...
				Iterator<TriplePath> triples = el.patternElts();
				while (triples.hasNext()) {
					// ...and grab the subject
					subjects.add(triples.next().getSubject());
				}
			}
		}
				);

		return subjects; 
	}

	/***************************************************************
	 * @param query: String representation of SPARQL query
	 * @return a set of Nodes representing objects in the SPARQL query. 
	 */
	public static Set<Node> getAllObjects(String query)
	{
		Query q = QueryFactory.create(query); // SPARQL 1.1

		// Remember distinct subjects in this
		final Set<Node> objects = new HashSet<Node>();

		// This will walk through all parts of the query
		ElementWalker.walk(q.getQueryPattern(),
				// For each element...
				new ElementVisitorBase() {
			// ...when it's a block of triples...
			@Override
			public void visit(ElementPathBlock el) {
				// ...go through all the triples...
				Iterator<TriplePath> triples = el.patternElts();
				while (triples.hasNext()) {
					// ...and grab the subject
					objects.add(triples.next().getObject());
				}
			}
		}
				);

		return objects; 
	}

	/***************************************************************
	 * @param query: String representation of SPARQL query
	 * @return a set of Nodes representing predicates in the SPARQL query. 
	 */
	public static Set<Node> getAllPredicates(String query)
	{
		Query q = QueryFactory.create(query); // SPARQL 1.1

		// Remember distinct subjects in this
		final Set<Node> pred = new HashSet<Node>();

		// This will walk through all parts of the query
		ElementWalker.walk(q.getQueryPattern(),
				// For each element...
				new ElementVisitorBase() {
			// ...when it's a block of triples...
			@Override
			public void visit(ElementPathBlock el) {
				// ...go through all the triples...
				Iterator<TriplePath> triples = el.patternElts();
				while (triples.hasNext()) {
					// ...and grab the subject
					pred.add(triples.next().getPredicate());
				}
			}
		}
				);

		return pred; 

	}

	/***************************************************************
	 * @param query String representation of SPARQL query
	 * @return set of triples in the Query. 
	 * @Note: This only prints the triples in the Query and not paths. 
	 * Example of a path: "?x foaf:knows/foaf:knows/foaf:name ?name ." 
	 */
	public static Set<Triple> getAllTriples(String query)
	{

		Query q = QueryFactory.create(query); // SPARQL 1.1
		final Set<Triple> triples = new HashSet<Triple>(); 

		ElementWalker.walk(q.getQueryPattern(), 
				new ElementVisitorBase() {
			// ...when it's a block of triples...
			@Override
			public void visit(ElementPathBlock el) {

				Iterator<TriplePath> it = el.patternElts(); 

				while (it.hasNext()) {
					TriplePath tp = it.next(); 
					if(tp.isTriple())
					{
						triples.add(tp.asTriple()); 
					}
				}
			}
		}
				);

		return triples; 
	}

	/***************************************************************
	 * @param queries
	 * @return
	 */
	public static Set<Node> findCommonPredicate(String [] queries)
	{
		Set<Set<Node>> qSets = Arrays.stream(queries).
				map(s -> SPARQLInMemoryGraph.getAllPredicates(s)). 
				collect(Collectors.toSet());

		Iterator<Set<Node>> it = qSets.iterator();  
		Set<Node> crossSet = it.next();  
		while(it.hasNext())
		{
			crossSet.retainAll(it.next()); 
		}
		
		return crossSet;
	}

	/***************************************************************
	 * @param label
	 * @return
	 */
	public boolean vertexLabelExist(String label)
	{
		int [] vertices = getVertices().toIntArray(); 

		for(int i : vertices)
		{
			if(getVertexLabelProperty().getValueAsString(i).equalsIgnoreCase(label))
				return true;
		}

		return false; 
	}

	/***************************************************************
	 * @param label
	 * @param type
	 * @return
	 */
	public int getLabelId(String label, Grph.TYPE type)
	{

		if( type == Grph.TYPE.vertex)
		{
			for (int i : getVertices().toIntArray()) {
				if(getVertexLabelProperty().getValueAsString(i).equals(label))
					return i;
			}

		}
		else if(type == Grph.TYPE.edge)
		{ 
			for(int i : getEdges().toIntArray())
			{
				if(getEdgeLabelProperty().getValueAsString(i).equals(label))
					return i;
			}
		}

		return -1 ;
	}


	/***************************************************************
	 * @param query
	 * @return
	 */
	public static Grph sparqlToGraph(String query)
	{
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
		//In-order to create a graph for each Triple we need to get S,P and O and create a node for each
		// and then connect them. 
		// Then print the graph.
		Set<Triple> tSet = SPARQLInMemoryGraph.getAllTriples(query); 

		SPARQLInMemoryGraph g = new SPARQLInMemoryGraph(); 
		Property vtxProp = g.getVertexLabelProperty();
		Property edgProp = g.getEdgeLabelProperty();

		for(Triple t : tSet)
		{
			Node s = t.getSubject(); 
			Node p = t.getPredicate(); 
			Node o = t.getObject(); 

			int sId = -1; 
			int oId = -1;

			if(!g.vertexLabelExist(s.toString()))
			{
				sId= g.addVertex(); 
				vtxProp.setValue(sId, s.toString());
			}
			else
			{
				sId = g.getLabelId(s.toString(), Grph.TYPE.vertex);
			}

			if(!g.vertexLabelExist(o.toString()))
			{
				oId = g.addVertex(); 
				vtxProp.setValue(oId, o.toString());

			}
			else
			{
				oId = g.getLabelId(o.toString(), Grph.TYPE.vertex); 
			}

			int e = g.addDirectedSimpleEdge(sId,oId); 
			edgProp.setValue(e, p.toString());
		}

		return g; 
	}


	/***************************************************************
	 * @param g
	 * @return
	 */
	public static SPARQLInMemoryGraph getLineGraph(SPARQLInMemoryGraph g)
	{
		SPARQLInMemoryGraph lineGraph = new SPARQLInMemoryGraph();

		for (int e : g.getEdges().toIntArray())
		{
			lineGraph.addVertex(e);
			lineGraph.getVertexLabelProperty().setValue(e, g.getEdgeLabelProperty().getValueAsString(e));
		}

		// Because of some assertions, the previous loop has to be completed
		// before this one
		for (int a : g.getEdges().toIntArray())
		{
			for (int b : g.getEdgesAdjacentToEdge(a).toIntArray())
			{
				if(g.getDirectedSimpleEdgeTail(a) == g.getDirectedSimpleEdgeTail(b))
				{
					if (!lineGraph.areVerticesAdjacent(a, b))
					{
						int r = lineGraph.addDirectedSimpleEdge(a, b);
						lineGraph.getEdgeLabelProperty().setValue(r, "l0");
					}
				}
				else if(g.getDirectedSimpleEdgeHead(a) == g.getDirectedSimpleEdgeHead(b))
				{
					if (!lineGraph.areVerticesAdjacent(a, b))
					{
						int r = lineGraph.addDirectedSimpleEdge(a, b);
						lineGraph.getEdgeLabelProperty().setValue(r, "l3");
					}
				}
				else if(g.getDirectedSimpleEdgeHead(a) == g.getDirectedSimpleEdgeTail(b))
				{
					if (!lineGraph.areVerticesAdjacent(a, b))
					{
						int r = lineGraph.addDirectedSimpleEdge(a, b);
						lineGraph.getEdgeLabelProperty().setValue(r, "l2");
					}
				}
				else if(g.getDirectedSimpleEdgeTail(a) == g.getDirectedSimpleEdgeHead(b))
				{
					if (!lineGraph.areVerticesAdjacent(a, b))
					{
						int r = lineGraph.addDirectedSimpleEdge(a, b);
						lineGraph.getEdgeLabelProperty().setValue(r, "l1");
					}
				}
			}
		}

		return lineGraph;
	}


	/***************************************************************
	 * @return
	 */
	public IntArrayList getAllOutEdgeDegrees()
	{
		IntArrayList outDegrees = new IntArrayList(getVertices().getGreatest() + 1);

		for (IntCursor v : getVertices())
		{
			outDegrees.add(getVertexDegree(v.value, Grph.TYPE.edge, Grph.DIRECTION.out));
		}

		return outDegrees;
	}

	
	/***************************************************************
	 * @param g
	 * @return
	 */
	public SPARQLInMemoryGraph getProductGraph( SPARQLInMemoryGraph g)
	{
		SPARQLInMemoryGraph prodg = new SPARQLInMemoryGraph();
		
		//Adding appropriate vertices to the product Graph. 
		for (int i : getVertices().toIntArray()) {
			
			for(int j : g.getVertices().toIntArray())
			{
				if( getVertexLabelProperty().getValueAsString(i).equals(g.getVertexLabelProperty().getValueAsString(j)))
				{
					int v = prodg.addVertex(); 
					String s = getVertexLabelProperty().getValueAsString(i); 
					System.out.println(s);
					prodg.getVertexLabelProperty().setValue(v,s );
					
				}
			}//end for 
		}//end for
		
		int [] prodV = prodg.getVertices().toIntArray(); 
		
		// Now do a pairwise comparison
		for(int k = 0 ; k < prodV.length; k++)
		{
			String slbl = prodg.getVertexLabelProperty().getValueAsString(prodV[k]);
			
			for(int l = 0; l < prodV.length; l++)
			{
				// No self comparison 
				if(k == l) 
					continue; 
				
				//get the label of the vertex
				String dlbl = prodg.getVertexLabelProperty().getValueAsString(prodV[l]);
				
				// Now we have a pair of vertices we should see if they are adjacent in both graphs
				//In the first graph
				boolean s = areVerticesAdjacent(getLabelId(slbl, Grph.TYPE.vertex), getLabelId(dlbl, Grph.TYPE.vertex)); 
				boolean d = g.areVerticesAdjacent( g.getLabelId(slbl, Grph.TYPE.vertex), g.getLabelId(dlbl, Grph.TYPE.vertex)); 
				
				
				if(s && d)
				{
					prodg.addDirectedSimpleEdge(prodV[k], prodV[l]); 
				}
				else if (!s && !d)
				{
					prodg.addDirectedSimpleEdge(prodV[k], prodV[l]); 
				}
				
			}//end for
		}//end for
		
		return prodg; 
	}
	
	


	
	public static  SPARQLInMemoryGraph getProductGraph( SPARQLInMemoryGraph[] lineGrphs)
	{
		
		// Let's first find the intersection of all vertices (i.e. predicates)
		
		//Iterator<SPARQLInMemoryGraph> it = lineGrphs.iterator();
		Set<Set<String>> lables = new HashSet<>(); 
		
		for(SPARQLInMemoryGraph g : lineGrphs)
		{
			Property p = g.getVertexLabelProperty(); 
			Set<String> s = new HashSet<>();
			
			for(int i : g.getVertices().toIntArray())
			{
				s.add(p.getValueAsString(i));
				
			}		
			
			lables.add(s); 
		}// end while
		
		
		Iterator<Set<String>> lblit = lables.iterator(); 
		Set<String> cross = lblit.hasNext()? lblit.next() : null;
		
		while(lblit.hasNext())
		{
			cross.retainAll(lblit.next()); 
		}
		
		// Now we have set of common vertices (i.e. predicates) in line graphs.
		//Now let's calculate their product graph.
		String[] crossAr = cross.toArray(new String[0]); 
		
		for (int i = 0; i < crossAr.length; i++) {
			for (int j = 0; j < crossAr.length; j++) {
				
				if(i == j )
					continue;
				
				
				
				
				
				
			}// end for 
		}//end for
		
		
		
		
		return null ;  
	}
	
	
	
	



	public static void main(String[] args) {
		BasicConfigurator.configure();
		
		String [ ] queryStrings = new String [4]; 
		//Grph [ ] graphs = new Grph [4]; 


		queryStrings [0]  = "" +
				"SELECT * WHERE {\n" +
				"?x1 ?p1 ?z1.\n" +
				"?y1 ?p2 ?z1;\n" +
				"?p3 ?w1.\n" +
				"?w1 ?p4 ?v1.\n" +
				"}\n"; 

		queryStrings [1]  = "" +
				"SELECT * WHERE {\n" +
				"?x2 ?p1 ?z2.\n" + 
				"?y2 ?p2 ?z2.\n" +
				"?t2 ?p3 ?x2;\n" +
				"?p5 ?v1.\n" +
				"?w2 ?p4 ?v1" +
				"}\n"; 

		queryStrings [2]  = "" +
				"SELECT * WHERE {\n" +
				"?x3 ?p1 ?z3;\n" + 
				"?p3 ?y3.\n" +
				"?y3 ?p2 ?z3.\n" +
				"?v1 ?p5 ?y3.\n" +
				"?w3 ?p4 ?v1.\n" +
				"}\n"; 

		queryStrings [3]  = "" +
				"SELECT * WHERE {\n" +
				"?x4 ?p1 ?z4.\n" + 
				"?y4 ?p2 ?z4;\n" +
				"?p3 ?u4.\n" +
				"?w4 ?p6 ?u4;\n" +
				"?p4 ?v1" + 
				"}\n"; 

		
		SPARQLInMemoryGraph [] lineGrphs = new SPARQLInMemoryGraph [queryStrings.length];
		
		for(int i = 0; i < lineGrphs.length ; i++)
		{
			//Create a graph 
			SPARQLInMemoryGraph grph = (SPARQLInMemoryGraph) SPARQLInMemoryGraph.sparqlToGraph(queryStrings[i] );
			lineGrphs[i] = SPARQLInMemoryGraph.getLineGraph(grph);
			//lineGrphs[i].displayGraphstream_0_4_2(); 
		}
		
		SPARQLInMemoryGraph.getProductGraph(lineGrphs); 
		
		
		System.out.println("Done!");
		
		
		
		
		
		
		
		
		
		
		
		
//		SPARQLInMemoryGraph grph1 = (SPARQLInMemoryGraph) SPARQLInMemoryGraph.sparqlToGraph(queryStrings[0]);
//		//grph1.displayGraphstream_0_4_2(); 
//
//		SPARQLInMemoryGraph grphLnGraph1 = SPARQLInMemoryGraph.computeLineGraph(grph1);
//		//grphLnGraph1.displayGraphstream_0_4_2(); 
//		
//		SPARQLInMemoryGraph grph2 = (SPARQLInMemoryGraph) SPARQLInMemoryGraph.sparqlToGraph(queryStrings[1]);
//		grph2.displayGraphstream_0_4_2(); 
//		
//		SPARQLInMemoryGraph grphLnGraph2 = SPARQLInMemoryGraph.computeLineGraph(grph2);
//		grphLnGraph2.displayGraphstream_0_4_2(); 
//		
//		SPARQLInMemoryGraph prodGrph = grphLnGraph1.productGraph(grphLnGraph2); 
//		prodGrph.displayGraphstream_0_4_2();
		
		
		
		
		
//		System.out.println("DONE!");

		//Simple Product Graph
		//First  graph
		
//		SPARQLInMemoryGraph g1 = new SPARQLInMemoryGraph(); 
//		SPARQLInMemoryGraph g2 = new SPARQLInMemoryGraph(); 
//		
//		int v = g1.addVertex();   //add a vertex; 
//		g1.getVertexLabelProperty().setValue(v, "P1"); 
//		
//		v = g1.addVertex(); 
//		g1.getVertexLabelProperty().setValue(v, "P2");
//		
//		g1.addUndirectedSimpleEdge(v, v-1); 
//		
//		// Second Graph
//		v = g2.addVertex();   //add a vertex; 
//		g2.getVertexLabelProperty().setValue(v, "P1");
//		
//		v = g2.addVertex(); 
//		g2.getVertexLabelProperty().setValue(v, "P2");
//		
//		g2.addUndirectedSimpleEdge(v, v-1); 
//		
//		
//		g1.displayGraphstream_0_4_2(); 
//		g2.displayGraphstream_0_4_2(); 
//		
//		
//		// Product Graph
//		SPARQLInMemoryGraph prodg = (SPARQLInMemoryGraph) g1.productGraph(g2); 
//		
//		
//		prodg.displayGraphstream_0_4_2();
//		
		
		
		
		
		
		
		
		
		
		
		
		
		

	}



}
