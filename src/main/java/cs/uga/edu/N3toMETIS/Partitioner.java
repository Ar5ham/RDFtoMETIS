package cs.uga.edu.N3toMETIS;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;

import cs.uga.edu.util.Util;


public class Partitioner {

	private static boolean DEBUG = true; 

	private static boolean useDirectHops 		= true;		//Using directed hop (DEFAULT)
	private static boolean useHashPartitioning 	= false;
	private static int 	   numPartitions 		= 5;
	private static String  partfile;
	private static int 	   hops 				= 0;
	private static boolean useRiot 				= true;
	private static boolean forceIsOwned     	= false;
	private static int 	   nodecnt;
	private static int max = 1;
	// Keeping all the hashes for node lables
	static TLongIntHashMap longhashes = new TLongIntHashMap();


	final static Node IS_OWNED = NodeFactory.createURI("http://rdf2metis/isOwned"); 
    final static Node YES = NodeFactory.createLiteral("Yes");

	public static void main(String[] args) {

		if (args.length < 4)
		{
			usage();
			System.exit(1);
		}

		for (int i = 0; i < args.length; i++) {

			//Directed Hop?
			if(args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--directed") )
			{
				useDirectHops = true; 
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
				numPartitions = Integer.valueOf(args[++i]);
			}
			else if (args[i].equalsIgnoreCase("-g") || args[i].equalsIgnoreCase("--graph"))
			{
				useHashPartitioning = false;
				if (args.length < i + 4)
				{
					usage();
					System.exit(1);
				}

				partfile = args[++i];  //TODO: Part file ? 
			}
			else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--hops"))
			{
				if (args.length < i + 4)
				{
					usage();
					System.exit(1);
				}

				hops = Integer.valueOf(args[++i]);
			}
			else if (args[i].equalsIgnoreCase("-r")|| args[i].equalsIgnoreCase("--riot"))
			{
				useRiot = true;
			}
			//TODO: EXPERIMENT WITH THIS LATER
			//    		else if ((args[i].compareTo("-l")==0)||(args[i].compareTo("--literals")==0))
			//    		{
			//    			includeLiterals=true;
			//    		}
			else if (args[i].equalsIgnoreCase("-f") || args[i].equalsIgnoreCase("--force"))
			{
				forceIsOwned = true;
			}
			else if (args[i].startsWith("-"))
			{
				usage();
				System.exit(1);
			}
			else
			{
				nodecnt = Integer.valueOf(args[i]);

				partition(Arrays.copyOfRange(args, i+1, args.length));
				break;
			}
		}

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
				+ Partitioner.class.getName()
				+ " [-o | -r] (-p p | -g file) [-d | -u] [-h h] nodecount file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -r, --riot       : use RiotLoader to load graph(s) (default)");
		System.out.println(" -p, --hash p     : use hash partitioning on subject and create p partitions");
		System.out.println(" -g, --graph file : use graph-based partitioning stored in file");
		System.out.println(" -d, --directed   : directed hops (default)");
		System.out.println(" -u, --undirected : undirected hops (untested)");
		System.out.println(" -h, --hops h     : use h hops ");
		//System.out.println(" -l, --literals   : graph includes literals (experimental)");
		System.out.println(" -f, --force      : force isOwned facts even if hops<2");
		System.out.println(" --help           : print this help and exit");
	}


	/**************************************************************************
	 * @param args
	 */
	private static void partition(String args[]) {

		final short part[] = new short[ nodecnt + 1]; // max 2**15 partitions should be enough

		//figure out number of partitions from part(graph) file. 
		if(!useHashPartitioning)
		{
			System.out.println("loading partitioning for "+nodecnt+" nodes...");

			int maxPartition = 0;

			try(final BufferedReader bufRead = new BufferedReader( new FileReader(partfile)))
			{
				int pos = 0;

				String line = bufRead.readLine();
				while (line != null)
				{
					part[++pos] = Short.valueOf(line);
					if (part[pos] > maxPartition)
					{
						maxPartition = part[pos];
					}
					line = bufRead.readLine();
				}
			}
			catch(Exception e)
			{
				System.err.println("cannot read from " + partfile + ".");
				e.printStackTrace();
				System.exit(1);
			}

			numPartitions = maxPartition + 1;
		}
		
		/////////////////////////////////////////////////////////////////////////////////////////
		// Keeping track of which nodes are primarily allocated to a partition and which nodes //
		// are replicated. 																	   //
		/////////////////////////////////////////////////////////////////////////////////////////
		
		// this collects the hash64 of all nodes that are primarily allocated to a partition
		final TLongHashSet[] primary = new TLongHashSet[numPartitions];

		// this collects the hash64 of all nodes that are replicated to this partition
		TLongHashSet[] secondary = new TLongHashSet[numPartitions];

		for (int i = 0; i < primary.length; i++) {

			primary[i] = new TLongHashSet(); 
			secondary[i] = new TLongHashSet();
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

		// this assumes that we get the triples in the same order as in the run used to generate the graph representation
		// if it turns out that this is not the case, we need to write the mapping to disk in that process and read it again here
		// I think we need to do this since we are using ANY to pick a random node.

		for (int i = 0; i < args.length; i++) {

			//if ((i+1)%100==0)
			//{
			//	final long now = System.currentTimeMillis();
			//	System.err.println("[parse] file " + (i + 1) + " of " + args.length + " ("+((100.0*i)/args.length) + "%), last 100 in " + (now - last)/1000.0 + "s");
			//	last = now;
			//}

			final Iterator<Triple> it; 
			
			if(useRiot)
			{   
				//Load RDF using Riot
				it = RDFDataMgr.loadGraph(args[i]).find(Node.ANY, Node.ANY, Node.ANY); 
			}
			else{
				//Use your own loader. 
				it = load(args[i]).iterator(); 
			}

			while (it.hasNext()) {
				final Triple triple = it.next();
				
				final Node subNode = triple.getSubject();
				String s = subNode.isURI() ? subNode.getURI() : subNode.getBlankNodeLabel();
				
				final Node objNode = triple.getObject();
				
				
				int subId; 
				
				if(!useHashPartitioning)
				{	
					subId = getId(s); 
					
					/* subId is integer between 1..n where n is number of nodes in a graph
					 * We know from partFile (stored in part[]) which partition that node belongs to and therefore 
					 * which file it will be written to. */ 
					
					//Write the triple to the partition file it should be added to.
					// Add Triple to SUBJECT partition. 
					emit(files[part[subId]],triple);
					
					if(objNode.isURI() || objNode.isBlank())
					{
						String o = objNode.isURI() ? objNode.getURI() : objNode.getBlankNodeLabel();
						int objId = getId(o);
						
						/* **************************************************************************
						 * If the Object is a URI (Not literal) and Subject and Object belong to  
						 * different partitions and we are using un-directed hops we will need to 
						 * replicate the s-->o in both Subject and Object partition				  	
						 *****************************************************************************/
						//for undirected hops.
						if(useDirectHops == false && hops >= 1)
						{
							//replicates sub in obj's partition
							emit(files[part[objId]],triple);
							// remember that s was replicated in o's partition
							secondary[part[objId]].add(Util.hash64(s)); 
						}
						
						// remember that o was replicated in s's partition
						secondary[part[subId]].add(Util.hash64(o)); 
					}
					//NOTE: We are not keeping literals at this point.
					//TODO: Experiment with this later.
//					else if (includeLiterals)
//					{
//						id(on.getLiteralLexicalForm()); // this is needed to make sure the id mapping is the same used during graph generation
//					}
					
					//Here we are adding <isOwned> "yes" relation for the triple
					if ((hops > 1)||(forceIsOwned))
					{
						final long subjecthash = Util.hash64(s);
						if ( !primary[part[subId]].contains(subjecthash) )
						{
							emit(files[part[subId]],Triple.create(subNode, IS_OWNED, YES));
							primary[part[subId]].add(subjecthash);
						}
					}
	
				}
				else // useHashPartitioning
				{
					//TODO: IMPLEMENT THIS LATER 
				}
				
				if(DEBUG)
				{
					System.out.println(triple.getSubject() + "; " + triple.getPredicate()+"; " + triple.getObject());
				}

			}//end while


		}//end for

		//No longer need the primary map, lets get rid of it
		for (int i = 0; i < primary.length; i++) {
			primary[i] = null; 
		}
		
		
		//TODO: Fix this for any # of hops 
		
		if( hops == 2)
		{
			System.out.println("calculating hop 2"); 

			// this collects all nodes that are additionally replicated to this partition in this step
			// at the end of this iteration, secondary will be merged with primary (to check if a node already exists in that partition),
			// and this will become the new secondary
			final TLongHashSet[] ternary = new TLongHashSet[numPartitions];
			
			for (int i = 0; i < ternary.length; i++) {
				ternary[i] = new TLongHashSet(); 
			}
			
			
			//Let's read the all the graph files again.
			for (int i = 0; i < args.length; i++) {
				
				final Iterator<Triple> it = useRiot ? 
											RDFDataMgr.loadGraph(args[i]).find(Node.ANY, Node.ANY, Node.ANY) : 
										    load(args[i]).iterator();
				//Iterate though the graph
				while (it.hasNext()) {
					
					final Triple triple = (Triple) it.next();
					
					final Node subNode  = triple.getSubject(); 
					String s = subNode.isURI() ? subNode.getURI() : subNode.getBlankNodeLabel(); 
					
					final Node objNode = triple.getObject(); 
					final String pred = triple.getPredicate().getURI(); 
					
					int id; 
					
					// we need to replicate this triple in the partition where SECONDARY contains the SUBJECT
					// and where SECONDARY contains the OBJECT (for UNDIRECTED replication)
					
					
					
					
					
					
				}
				
				
				
				
			}// end for
			
			
			
			
			
		}



	}

	private static List<Triple> load(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	static void emit(PrintWriter pw, Triple t) {
		pw.println((t.getSubject().isBlank() ? "_:"
				+ t.getSubject().getBlankNodeLabel() : "<"
				+ t.getSubject().toString() + ">")
				+ " <"
				+ t.getPredicate()
				+ "> "
				+ (t.getObject().isLiteral() ? ("\""
						+ t.getObject().getLiteralLexicalForm() + "\"" + (t
						.getObject().getLiteralDatatypeURI() != null ? "^^<"
						+ t.getObject().getLiteralDatatypeURI() + ">" : ""))
						: (t.getObject().isBlank() ? "_:"
								+ t.getObject().getBlankNodeLabel() : "<"
								+ t.getObject().getURI() + ">")) + ".");

	}
	
	
	/**************************************************************************
	 * @param s Node URI or label (if blank) 
	 * @return returns the id of s.
	 */
	private static final int getId(String s) {
		
		long hash = Util.hash64(s);
		int id=longhashes.get(hash);
		if (id == 0)
		{
			id = max;
			longhashes.put(hash,id);
			max++;
//			if (max%10000==0) System.out.println(max+"\t"+hash+"\t"+s);
		}
		return id;	
	}


}
