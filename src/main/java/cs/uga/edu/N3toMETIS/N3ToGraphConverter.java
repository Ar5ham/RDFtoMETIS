package cs.uga.edu.N3toMETIS;

import gnu.trove.TIntCollection;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

public class N3ToGraphConverter {

	private static final boolean DEBUG = true; 		//for Debugging purposes

	private static boolean useRiot = true;		
	private static boolean includeLiterals = false;	

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
			
			

		}

	}


	/**************************************************************************
	 * Print usage information for the user.
	 */
	private static void usage()
	{
		System.out.println("usage: " + N3ToGraphConverter.class.getName() + " (-r|-o) (-a|-t) file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -r, --riot : use RiotLoader to load graph(s) (default)");
		System.out.println(" -o, --own  : use own loader to load graph(s) (supports something like NTriples with types for literals)");
		System.out.println(" -a, --hash : use hash table for uri-to-string mapping (default)");
		System.out.println(" -t, --trie : use trie for uri-to-string mapping (slow, but saves memory)");
		System.out.println(" -l, --literals : include literals in graph (experimental)");
		System.out.println(" -h, --help : print this help and exit");
	}

}
