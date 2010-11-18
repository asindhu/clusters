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
	
	
	/* Returns the top feelings associated with a particular topic
	 * If no num is provided, it returns all feelings associated with the topic
	 * If no topic is provided, it simply returns the top feelings across all topics
	 */
	public static Map<String, Integer> getFeelings(String topic, int num) {
		generateXMLFile("&contains=" + topic);
		NodeList list = getNodeListFromXMLFile(xmlfile);
		Map<String, Integer> feelings = getFeelingsFromNodeList(list);
		if (num != -1) feelings = getTopNum(feelings, num);
		return feelings;
	}
	
	public static Map<String, Integer> getFeelings(String topic) {
		return getFeelings(topic, -1);
	}
	
	public static Map<String, Integer> getFeelings(int num) {
		return getFeelings("", num);
	}
	
	public static Map<String, Integer> getFeelings() {
		return getFeelings("", -1);
	}
	
	/* Returns the top topics associated with a particular feeling
	 * If no num is provided, it returns all topics associated with the feeling
	 * If no topic is provided, it simply returns the top topics across all feelings
	 */
	public static Map<String, Integer> getTopics(String feeling, int num) {
		generateXMLFile("&contains=" + feeling);
		NodeList list = getNodeListFromXMLFile(xmlfile);
		Map<String, Integer> Topics = getTopicsFromNodeList(list);
		if (num != -1) Topics = getTopNum(Topics, num);
		return Topics;
	}
	
	public static Map<String, Integer> getTopics(String feeling) {
		return getTopics(feeling, -1);
	}
	
	public static Map<String, Integer> getTopics(int num) {
		return getTopics("", num);
	}
	
	public static Map<String, Integer> getTopics() {
		return getTopics("", -1);
	}
		
		
	/* Main method to execute file */
	public static void main(String[] args) throws Exception {
		DataFace.init();
		//Map<String, Integer> testing = getFeelings("china", 10);
		Map<String, Integer> testing = getFeelings("happy", 10);
		printMap(testing);
	}
	
	
	
/* ****************************************************************************************************** */
// Initializes the stopwords list
/* ****************************************************************************************************** */
	
	public static void init() {
		buildStopWords("stopwords_withfeelings.txt");
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
// Returns a hashmap histogram containing the most frequent "num" elements of the provided histogram
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
	private static void printMap(final Map<String, Integer> histogram) {
		ArrayList<String> arr = new ArrayList<String>(histogram.keySet());  
		Collections.sort(arr, new Comparator<String>() {  
			public int compare(String a, String b) {    
				return histogram.get(b) - histogram.get(a);  
			}  
		});    

		for (String s: arr) {
			System.out.println(s + ": " + histogram.get(s));  
		} 
	}
}