package cs.uga.edu.N3toMETIS;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.jena.riot.RDFDataMgr;


import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import cs.uga.edu.util.Util;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

public class N3ToGraphConverter {

	private static final boolean DEBUG = true; 			 //for Debugging purposes

	private static boolean useRiot 			= true;		
	private static boolean includeLiterals  = false;	
	private static boolean useradixtree 	= false;     // Flag for using RadixTree (Not used for now)
	private static int  edgeCount = 0; 					 // Total number of edges
	private static int  max 	  = 1;					 // Total number of nodes
	/* 
	 * Used for edge mapping (Contains subject and array list of object ids, aka there is an edge from sub --> obj)
	 * An open addressed Map implementation for int keys and Object values
	 */
	private static TIntObjectHashMap<TIntCollection> edges = new TIntObjectHashMap<TIntCollection>();
	/* 
	 * if a list contains more than that many entries, convert it into a HashSet
	 * this is used to tradeoff storage cost (which is cheap for ArrayLists) vs. cost for contains() 
	 * (which is cheap for HashSets)
	 */
	private static final int THRESHOLD = 10;

	// Keeping all the hashes for node labels
	private static TLongIntHashMap longhashes = new TLongIntHashMap();

	private static String outfile ;
	

	public static void main(String[] args) {

		// Let's go through the user input and pars the flags.
		if(args.length == 0)
		{
			usage();
			System.exit(0);
		}
		
		for (int i = 0; i < args.length; i++) {

			//If help flag is supplied, print usage info and exit.
			if ((args[i].equalsIgnoreCase("-h")) || (args[i].equalsIgnoreCase("--help")))
			{
				usage();
				System.exit(0);
			}
			else if (args[i].equalsIgnoreCase("-ol") || args[i].equalsIgnoreCase("--own")) {
				System.err.println("This feature is not implemented yet. Using riot");
				useRiot = true;
			}
			else if (args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--riot")) {
				useRiot = true;
			}
			else if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--hash"))
			{
				useradixtree = false;
			}
			else if (args[i].equalsIgnoreCase("-t") || args[i].equalsIgnoreCase("--trie"))
			{
				System.err.println("This feature is not implemented yet."); 
				useradixtree = false;
			}
			else if ((args[i].equalsIgnoreCase("-l"))||(args[i].equalsIgnoreCase("--literals")))
			{
				System.err.println("This feature is not implemented yet."); 
				includeLiterals = false;
			}
			else if (args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--output"))
			{
				if(args.length < i + 2)
				{
					usage();
					System.exit(1);
				}

				outfile = args[++i]; 

			}			
			else if (args[i].startsWith("-"))
			{
				usage();
				System.exit(1);
			}    		
			else
			{
				if(outfile == null)
				{
					usage();
					System.exit(1);
				}
				// if none of the above is true, the rest should be name of the input file(s)
				perform(Arrays.copyOfRange(args, i, args.length));
				break;
			}

		}
	}

	/**************************************************************************
	 * Print usage information for the user.
	 */
	private static void usage()
	{
		System.out.println("usage: " + N3ToGraphConverter.class.getName() + " (-r|-ol) (-a|-t) -o outfile.txt file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -r,  --riot : use RiotLoader to load graph(s) (default)");
		System.out.println(" -ol, --own  : use own loader to load graph(s) (supports something like NTriples with types for literals)");
		System.out.println(" -a,  --hash : use hash table for uri-to-string mapping (default)");
		System.out.println(" -t,  --trie : use trie for uri-to-string mapping (slow, but saves memory)");
		System.out.println(" -l,  --literals : include literals in graph (experimental)");
		System.out.println(" -o,  --output : output file");
		System.out.println(" -h,  --help : print this help and exit");
	}

	/**************************************************************************
	 * @param args
	 */
	private static void perform(String[] args) {


		Iterator <Triple> itt = null; 			

		// For as many file as we have
		for (int i = 0; i < args.length;i++)
		{


			if(useRiot)
			{
				Graph g =  RDFDataMgr.loadGraph(args[i]); 
				// Node.Any matches any node.
				itt = g.find(Node.ANY, Node.ANY, Node.ANY);
			}
			else
			{
				//TODO: Implement this..... 
				/* Write a loader that returns list of all nodes in the graph as a list, store 
				 * them in a list<node> and get an iterator over the list
				 */
			}

			// Iterate over the Graph here.
			while(itt.hasNext())
			{
				Triple triple = itt.next();
				if (DEBUG) 
				{
					System.out.println(triple.getSubject() + "; " + triple.getPredicate() + "; " + triple.getObject());
				}

				// Get s o p 
				// Subject can be either URI or Blank node
				Node subNode = triple.getSubject();
				String s = subNode.isURI() ? subNode.getURI() : subNode.getBlankNodeLabel();
				//Assign an id to the node
				Integer SubjId= getId(s);

				// NOTE: no mapping for predicates at this point 

				Node objNode = triple.getObject();
				//If Object Node is URI or blank we create a mapping for it.
				if (objNode.isURI() || objNode.isBlank())
				{
					String o = objNode.isURI() ? objNode.getURI() : objNode.getBlankNodeLabel();
					Integer ObjId = getId(o);
					addEdge(SubjId,ObjId);
				}
				else
				{
					//TODO: Do we need to include Literals here or not? Experiment
				}

			}
		}//end for


		if (DEBUG) {
			System.out.println((max - 1 ) + " " + edgeCount); 
		}

		//NOTE: The first line of METIS file is number of nodes and number of edges
		File f = new File(outfile); 
		try {
			Util.stringToFile((max - 1 ) + " " + edgeCount, f, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int count  = 0; 
		for (int i = 0; i < max; i++) {

			StringBuilder sb = new StringBuilder();

			TIntCollection l = edges.get(i);
			//if mapping exist for this key
			if(l != null)
			{
				count += l.size();
				TIntIterator it = l.iterator();				
				for (int k = l.size(); k > 0; k--)
				{
					sb.append(it.next()).append(" ");
				}

				//				while(it.hasNext())
				//				{
				//					sb.append(it.next()).append(" "); 
				//					count ++ ;
				//				}

			}
			System.out.println(sb.toString());
			try {
				Util.stringToFile(sb.append("\n").toString(), f, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		System.err.println(count + " edges.");

	}// end perform


	/**************************************************************************
	 * @param s Node URI or label (if blank) 
	 * @return returns the id of s.
	 */
	private static final int getId(String s) 
	{

		long hash = Util.hash64(s);		//Create hash of s 
		int id = longhashes.get(hash);	// see if the hash already exist
		if (id == 0)
		{
			id = max;
			longhashes.put(hash,id); 
			max++;
		}
		return id;	
	}

	/**************************************************************************
	 * This method create a edges between nodes. 
	 * No self loop nodes and all edges are undirected 
	 * Foe Metis when there is an edge from s -> o there should be ab edge from o -> s as well
	 * @param subj subject id
	 * @param obj  object id 
	 */
	private static void addEdge(int subj, int obj) {

		// no self loops for the partitioning
		if (subj == obj) 
			return;


		// TIntCollection is an interface, TIntArrayList is one of the implementing classes
		// get the V with subj as a key if exist.
		TIntCollection l =  edges.get(subj);
		//Subject is not in the list
		if (l == null)
		{
			// create an TIntArrayList of size 10.
			l = new TIntArrayList(THRESHOLD);
			//Add the subj and array list coresponding to obj ids to the map
			edges.put(subj,l);
		}

		//if it is not null and doesnt contain the object id
		if(!l.contains(obj))
		{
			l.add(obj);
			if(l instanceof TIntArrayList && l.size() == THRESHOLD)
			{
				//TIntHashSet is also an implementation of TIntCollection interface
				TIntHashSet hs = new TIntHashSet(l); 
				// If the map previously contained a mapping for the key, the old value is replaced by the specified value
				edges.put(subj, hs); 
			}
			edgeCount++;

		}

		//Now lets get the object
		l = edges.get(obj);

		if (l == null)
		{
			l = new TIntArrayList(THRESHOLD);
			edges.put(obj,l);
		}

		if(! l.contains(subj))
		{
			l.add(subj);
			//Don't need to add to the edgeCount here.
			if(l instanceof TIntArrayList && l.size() == THRESHOLD)
			{
				TIntHashSet hs = new TIntHashSet(l); 
				edges.put(obj, hs); 

			}
		}
	}




}
