package cs.uga.edu.N3toMETIS;

public class METISPartitioner {

	private static boolean useDirectHops = true;		//Using directed hop (DEFAULT)
	private static boolean useHashPartitioning = false;
	private static Integer numPartitions = 2;
	 




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
				+ METISPartitioner.class.getName()
				+ " (-p p | -g file) [-d | -u] file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -p, --hash p     : use hash partitioning on subject and create p partitions");
		System.out.println(" -g, --graph file : use graph-based partitioning stored in file");
		System.out.println(" -d, --directed   : directed hops (default)");
		System.out.println(" -u, --undirected : undirected hops (untested)");
		//System.out.println(" -h, --hops h     : use h hops ");
		//System.out.println(" -l, --literals   : graph includes literals (experimental)");
		System.out.println(" --help           : print this help and exit");
	}







}
