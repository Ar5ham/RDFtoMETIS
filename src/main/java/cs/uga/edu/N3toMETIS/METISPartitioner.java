package cs.uga.edu.N3toMETIS;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jena.schemagen;
import cs.uga.edu.util.Util;

public class METISPartitioner {

	private static final boolean DEBUG = true;
	
	private static boolean useDirectHops = true;		//Using directed hop (DEFAULT)
	private static boolean useHashPartitioning = false;
	private static Integer numPartitions = 2;
	private static String partfile;
	private static String nodemap;
	private static TLongIntHashMap longhashes ;
	private static TObjectLongHashMap<String> schemaMap = new TObjectLongHashMap<String>(); 
	
	public static void main(String[] args) {

		// Let's go through the user input and pars the flags.
		if(args.length == 0)
		{
			usage();
			System.exit(0);
		}
		
		for (int i = 0; i < args.length; i++) {
			//Directed Hop?
			if(args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--directed") )
			{
				useDirectHops  = true; 
			}
			else if (args[i].equalsIgnoreCase("-u") || args[i].equalsIgnoreCase("--undirected"))
			{
				useDirectHops = false;
			}
			else if (args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--hash"))
			{
				useHashPartitioning  = true;
				if (args.length < i + 4)
				{
					usage();
					System.exit(1);
				}

				//Number of partitions is the next param 
				numPartitions  = Integer.valueOf(args[++i]);
			}
			else if (args[i].equalsIgnoreCase("-g") || args[i].equalsIgnoreCase("--graph"))
			{
				useHashPartitioning = false;
				if (args.length < i + 4)
				{
					usage();
					System.exit(1);
				}

				partfile = args[++i]; 
			}
			else if (args[i].equalsIgnoreCase("-nm") || args[i].equalsIgnoreCase("--nodemap"))
			{
				if (args.length < i + 2)
				{
					usage();
					System.exit(1);
				}

				nodemap = args[++i]; 
			}
//			else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--hops"))
//			{
//				if (args.length < i + 4)
//				{
//					usage();
//					System.exit(1);
//				}
//
//				hops = Integer.valueOf(args[++i]);
//			}
			else
			{
				//nodecnt = Integer.valueOf(args[i]);

				partition(Arrays.copyOfRange(args, i, args.length));
				break;
			}		
		}
	}





	private static void partition(String[] args) {
		
		final TShortArrayList part = new TShortArrayList(1);
		part.add((short) -1); 	// Ignore the first element.
		
		Charset encoding = StandardCharsets.UTF_8; 
		schemaMap.put("type", Util.hash64("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")); 
		schemaMap.put("imports", Util.hash64("http://www.w3.org/2002/07/owl#imports")); 
				
		//Finding max number of partition and the partition for each node. 
		if(!useHashPartitioning)
		{
			int maxPartition = 0; 
			
			try(final BufferedReader bufRead = new BufferedReader( new FileReader(partfile)))
			{
				String line =  null ; 
				while ((line = bufRead.readLine()) != null)
				{
					short p = Short.valueOf(line); 
					if (p > maxPartition)
					{
						maxPartition = p;
					}
					part.add(p); 
				}
			} catch (IOException e) {
				System.err.println("cannot read from " + partfile + ".");
				e.printStackTrace();
				System.exit(1);
			}
			numPartitions = maxPartition + 1;
		}
		
		// read the mapping for Subject hash --> Number from file
		try {
		
			 longhashes = (TLongIntHashMap) Util.readSerializedNodeMap(new File(nodemap));
		
		} catch (ClassNotFoundException | IOException e) {
			System.err.println("cannot read from " + nodemap + ".");
			e.printStackTrace();
			System.exit(1);
		} 
		
		///////////////////////////////////////////////////////////////////////////////////////
		// Creating output files for each partition											 //
		///////////////////////////////////////////////////////////////////////////////////////
		System.out.println("opening " + numPartitions + " output files...");
		
		// Opening PrintWriter for writing output files.
		final PrintWriter files[] = new PrintWriter[numPartitions];

		try
		{
			for (int i = 0; i < numPartitions ; i++)
				files[i] = new PrintWriter(new File("sample.part" + i +".n3"),"UTF-8");	
		}
		catch(Exception e)
		{
			System.err.println("cannot open output files.");
			e.printStackTrace();
			System.exit(2);
		}

		System.out.println("parsing triples...");
		
		/**********************************************************************************
		 * Now let's read the original input file
		 * KB: Here we are reading the original input file (Not processed)
		 * so that the "type" and "type" like triples will also get distributed. 
		 * However, we need to keep track of Object URIs that get replicated in different
		 * partitions and then later on we need to add "type" triples to those partitions
		 * as well. 
		 **********************************************************************************/
		
		Path path ;
		
		for (int i = 0; i < args.length; i++) {
			
			path = Paths.get(args[i]); 
			try (BufferedReader reader = Files.newBufferedReader(path, encoding)){
				
				String line = null;
				while ((line = reader.readLine()) != null) 
				{
					String triple[] = line.substring(0, line.length() - 1).trim().split("\\s+");
					
					String subUri, objUri, predUri; 
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
					
					SubjId = longhashes.get(Util.hash64(subUri));
					
					if(SubjId == 0)
					{
						System.err.println("Did not find the hash for " + subUri) ;
						continue;
					}
					
					//check if the predicate is type or import write to all partitions
					predUri = triple[1].substring(1, triple[1].length() - 1);
					if(Util.hash64(predUri) == schemaMap.get("type") || Util.hash64(predUri) == schemaMap.get("imports"))
					{
						for (int j = 0; j < files.length; j++) {
							
							emit(files[i], line);
							
						}
					}else{
						//Write the triple in 
						emit(files[part.get(SubjId)], line ); 
					}
					
					/* **************************************************************************
					 * If the Object is a URI (Not literal) and Subject and Object belong to  
					 * different partitions and we are using un-directed hops we will need to 
					 * replicate the s-->o in both Subject and Object partition				  	
					 *****************************************************************************/
					
					if(useDirectHops == false)
					{
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
							System.err.println("Invalid object: SKIP, " + line);
							continue;
						}

						ObjId = longhashes.get(Util.hash64(objUri)); 
						emit(files[part.get(ObjId)], line ); 
					}
					
				}
	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}





	/**************************************************************************
	 * @param printWriter
	 * @param line
	 */
	private static void emit(PrintWriter printWriter, String line) {
		
		
	}





	/**************************************************************************
	 * Notes: 
	 * graph file: is the partition representation of the graph. i.e which cluster each node(entity) is 
	 * assigned to. 
	 * 
	 * nodeCount: number of entities (nodes) in the graph
	 */
	private static void usage() {

		System.out.println("usage: "
				+ METISPartitioner.class.getName()
				+ " (-p p | -g file) [-d | -u] [-nm file] file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -p, --hash p     : use hash partitioning on subject and create p partitions");
		System.out.println(" -g, --graph file : use graph-based partitioning stored in file (Partition file generated by METIS)");
		System.out.println(" -d, --directed   : directed hops (default)");
		System.out.println(" -u, --undirected : undirected hops (untested)");
		System.out.println(" -nm, --nodemap file: read node map produced by  Metisloader");
		//System.out.println(" -h, --hops h     : use h hops ");
		//System.out.println(" -l, --literals   : graph includes literals (experimental)");
		System.out.println(" --help           : print this help and exit");
	}







}
