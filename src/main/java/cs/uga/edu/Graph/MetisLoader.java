/**
 * @author Arsham
 * @LastUpdated: May 11, 2014
 * This class will read an NTriple and will out put the result in to a file which will be 
 * the input of METIS partitioner.
 * The mapping for the 
 */


package cs.uga.edu.Graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import cs.uga.edu.util.Util;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

public class MetisLoader {


	private static final boolean DEBUG = true; 			 //for Debugging purposes

	private static boolean includeLiterals  = false;
	private static boolean useradixtree		= false;

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

		for (int i = 0; i < args.length; i++) 
		{
			//If help flag is supplied, print usage info and exit.
			if ((args[i].equalsIgnoreCase("-h")) || (args[i].equalsIgnoreCase("--help")))
			{
				usage();
				System.exit(0);
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
				if(args.length < i + 1)
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
			else{

				if(outfile == null)
				{
					usage();
					System.exit(1);
				}

				long startTime = System.nanoTime(); 
				// if none of the above is true, the rest should be name of the input file(s)
				perform(Arrays.copyOfRange(args, i, args.length));

				long endTime = System.nanoTime(); 
				System.out.println("Duration: " + (endTime - startTime) * 1.0e-9);

				break;
			}
		}
	}

	/**************************************************************************
	 * Print usage information for the user.
	 */
	private static void usage()
	{
		System.out.println("usage: " + MetisLoader.class.getName() + " (-l) (-a|-t) -o outfile.txt file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -a,  --hash : use hash table for uri-to-string mapping (default)");
		System.out.println(" -t,  --trie : use trie for uri-to-string mapping (slow, but saves memory)");
		System.out.println(" -l,  --literals : include literals in graph (experimental)");
		System.out.println(" -o,  --output : output file");
		System.out.println(" -h,  --help : print usage and exit");
	}

	/**************************************************************************
	 * @param args
	 */
	private static void perform(String[] args) {

		Path path ;
		Charset encoding = StandardCharsets.UTF_8; 

		for (int i = 0; i < args.length; i++) {

			path = Paths.get(args[i]); 
			try (BufferedReader reader = Files.newBufferedReader(path, encoding)){
				String line = null;
				while ((line = reader.readLine()) != null) 
				{
					//process each line 

					line = line.substring(0, line.length() - 1).trim();
					String triple[] = line.split("\\s+");
					String subUri, objUri; 
					Integer SubjId, ObjId; 


					if(DEBUG)
					{
						System.out.println(line);
					}

					if (triple.length < 3)
					{
						System.err.println("[" + path.getFileName() + "] ignore short line " + line);
						continue;
					}
					else if (triple.length > 3)
					{
						for (int j = 3; j < triple.length; j++)
							triple[2] += " " + triple[j];
					}

					// Processing Subject String 
					//if subject is URI
					if (triple[0].startsWith("<") && triple[0].endsWith(">"))
					{
						subUri = triple[0].substring(1, triple[0].length() - 1);
					}
					else if (triple[0].startsWith("_:"))
					{
						// for anonymous 
						subUri = triple[0].substring(2); 
					}
					else
					{
						System.err.println("[" + path.getFileName() + "] invalid subject " + triple[0] + ", skip.");
						continue;
					}

					if(DEBUG)
					{
						System.out.print(subUri + " --> ");
					}

					SubjId = getId(subUri);

					// Processing Object String
					if(triple[2].startsWith("<") && triple[2].endsWith(">"))
					{
						objUri = triple[2].substring(1, triple[2].length() - 1); 

					}
					else if (triple[2].startsWith("_:"))
					{
						objUri = triple[2].substring(2); 
					}
					else
					{
						System.err.println("[" + path.getFileName() + "] invalid object " + triple[2]+", skip.");
						continue;
					}

					if(DEBUG)
					{
						System.out.println(objUri);

					}


					ObjId = getId(objUri); 

					addEdge(SubjId,ObjId);
				}      
			} catch (IOException e) {
				System.err.println("ERROR: Failed to read the file " + args[i]);
				e.printStackTrace();
			}
		}//End for

		if(DEBUG){
			System.out.println((max - 1 ) + " " + edgeCount); 
		}

		//Create METIS input file

		File f = new File(outfile); 
		try {
			Util.stringToFile((max - 1 ) + " " + edgeCount, f, true);
		} catch (IOException e) {
			e.printStackTrace();
		}


		int cnt  = 0; 
		for (int i = 0; i < max; i++) {

			StringBuilder sb = new StringBuilder();

			TIntCollection l = edges.get(i);
			//if mapping exist for this key
			if(l != null)
			{
				cnt += l.size();
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
				e.printStackTrace();
			}

		}

		System.out.println(cnt + " edges.");
		File file = new File(args[0].substring(0 , args[0].lastIndexOf('.')) + ".NodeMap"); 
		try {
			
			Util.serializeNodeMap(longhashes, file);
			
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println("Error: Failed to write the Node map to file \n" + e.getMessage());
			
			 
		}
		

		// Calculating memory usage: 
		Runtime runtime = Runtime.getRuntime();
		// Run the garbage collector
		runtime.gc();
		// Calculate the used memory
		long memory = runtime.totalMemory() - runtime.freeMemory();
		System.out.println("Used memory is bytes: " + memory);
		System.out.println("Used memory is megabytes: "
				+ bytesToMegabytes(memory));
	}

	public static long bytesToMegabytes(long bytes) {
		final long MEGABYTE = 1024L * 1024L;
	    return bytes / MEGABYTE;
	  }

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
	} //End method



}
