import java.io.*;
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
	
	private static final int QUERY_SIZE = 1500;
	private static final String xmlfile = "output.xml";
	private static Set<String> stopwords;
	private static NodeList generalList;
	
	/* Returns WeFeelFine query results for a given Topic */
	public static Map<String, Integer> getFeelings(String topic) {
		generateXMLFile("&contains=" + topic);
		NodeList list = getNodeListFromXMLFile(xmlfile);
		Map<String, Integer> feelings = getFeelingsFromNodeList(list);		
		return feelings;
	}
	
	
	/* Returns WeFeelFine query results for a given Feeling */
	public static Map<String, Integer> getTopics(String feeling) {
		generateXMLFile("&feeling=" + feeling);
		NodeList list = getNodeListFromXMLFile(xmlfile);
		Map<String, Integer> topics = getTopicsFromNodeList(list);		
		return topics;
	}
	
	
	/* Returns the top Feelings and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopFeelings(int top) {
		Map<String, Integer> topFeelings = getFeelingsFromNodeList(generalList);
		topFeelings = getTopNum(topFeelings, top);
		return topFeelings;
	}
	
	
	/* Returns the top Topics and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopTopics(int top) {
		Map<String, Integer> topTopics = getTopicsFromNodeList(generalList);
		topTopics = getTopNum(topTopics, top);
		return topTopics;
	}
	
	
	/* Main method to execute file */
	public static void main(String[] args) throws Exception {
		DataFace.init();
		Map<String, Integer> topFeelings = getTopFeelings(10);
		Map<String, Integer> topTopics = getTopTopics(10);
		Map<String, Integer> topics = getTopics("disappointed");
		printMap(topFeelings);
	}
	
	
	
/* ****************************************************************************************************** */
// Initializes the DataFace class by building stopwords and doing general query
/* ****************************************************************************************************** */
	
	public static void init() {
		//Build stopword list
		buildStopWords("stopwords_withfeelings.txt");
		
		//Build nodelist for general query
		generateXMLFile("");
		generalList = getNodeListFromXMLFile(xmlfile);
	}
	
	/* Build stopwords list */
	private static void buildStopWords(String filename) {
		try {
			stopwords = new HashSet<String>();
			FileReader input = new FileReader(filename);
			BufferedReader buffer = new BufferedReader(input);

			String line = buffer.readLine();
			while (line != null) {
				stopwords.add(line);
				line = buffer.readLine();
			}
			buffer.close();
		} 
		catch (IOException e) {e.printStackTrace();	}
	}

	
	
/* ****************************************************************************************************** */
// Code below performs query and XML parsing
/* ****************************************************************************************************** */
		
	
	/* Generates an XML file with general query */
	private static void generateXMLFile(String query) {
		String url = "http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling"+ query +"&limit=" + QUERY_SIZE + "&password=sepistheman";
		try {
			URL data = new URL(url);
			BufferedReader input = new BufferedReader(new InputStreamReader(data.openStream()));
			FileWriter stream = new FileWriter("output.xml");
			BufferedWriter output = new BufferedWriter(stream);
			
			while (true) {
				String inputLine = input.readLine();
				if (inputLine == null) break;
				output.write(inputLine + "\n");
			}
			input.close();
			output.close();
		} 
		catch (MalformedURLException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
	}
	
	/* Generate NodeList */
	private static NodeList getNodeListFromXMLFile(String filename) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse (new File("output.xml"));
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("feeling");
			return list;
		} 
		catch (SAXException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();} 
		catch (ParserConfigurationException e) {e.printStackTrace();}
		return null;
	}
	
	
	/* Generates a histogram for "feelings" */
	private static Map<String, Integer> getFeelingsFromNodeList(NodeList list) {
		Map<String, Integer> histogram = new HashMap<String, Integer>();        
        for (int s = 0; s < list.getLength(); s++) {
        	Element element = (Element)list.item(s);
        	String feeling = element.getAttribute("feeling");
        
        	if (histogram.containsKey(feeling)) {
        		int count = histogram.get(feeling) + 1;
        		histogram.put(feeling, count);
        	} else {
        		histogram.put(feeling, 1);
        	}
        }
        return histogram;
	}
	
	/* Generates a histogram for "topics" */
	private static Map<String, Integer> getTopicsFromNodeList(NodeList list) {
		Map<String, Integer> histogram = new HashMap<String, Integer>();
		
		for (int s = 0; s < list.getLength(); s++) {
			Element element = (Element) list.item(s);
			String sentence = element.getAttribute("sentence");
			StringTokenizer tokenizer = new StringTokenizer(sentence);

			while (tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken();
				if (!stopwords.contains(word) && word.length() > 1) {
					if (histogram.containsKey(word)) {
						int count = histogram.get(word) + 1;
						histogram.put(word, count);
					} else {
						histogram.put(word, 1);
					}
				}
			}
		}
		return histogram;
	}


/* ****************************************************************************************************** */
// Returns a sorted hash map
/* ****************************************************************************************************** */
	
	private static Map<String, Integer> getTopNum(final Map<String, Integer> histogram, int num) {
		ArrayList<String> arr = new ArrayList<String>(histogram.keySet());
		Collections.sort(arr, new Comparator<String>() {  
			public int compare(String a, String b) {    
				return histogram.get(b) - histogram.get(a);  
			}  
		});  
		
		Map<String, Integer> tops = new HashMap<String, Integer>();
		for (int i = 0; i < num; i++) {
			String key = arr.get(i); 
			tops.put(key, histogram.get(key)); 
		}		
		return tops;
	}
	
	
	/* Print a hashmap in sorted order */
	private static void printMap(Map<String, Integer> map) {
		ArrayList as = new ArrayList(map.entrySet());  

		Collections.sort( as , new Comparator() {  
			public int compare( Object o1 , Object o2 )  
			{  
				Map.Entry e1 = (Map.Entry)o1 ;  
				Map.Entry e2 = (Map.Entry)o2 ;  
				Integer first = (Integer)e1.getValue();  
				Integer second = (Integer)e2.getValue();  
				return first.compareTo( second );  
			}  
		});  

		Iterator i = as.iterator();  
		while ( i.hasNext() )  
		{  
			System.out.println( (Map.Entry)i.next() );  
		} 
	}
}