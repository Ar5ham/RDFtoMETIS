package cs.uga.edu.N3toMETIS;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import cs.uga.edu.util.Util;
import gnu.trove.TIntCollection;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

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
		System.out.println("usage: " + MetisLoader.class.getName() + " (-r|-ol) (-a|-t) -o outfile.txt file1.n3 [file2.n3 ...]");
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
				while ((line = reader.readLine()) != null) {
					//process each line 
				
					line=line.substring(0, line.length() - 1).trim();
					String triple[] = line.split("\\s+");
					String subUri, objUri; 
					Integer SubjId, objId; 
					
					
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
						System.out.print(subUri + "--> ");
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
					
					objId = getId(objUri); 
					
					
					
					
					
					
					
					
					
					
					
					
					
				}      
			} catch (IOException e) {
				System.err.println("ERROR: Failed to read the file " + args[i]);
				e.printStackTrace();
			}


		}//End for



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


	}



}
