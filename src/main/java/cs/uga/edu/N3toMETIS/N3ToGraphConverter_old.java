package cs.uga.edu.N3toMETIS;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.jena.riot.RDFDataMgr;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.tdb.store.Hash;

import cs.uga.edu.util.MultiBiMap;
import cs.uga.edu.util.Util;
import net.kotek.jdbm.DB;
import net.kotek.jdbm.DBMaker;


public class N3ToGraphConverter_old {

	private static final boolean DEBUG = true;

	//Options
	private static boolean useRiot = true;					// Flag for using Riot (Default)
	private static boolean usejdbm = false;					// Flag for using JDBM ((Not used for now)
	private static boolean useradixtree = false;			// Flag for using RadixTree (Not used for now)
	private static boolean includeLiterals = false;			// Flag for storing literal (Not used for now)
	private static boolean useGuava = false;				// Flag for using Guava library for edge mapping 

	private static DB db;							 
	private static Map<String,Integer> map;			// Map of uri-to-string
	private static int  edgeCount = 0; 				// Total number of edges
	private static int max = 1;

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
	
	//private static HashBiMap<Integer,Integer > G_edges  = HashBiMap.create ();
	//Multimap<Integer, Integer> G_mapping = HashMultimap.create(); 
	
	// Keeping all the hashes for node lables
	static TLongIntHashMap longhashes = new TLongIntHashMap();




	public static void main(String[] args) {

		// Let's go through the user input and pars the flags.

		for (int i = 0; i < args.length; i++) {

			//If help flag is supplied, print usage info and exit.
			if ((args.length == 0) || (args[i].equalsIgnoreCase("-h")) || (args[i].equalsIgnoreCase("--help")))
			{
				usage();
				System.exit(0);
			}
			else if (args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--own")) {
				System.err.println("This feature is not implemented yet. Using riot");
				useRiot = true;
			}
			else if (args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--riot")) {
				useRiot = true;
			}
			else if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--hash"))
			{
				usejdbm=false; 
				useradixtree=false;
			}

			else if (args[i].equalsIgnoreCase("-t") || args[i].equalsIgnoreCase("--trie"))
			{
				System.err.println("This feature is not implemented yet."); 
				usejdbm = false; 
				useradixtree = false;
			}
			else if (args[i].equalsIgnoreCase("-j") || args[i].equalsIgnoreCase("--jdbm"))
			{
				System.err.println("This feature is not implemented yet."); 
				usejdbm = false;
				useradixtree = false;
			}
			else if ((args[i].compareTo("-l")==0)||(args[i].compareTo("--literals")==0))
			{
				System.err.println("This feature is not implemented yet."); 
				includeLiterals = false;
			}
			else if (args[i].equalsIgnoreCase("-gu") || args[i].equalsIgnoreCase("--guava"))
			{
				useGuava = true; 

			}
			else if (args[i].startsWith("-"))
			{
				usage();
				System.exit(1);
			}    		
			else
			{
				// if none of the above is true, the rest should be name of the file(s)
				perform(Arrays.copyOfRange(args, i, args.length));
				break;
			}
		}// end for
	}


	/**************************************************************************
	 * @param args
	 */
	private static void perform(String[] args) {

		Iterator <Triple> itt = null; 

		// If using JDBM Initialize .....  
		if (usejdbm)
		{
			String fileName = "urimap";
			DBMaker dbm = new DBMaker(fileName);
			dbm.enableHardCache();
			dbm.disableTransactions();
			db = dbm.build();
			//Creates Map which persists data into DB
			map = db.createHashMap("uri");
		}

		// For performance testing.....
		long time = System.currentTimeMillis();

		// For as many file as we have
		for (int i = 0; i < args.length;i++)
		{
			//check how long did it take 
			if((i + 1) % 100 == 0)
			{
				long now = System.currentTimeMillis() ; 
				System.err.println("file " + (i+1) + " of " +args.length + " (" + ((100.0 * i) / args.length) + 
						"%), last 100 in " + (now - time)/1000.0 + "s");
				time = now;
			}


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
				if (DEBUG) {
					System.out.println(triple.getSubject() + "; " + triple.getPredicate() + "; " + triple.getObject());
				}
				
				
				// Get s o p
				Node subNode = triple.getSubject();
				String s = subNode.isURI() ? subNode.getURI() : subNode.getBlankNodeLabel();
				Integer SubjId= getId(s);

				// no mapping for predicates at this point 

				Node objNode = triple.getObject();
				//If Object Node is URI or blank we create a mapping for it.
				if (objNode.isURI() || objNode.isBlank())
				{
					String o = objNode.isURI() ? objNode.getURI() : objNode.getBlankNodeLabel();
					Integer ObjId = getId(o);
					addEdge(SubjId,ObjId);
				}
				else if (includeLiterals)
				{
					//TODO: Do we need to include Literals here or not? 
				}

//				if (DEBUG) {
//					System.out.println(triple.getSubject() + "; " + triple.getPredicate() + "; " + triple.getObject());
//				}
			}
		}//end for
		

		if (DEBUG) {
			System.out.println((max - 1 ) + " " + edgeCount); 
		}
		File f = new File("Sample.graph.txt"); 
		try {
			Util.stringToFile((max - 1 ) + " " + edgeCount, f, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int count = 0 ; 
		for (int i = 0; i < max ; i++) 
		{
			
			StringBuilder sb = new StringBuilder();
			
			TIntCollection l = edges.get(i);
			
			//if mapping exist for this key
			if(l != null)
			{
				//sb.append(i).append(": ");
				count += l.size();
				TIntIterator it = l.iterator();				
				for (int k = l.size(); k > 0; k--)
				{
					sb.append(it.next()).append(" ");
				}
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

		if(!useGuava)
		{
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
			l= edges.get(obj);

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
		else
		{
			// If using Google Guova
			//1. Create miltiBiMap
			MultiBiMap<Integer, Integer> gMap = new MultiBiMap<Integer, Integer>(); 
			
			/*2. if K,V doesn't exist, add it to the map. This is a multiBiMap meaning 
			 * if you have a K->V you also have a V->K relationship.
			 */
			if(!gMap.containEntry(subj, obj))
			{
				gMap.put(subj, obj);
				
			}
		}
	}

	/**************************************************************************
	 * @param s Node URI or label (if blank) 
	 * @return returns the id of s.
	 */
	private static final int getId(String s) {
		
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
	 * Print usage information for the user.
	 */
	private static void usage()
	{
		System.out.println("usage: " + N3ToGraphConverter_old.class.getName() + " (-r|-o) (-a|-t) file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -r, --riot : use RiotLoader to load graph(s) (default)");
		System.out.println(" -o, --own  : use own loader to load graph(s) (supports something like NTriples with types for literals)");
		System.out.println(" -a, --hash : use hash table for uri-to-string mapping (default)");
		System.out.println(" -j, --jdbm : use JDBM for uri-to-string mapping (very slow)");
		System.out.println(" -t, --trie : use trie for uri-to-string mapping (slow, but saves memory)");
		System.out.println(" -l, --literals : include literals in graph (experimental)");
		System.out.println(" -h, --help : print this help and exit");
	}

}
