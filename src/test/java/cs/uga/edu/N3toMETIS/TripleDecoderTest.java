package cs.uga.edu.N3toMETIS;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TripleDecoderTest {

	private ArrayList<String> uriSet; 
	
	@Before
	public void testSetup() {

		uriSet = new ArrayList<String>(); 
		uriSet.add("@base <http://yago-knowledge.org/resource/> .");
		uriSet.add("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .");
		uriSet.add("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		uriSet.add("@prefix dct: <http://purl.org/dc/terms/> .");
		uriSet.add("@prefix db: <http://dbpedia.org/resource/> .");
		uriSet.add("@prefix db: <http://dbpedia.org/resource/> .");
		uriSet.add("@prefix db: <http://dbpedia.org/resource/> .");
		uriSet.add("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		uriSet.add("<Aarhus> <rdf:type> <wordnet_geographical_area_108574314> .");
		uriSet.add("<db:Autism>     <dct:subject>   <db:Category:Autism> .");
		uriSet.add("<db:Anguilla>   <http://www.w3.org/2003/01/geo/wgs84_pos#lat> \"18.220555555555556\"^^<xsd:float> .");
		uriSet.add("<Jim_Merkel>    <hasFamilyName> \"Merkel\"@en .");
		uriSet.add("<Merkelbach>    <hasLatitude>   \"50.63333333333333\"^^<xsd:float> .");
	}
	
	@After
	public void testCleanup()
	{
		uriSet = null; 
	}

	@Test
	public void testDecode() {
		//TODO: Implement this.
	}

	@Test
	public void testValidateURI() {
		
		Iterator<String> iter = uriSet.iterator();
		
		while (iter.hasNext()) {
		  assertEquals(true, TripleDecoder.ValidateURI(iter.next()));
		}
		
	}

}
