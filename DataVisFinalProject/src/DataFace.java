import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.net.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class DataFace {
	
	private static final int QUERY_SIZE = 10000;
	

	/* Returns WeFeelFine query results for a given Topic */
	public static Set<Snippet> getFeelings(String topic) {
		Set<Snippet> feelings = new HashSet<Snippet>();
		//CODE GOES HERE
		return feelings;
	}
	
	/* Returns WeFeelFine query results for a given Feeling */
	public static Set<Snippet> getTopics(String feeling) {
		Set<Snippet> topics = new HashSet<Snippet>();
		//CODE GOES HERE
		return topics;
	}
	
	/* Returns the top Feelings and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopFeelings(int top) {
		Map<String, Integer> topFeelings = new HashMap<String, Integer>();
		
		generateDataFile("http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling&limit=" + QUERY_SIZE + "&password=sepistheman");
		parseXMLFile("output.xml");
		
		
		return topFeelings;
	}
	
	/* Returns the top Topics and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopTopics(int top) {
		Map<String, Integer> topTopics = new HashMap<String, Integer>();
		//CODE GOES HERE
		return topTopics;
	}
	
	/* Generates an XML file with general query */
	private static void generateDataFile(String url) {
		URL data;
		try {
			data = new URL(url);
			BufferedReader input;
			try {
				input = new BufferedReader(new InputStreamReader(data.openStream()));
				FileWriter fstream;
				try {
					fstream = new FileWriter("output.xml");
					BufferedWriter output = new BufferedWriter(fstream);
					
			        String inputLine;
					while (true) {
						inputLine = input.readLine();
						if (inputLine == null) break;
						output.write(inputLine + "\n");
					}
					input.close();
					output.close();
				} catch (IOException e) {e.printStackTrace();}
			} catch (IOException e1) {e1.printStackTrace();}
		} catch (MalformedURLException e) {e.printStackTrace();}
	}
	
	/* Parse XML */
	private static void parseXMLFile(String filename) {
		Map<String, Integer> snippets = new HashMap<String, Integer>();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc;
			try {
				doc = docBuilder.parse (new File("output.xml"));
				doc.getDocumentElement().normalize();

		        NodeList list = doc.getElementsByTagName("feeling");
		        int totalItems = list.getLength();
		        System.out.println("Total no of items : " + totalItems);

		        Element f;
		        String feeling;
		        int count;
		        
		        for (int s = 0; s < totalItems; s++) {
		        	f = (Element)list.item(s);
		        	feeling = f.getAttribute("feeling");
		        
		        	if (snippets.containsKey(feeling)) {
		        		count = snippets.get(feeling) + 1;
		        		snippets.put(feeling, count);
		        	} else {
		        		snippets.put(feeling, 1);
		        	}
		        }
			} catch (SAXException e) {e.printStackTrace();
			} catch (IOException e) {e.printStackTrace();}	        
		} catch (ParserConfigurationException e) {e.printStackTrace();
		}
        
	}
	
	
	
	
	
	/* Main method to execute file */
	public static void main(String[] args) throws Exception {
		DataFace.getTopFeelings(10);
	}
	
	
}