package cs.uga.edu.N3toMETIS;

import java.util.HashMap;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.datatypes.TypeMapper;

public class TripleDecoder {
	
	static String baseURI;
	static String typeURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"; 
	static Node typeNode = NodeFactory.createURI(typeURI); 
	static HashMap<String,String> prefixes = new HashMap<String,String>();
	
	final static boolean BROKEN_PREFIXES=true;
	
	/* The TypeMapper provides a global registry of known datatypes. 
	 * The datatypes can be retrieved by their URI or from the java class that is 
	 * used to represent them.
	 */
	public static TypeMapper tm = TypeMapper.getInstance();
	
	/*****************************************************************
	 * Need to return string representation of a triple. <s p o> 
	 * If subject is blank we return _:BlankNode_Lable 
	 * If Object is Literal the string representation of the literal + LiteralDatatypeURI (if not null) 
	 * will be returned as part of the string. 
	 * If object is blank node _:BlankNode_Lable will be returned.
	 * Otherwise the Object URI will be returned.
	 * @param t Triple object. 
	 * @return String representation of the triple <s p o>
	 */
	public static String toString(Triple t)
	{
		return (t.getSubject().isBlank()?"_:"+t.getSubject().getBlankNodeLabel():"<"+t.getSubject().toString()+">")+" <"+t.getPredicate()+"> "+(t.getObject().isLiteral()?("\""+t.getObject().getLiteralLexicalForm()+"\""+(t.getObject().getLiteralDatatypeURI()!=null?"^^<"+t.getObject().getLiteralDatatypeURI()+">":"")):(t.getObject().isBlank()?"_:"+t.getObject().getBlankNodeLabel():"<"+t.getObject().getURI()+">"))+" .";
		
	}
	
	
	/*****************************************************************
	 * If the subject is blank it will return _:BlankNode_Lable and Object
	 * representation in string
	 * @param t triple object. 
	 * @return subject and object string representation.
	 */
	public static String getSubjectString(Triple t)
	{
		return (t.getSubject().isBlank()?"_:"+t.getSubject().getBlankNodeLabel():"<"+t.getSubject().toString()+">");
	}
	
	/*****************************************************************
	 * Returns the string representation of the predicate
	 * @param t triple object
	 * @return string representation of the predicate
	 */
	public static String getPredicateString(Triple t)
	{
		return "<"+t.getPredicate()+">";
	}

	/*****************************************************************
	 * Returns the string representation of the object.
	 * @param t triple object
	 * @return string representation of the object.
	 */
	public static String getObjectString(Triple t)
	{
		return (t.getObject().isLiteral()?("\""+t.getObject().getLiteralLexicalForm()+"\""+(t.getObject().getLiteralDatatypeURI()!=null?"^^<"+t.getObject().getLiteralDatatypeURI()+">":"")):(t.getObject().isBlank()?"_:"+t.getObject().getBlankNodeLabel():"<"+t.getObject().getURI()+">"));
	}

	/*****************************************************************
	 * TODO: Write comment for this method.
	 * @param line
	 * @return
	 */
	public static Triple decode(String line)
	{
		
		if(ValidateURI(line))
		{
			String str[] = line.substring(0, line.length()-1).trim().split("\\s+");
			Node s,p,o;
			if (str.length>3)
			{
				for (int i=3;i<str.length;i++)
				{
					str[2] += " " + str[i];
				}
			}
			
			// handling the prefixes here
			if (str[0].compareTo("@prefix")==0)
			{
//				System.out.println(str[1]+"\t"+str[2].substring(1,str[2].length()-1));
				prefixes.put(str[1], str[2].substring(1,str[2].length()-1));
				return null;
			}
			
			//****************************************************************
			//Subject (str[0])
			//****************************************************************
			if((str[0].startsWith("<"))&&(str[0].endsWith(">")))
			{	
				String uri = str[0].substring(1, str[0].length() - 1).trim();
				//subject can not have space in between. 
				if (uri.indexOf(' ') != -1)
				{
					System.err.println("ignore invalid subject uri with blank: "+uri);
					return null;
				}
				
				if ((uri.indexOf(':') == -1) && (baseURI != null))
				{
					// this is not a full uri, so add add base uri
					uri = baseURI + uri;
				}

				//Dealing with broken prefixes to be able to create correct URIs
				if (BROKEN_PREFIXES)
				{
					int idx = uri.indexOf(':');
					String map = prefixes.get(uri.substring(0, idx + 1));
					if (map != null)
						uri = map + uri.substring(idx + 1);
				}
				
				try
				{
					s = NodeFactory.createURI(uri);
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}
			}
			//Blank node 
			else if (str[0].startsWith("_:"))
			{
				try
				{
					// Create a blank node with specified ID.
					s = NodeFactory.createAnon(AnonId.create(str[0].substring(2)));
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}
			}
			else if (str[0].indexOf(':') != -1)
			{
				String uri = str[0];
				int idx = uri.indexOf(':');
				String map = prefixes.get(uri.substring(0, idx + 1));
//					System.out.println(uri.substring(0, idx+1)+"\t"+map);
				
				if (map != null)
				{
					uri = map + uri.substring(idx + 1);
					
					//Can't have space here
					if (uri.indexOf(' ')!=-1)
					{
						System.err.println("ignore invalid uri with blank: " + uri);
						return null;
					}
					
					try
					{
						s = NodeFactory.createURI(uri);
					}
					catch(Exception ex)
					{
						System.err.println(ex);
						return null;
					}
				}
				else
				{
					System.err.println("invalid subject " + str[0] + " (invalid prefix), skip.");
					return null;
				}
			}
			else
			{
				System.err.println("[invalid subject " + str[0] + ", skip.");
				return null;
			}
			
			//****************************************************************
			//Predicate (str[1])
			//****************************************************************
			
			//for < s a o>  
			if (str[1].compareTo("a") == 0) 
			{
				str[1] = "<" + typeURI + ">";
			}
			
			if ((str[1].startsWith("<")) && (str[1].endsWith(">")))
			{
				String uri = str[1].substring(1, str[1].length() - 1);
				
				if (uri.startsWith("dbo"))
				{
					uri = "dbo:" + uri.substring(3);
				}
				
 
				if ((uri.indexOf(':') == -1) && (baseURI != null))
				{
					// this is not a full uri, so add add base uri
					uri = baseURI + uri;
				}

				if (BROKEN_PREFIXES)
				{
					int idx = uri.indexOf(':');
					String map = prefixes.get(uri.substring(0, idx + 1));
					if (map != null)
						uri = map + uri.substring(idx + 1);
				}
				
				try
				{
					p = NodeFactory.createURI(uri);
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}
			}
			else if((str[1].indexOf(':') != -1))
			{
				
				String uri = str[1];
				int idx = uri.indexOf(':');
				String map = prefixes.get(uri.substring(0, idx + 1));
				if (map != null)
				{
					uri = map + uri.substring(idx + 1);
					try
					{
						p = NodeFactory.createURI(uri);
					}
					catch(Exception ex)
					{
						System.err.println(ex);
						return null;
					}
				}
				else
				{
					System.err.println("invalid property " + str[1] + " (invalid prefix), skip.");
					return null;
				}
			}
			else
			{
				System.err.println("invalid property "+str[1]+", skip.");
				return null;
			}
			
			//****************************************************************
			//Object (str[2])
			//****************************************************************
			if ((str[2].startsWith("<"))&&(str[2].endsWith(">")))
			{	
				String uri = str[2].substring(1, str[2].length() - 1).trim();
				if (uri.startsWith("dbo"))
				{
					uri="dbo:" + uri.substring(3);
				}
				
				//TODO: Figure out why we need this? 
				if ((uri.startsWith("<")) && (uri.endsWith(">")))
				{
					uri=uri.substring(1, uri.length() - 1);
				}
				
				if ((uri.indexOf(':') == -1) && (baseURI != null))
				{
					// this is not a full uri, so add add base uri
					uri = baseURI + uri;
				}
				
				if (BROKEN_PREFIXES)
				{
					int idx = uri.indexOf(':');
					String map = prefixes.get(uri.substring(0, idx + 1));
					if (map != null)
						uri = map + uri.substring(idx + 1);
				}
				
				if (uri.indexOf(' ') != -1)
				{
					System.err.println("ignore invalid object uri with blank: " + uri);
					return null;
				}
				try
				{
					o = NodeFactory.createURI(uri);
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}	
			}
			//Blank node
			else if (str[2].startsWith("_:"))
			{
				try
				{
					//Create a blank node with specified id
					o = NodeFactory.createAnon(AnonId.create(str[2].substring(2)));
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}
			}
			else if ((str[2].startsWith("\"")) && (str[2].endsWith("\"")))
			{
				try
				{
					o = NodeFactory.createLiteral(str[2].substring(1, str[2].length() - 1));
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}
			}
			else if ((str[2].startsWith("\"")) && (str[2].lastIndexOf('\"') < str[2].lastIndexOf("^^")))
			{
				String typeuri = str[2].substring(str[2].lastIndexOf("^^") + 2);
				
				if ((typeuri.startsWith("<") && (typeuri.endsWith(">"))))
				{
					typeuri = typeuri.substring(1, typeuri.length() - 1);
										
					if (BROKEN_PREFIXES)
					{
						int idx = typeuri.indexOf(':');
						String map = prefixes.get(typeuri.substring(0, idx + 1));
						if (map != null)
						{
							typeuri = map + typeuri.substring(idx + 1);
						}
					}

					// if it is a type URI, lets figure out what type?!
					RDFDatatype dt = tm.getTypeByName(typeuri);
					
					try
					{
						o = NodeFactory.createLiteral(str[2].substring(1, str[2].lastIndexOf("^^")-1), dt);
					}
					catch(Exception ex)
					{
						System.err.println(ex);
						return null;
					}
//						else
//						{
//							System.err.println("cannot parse object "+str[2]+", skip.");
//							line=r.readLine();
//							continue;							
//						}
				}
				else if (typeuri.indexOf(':') != -1)
				{
					int idx = typeuri.indexOf(':');
					String map=prefixes.get(typeuri.substring(0, idx + 1));
					if (map != null)
					{
						typeuri = map + typeuri.substring(idx + 1);
						RDFDatatype dt = tm.getTypeByName(typeuri);
						try
						{
							o = NodeFactory.createLiteral(str[2].substring(1, str[2].lastIndexOf("^^") - 1), dt);
						}
						catch(Exception ex)
						{
							System.err.println(ex);
							return null;
						}

					}
					else
					{
						System.err.println("invalid object "+ str[2] + " (invalid prefix in type), skip.");
						return null;
					}
				}
				else
				{
					System.err.println("invalid type in object " + str[2] + ", skip.");
					return null;
				}
			}
			else if ((str[2].charAt(0)!='\"') && (str[2].indexOf(':') != -1))
			{
				String uri = str[2];
				int idx = uri.indexOf(':');
				String map = prefixes.get(uri.substring(0, idx + 1));
				if (map != null)
				{
					uri = map+uri.substring(idx + 1);
					try
					{
						o = NodeFactory.createURI(uri);
					}
					catch(Exception ex)
					{
						System.err.println(ex);
						return null;
					}
				}
				else
				{
					System.err.println("invalid object " + str[2] + " (invalid uri prefix), skip.");
					return null;
				}
			}
			else if ((str[2].startsWith("\"")) && (str[2].lastIndexOf('\"') < str[2].lastIndexOf('@')))
			{
				// this is a String with a language identifier
				
				int idx = str[2].lastIndexOf('@');
				try
				{
					o = NodeFactory.createLiteral(str[2].substring(1,idx-1), str[2].substring(idx + 1, str[2].length()),false);
				}
				catch(Exception ex)
				{
					System.err.println(ex);
					return null;
				}

//					System.out.println(o.toString());
			}
			else if ((Character.isDigit(str[2].charAt(0))||(str[2].charAt(0)=='-')))
			{
				// special case for Turtle integers that do not require quotes (needed for the YAGO2 dump)
				o=NodeFactory.createLiteral(str[2]);
			}
			else if ((str[2].compareTo("true")==0)||(str[2].compareTo("false")==0))
			{
				// special case for Turtle booleans that do not require quotes (needed for the YAGO2 dump)
				o=NodeFactory.createLiteral(str[2]);
			}
			else
			{
				System.err.println("invalid object "+str[2]+" (literal not in quotes), skip.");
				return null;
			}
			
			return new Triple(s, p, o);
		}// if valid URI
		else
		{
			return null; 
		}
	}
	
	/*****************************************************************
	 * @param line Line of N3
	 * @return true if the URL is valid, false otherwise.
	 */
	public static boolean ValidateURI(String line)
	{
		if (line.length()>10000)
		{
			System.err.println("ignored long line with "+line.length()+" characters.");
			return false;
		}
		
		if (!line.trim().endsWith("."))
		{
			System.err.println("missing delimiter:\n"+line);
			return false;
		}
		
		if (line.split("\\s+").length != 2 && line.split("\\s+").length < 3)
		{
			System.err.println("ignore short line "+line);
			return false;
		}
		
		
		//Handling the base URI here.
		if ((line.split("\\s+").length == 2)&&(line.split("\\s+")[0].compareTo("@base") == 0))
		{
	
			baseURI = line.split("\\s+")[1].substring(1,line.split("\\s+")[1].length()-1);
			if (!baseURI.endsWith("/")) 
				baseURI += "/";
			return true;
		}
		
		return true;
	}
	
	

}
