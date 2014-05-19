/**
 * @author Arsham
 * date: 5/12/2014
 * The purpose of this class is to read the nt file generated by the preprocess.sh script
 * and partition the Triples according to the number of partitions requested by the user.
 *
 */


package cs.uga.edu.Graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.org.apache.bcel.internal.classfile.LineNumber;

import javafarm.demo.Sysout;

public class Partitioner {

	private static int 	   numPartitions 		= 3;
	private static boolean useHashPartitioning  = true;
	private final static int [] serverCap = {1000, 2000, 2000, 2000};
	private static boolean[][] partitionMap;




	private static final boolean DEBUG = true ;

	public static Set<String> findDistinctPredicates(String[] args)
	{
		Set<String> predSet = new HashSet<>(); 

		for(int i = 0 ; i < args.length; i++)
		{
			Path path = Paths.get(args[i]); 
			try (BufferedReader reader = Files.newBufferedReader(path , StandardCharsets.UTF_8)){
				String line = null;
				while ((line = reader.readLine()) != null) 
				{

					String pred = line.substring(0, line.length() - 1).trim().split("\\s+")[1]; 

					// Predicate has to be a URI  
					if(pred.startsWith("<") && pred.endsWith(">"))
					{
						pred = pred.substring(1, pred.length() - 1); 
						if(DEBUG)
							System.out.println(pred);

						if(!predSet.contains(pred))
							predSet.add(pred); 
					}
					else
					{
						System.err.println("[" + path + "] invalid predicate \n " + line + ", skip.");
						continue;
					}
				}
			}catch (IOException e) {
				System.err.println("ERROR: Failed to read the file " + path);
				e.printStackTrace();
			}
		}
		return predSet; 
	}

	/**************************************************************************
	 * @param args
	 */
	public static void partition(String[] args)
	{
		if(numPartitions != serverCap.length)
			return; 
		
		// Getting ready to write triples to each partition file.
		final PrintWriter files[] = new PrintWriter[numPartitions];
		try
		{
			for (int i = 0; i < numPartitions; i++)
				files[i] = new PrintWriter(new File("part"+i+".n3"),"UTF-8");	
		}
		catch(Exception e)
		{
			System.err.println("cannot open output files.");
			e.printStackTrace();
			System.exit(2);
		}

		if(!useHashPartitioning)
		{
			//For all the files in the args[]
			List<List<Long>> predLns = new ArrayList();

			for(int i = 0 ; i < args.length; i++)
			{
				try {

					RandomAccessFile aFile = new RandomAccessFile(args[i], "r");
					if(aFile.length() == 0)
						return;
					
					String line = null; 
					String curntPred = null; 
					int  predStrtLine = 1;
					long predStrtByte = 0; 
					
					int lineNum = 1;
					
					while(aFile.getFilePointer() != aFile.length())
					{
						long fp = aFile.getFilePointer(); 
						line = aFile.readLine(); 
						String pred = line.substring(0, line.length() - 1).trim().split("\\s+")[1]; 
						
						if(DEBUG)
						{
							if(lineNum == 3519)
							{
								System.out.println("---------------------------------------------------");
								System.out.println(lineNum + ": " + fp + ": " + line);
							}
							else 
								System.out.println(lineNum + ": " + fp + ": " + line);
						}
						
						if(curntPred == null)
						{
							curntPred = pred;
							predStrtLine = lineNum;
							predStrtByte = fp;
							lineNum ++; 
						}
						else if(aFile.getFilePointer() == aFile.length())
						{
							List<Long> l = new ArrayList<>();
							l.add((long) predStrtLine); 
							l.add((long) lineNum); 
							l.add((long) (lineNum - predStrtLine ));
							l.add(predStrtByte); 
							predLns.add(l); 
						}
						else if(curntPred != null && !curntPred.equalsIgnoreCase(pred) )
						{
							List<Long> l = new ArrayList<>();
							l.add((long) predStrtLine); 
							l.add((long) lineNum -1); 
							l.add((long) (lineNum - predStrtLine ));
							l.add(predStrtByte); 
							
							predLns.add(l); 
							
							predStrtLine = lineNum;
							curntPred = pred; 
							predStrtByte = fp; 
							lineNum++; 
						}
						else
						{
							lineNum ++ ; 
						}
					}// end while
					
					if(DEBUG)
					{
						for(List<Long> lns : predLns)
						{
							System.out.print("( ");
							for(long l : lns)
							{
								System.out.print(l + ",");
							}
							System.out.println(")");
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}//end for

			//Let's distribute these!
			partitionMap = new boolean[serverCap.length][predLns.size()];
			if(!pack(0, predLns))
			{
				System.err.println("No solution");

			}
			else
			{
				System.out.println("Distributing triples ....");
				// This is where the distribution happens.
				//TODO: we will need to fix this later.
				RandomAccessFile aFile; 
				try {
					aFile = new RandomAccessFile(args[0], "r");
				
				for (int i = 0; i < serverCap.length; i++)
				{
					if(DEBUG) 
						System.out.println("Partition" + i);
					
//					StringBuilder sb = new StringBuilder();
					for (int j = 0; j < predLns.size(); j++)
					{
						if (partitionMap[i][j])
						{
							if(DEBUG)
								System.out.print("item" + j + "(" + predLns.get(j).get(2) + ") ");
							//Now lets. write to these files we opened!
							aFile.seek(predLns.get(j).get(3));
							
							StringBuilder sb = new StringBuilder();
							while(aFile.getFilePointer() < ( j == predLns.size() -1 ?  aFile.length() :predLns.get((j+1)).get(3) ))
							{
								emit(files[i], aFile.readLine()); 
							}

							
							if(DEBUG)
							{
								System.out.println(sb.toString());
								//emit(files[i], sb.toString() ); 
							}
							
							/*if(DEBUG)
							{
								if(j == predLns.size() -1)
								{
									System.out.println(aFile.length());
								}
								else
								{
									System.out.println(predLns.get((j+1)).get(3)); 
								}
							}
							 */
							
						}
					}
					
					System.out.println("Writing to partition " + i + ": " );
//					emit(files[i], sb.toString() ); 
					
					System.out.println(); 
				}
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		else
		{
			for(int i = 0 ; i < args.length; i++)
			{
				Path path = Paths.get(args[i]); 
				try (BufferedReader reader = Files.newBufferedReader(path , StandardCharsets.UTF_8)){
					String line = null;
					while ((line = reader.readLine()) != null) 
					{
						// Let's read the file and distribute the triples based on pred hash.
						String pred = line.substring(0, line.length() - 1).trim().split("\\s+")[1]; 
						if(pred.startsWith("<") && pred.endsWith(">"))
						{
							int partition = (Math.abs(pred.substring(1, pred.length() - 1).hashCode()) % numPartitions);
							emit(files[partition],line);

							if(DEBUG)
							{
								System.out.println(partition + ": " + line);
							}
						}
						else
						{
							System.err.println("Invalid predicate \n " + line + ", skip.");
							continue;
						}
					}//end while

				} catch (IOException e) {
					System.err.println("ERROR: Failed to read the file " + path);
					e.printStackTrace();
				}

			}//end for
		}//end else
	}



	public static boolean pack(int elementIndex, List<List<Long>> elements)
	{ 
		// output the solution if we're done
		if (elementIndex == elements.size())
		{
			// This is just printing the result.
			if(DEBUG)
			{
				for (int i = 0; i < serverCap.length; i++)
				{
					System.out.println("bag" + i);
					for (int j = 0; j < elements.size(); j++)
						if (partitionMap[i][j])
							System.out.print("item" + j + "(" + elements.get(j).get(2) + ") ");
					System.out.println(); 
				}
			}
			return true;
		}

		// otherwise, keep traversing the state tree
		for (int i = 0; i < serverCap.length; i++)
		{
			if (serverCap[i] >= elements.get(elementIndex).get(2))
			{
				partitionMap[i][elementIndex] = true; // put item into bag
				serverCap[i] -= elements.get(elementIndex).get(2);
				if (pack(elementIndex + 1, elements))                 // explore subtree
					return true;
				serverCap[i] += elements.get(elementIndex).get(2);  // take item out of the bag
				partitionMap[i][elementIndex] = false;
			}
		}

		return false;

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
				+ " (-hp p | -op p)  file1.n3 [file2.n3 ...]");
		System.out.println("available options:\n==================");
		System.out.println(" -hp, --hash p     : use hash partitioning on predicate and create p partitions");
		System.out.println(" -op, --own p      : use our custom partitioning and create p partitions");
		System.out.println(" -h, --help        : print this help and exit");
	}

	/***************************************************************************
	 * @param pw
	 * @param line
	 */
	private static void emit(PrintWriter pw, String line) {
		pw.println(line); 
	}


	public static void main(String[] args) {

		if (args.length < 3)
		{
			usage();
			System.exit(1);
		}

		for (int i = 0; i < args.length; i++) {

			if(args[i].equalsIgnoreCase("-hp") || args[i].equalsIgnoreCase("--hash") )
			{
				useHashPartitioning = true; 

				if (args.length<i+2)
				{
					usage();
					System.exit(1);
				}

				numPartitions = Integer.valueOf(args[++i]);
			}
			else if(args[i].equalsIgnoreCase("-op") || args[i].equalsIgnoreCase("--own") )
			{
				useHashPartitioning = false; 

				if (args.length<i+2)
				{
					usage();
					System.exit(1);
				}

				numPartitions = Integer.valueOf(args[++i]);
			}
			else if (args[i].startsWith("-"))
			{
				usage();
				System.exit(1);
			}
			else
			{
				System.out.println("--------------------------------------------------------");
				//String s[] = {"University1_1.Short.SR.PREP.nt"}; 
				for(String str :findDistinctPredicates(Arrays.copyOfRange(args, i, args.length)).toArray(new String[0]))
				{
					System.out.println(str + " p = " + Math.abs(str.hashCode())%numPartitions);

				}

				partition(Arrays.copyOfRange(args, i, args.length));
			}			
		}



		/*RandomAccessFile aFile;
		String line = null;
		try {
			aFile = new RandomAccessFile("University1_1.SR.PREP.nt", "r");

			long temp = aFile.getFilePointer();
			for(int i = 0 ; i < 4; i++)
			{
				aFile.readLine(); 

			}
			long pos = aFile.getFilePointer(); 
			line = aFile.readLine(); 
			System.out.println("Byte Pos: " + pos + " line: " + line);

			aFile.seek(pos);
			System.out.println("Byte Pos: " + pos + " line: " + aFile.readLine());





		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		/*
		 * ( 1,209,209,0,)
		 * ( 210,246,37,30134,)
		 * ( 247,247,1,34709,)
		 * ( 248,284,37,34709,)
		 * ( 285,870,586,39363,)
		 * ( 871,1570,700,114748,)
		 * ( 1571,1582,12,230110,)
		 * ( 1583,3230,1648,231644,)
		 * ( 3231,3339,109,465515,)
		 * ( 3340,3366,27,480498,)
		 * ( 3367,3518,152,484381,)
		 */
		/*RandomAccessFile aFile;
		String line = null;
		int lineNum = 1; 
		try {
			aFile = new RandomAccessFile("University1_1.SR.PREP.nt", "r");
			System.out.println("File Size: " + aFile.length());
		
			while (aFile.getFilePointer() < aFile.length())
			{
				long fp = aFile.getFilePointer(); 
				line = aFile.readLine();
				if(lineNum == 3519)
				{
					System.out.println("---------------------------------------------------");
					System.out.println(lineNum + ": " + fp + ": " + line);
				}
				else 
					System.out.println(lineNum + ": " + fp + ": " + line);
				
				lineNum++;
			}
			System.out.println(aFile.getFilePointer() + " = " + aFile.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/


	}





}
