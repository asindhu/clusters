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
	
	private static final int QUERY_SIZE = 35000;
	private static final String xmlfile = "output.xml";
	private static HashSet<String> stopwords;

	public static void init() {
		/* Build stop words list */
		buildStopWords("stopwords_withfeelings.txt");
		
		/* Generate the XML file with the latest data, for use to get the top feelings
		 * and top topics to populate the initial display.
		 */
		String queryURL = "http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling&limit=" + QUERY_SIZE + "&password=sepistheman";
		//generateDataFile(queryURL);
		
		/* Right now these are just returning everything so we can see the whole list. Next thing to do
		 * is change the getTop methods to return only the top N number (where N is the supplied argument)
		 */
		Map<String, Integer> topFeelings = getTopFeelings(10);
		Map<String, Integer> topTopics = getTopTopics(10);
		
		//Map<String, Integer> feelings = getFeelings("obama");
		Map<String, Integer> topics = getTopics("grumpy");
		
		printMap(topics);
	}
	
	/* Returns WeFeelFine query results for a given Topic */
	public static Map<String, Integer> getFeelings(String topic) {
		
		/* Generate XML data to get the feelings for this given topic */
		String queryURL = "http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling&contains=" + topic + "&limit=" + QUERY_SIZE + "&password=sepistheman";
		generateDataFile(queryURL);
		
		/* Parse the XML data */
		Map<String, Integer> feelings = parseFeelings(xmlfile);
		
		return feelings;
	}
	
	/* Returns WeFeelFine query results for a given Feeling */
	public static Map<String, Integer> getTopics(String feeling) {
		
		/* Generate XML data to get the feelings for this given topic */
		String queryURL = "http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling&feeling=" + feeling + "&limit=" + QUERY_SIZE + "&password=sepistheman";
		generateDataFile(queryURL);
		
		/* Parse the XML data */
		Map<String, Integer> topics = parseTopics(xmlfile);
		return topics;
	}
	
	/* Returns the top Feelings and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopFeelings(int top) {
		//Map<String, Integer> topFeelings = new HashMap<String, Integer>();

		Map<String, Integer> topFeelings = parseFeelings(xmlfile);
		//sort map and return top 
		
		return topFeelings;
	}
	
	/* Returns the top Topics and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopTopics(int top) {
		//Map<String, Integer> topTopics = new HashMap<String, Integer>();
		
		Map<String, Integer> topTopics = parseTopics(xmlfile);
		
		// sort map and return top
		
		return topTopics;
	}
	
	
	
	/* Main method to execute file */
	public static void main(String[] args) throws Exception {
		DataFace.init();
	}
	
	
	
	
	
/* ****************************************************************************************************** */
// Code below performs query and XML parsing
/* ****************************************************************************************************** */
	
	/* Build stopwords list */
	private static void buildStopWords(String filename) {
		try {
			stopwords = new HashSet<String>();

			/*  Sets up a file reader to read the file */
			FileReader input = new FileReader(filename);

			/* Filter FileReader through a Buffered read to read a line at a time */
			BufferedReader bufRead = new BufferedReader(input);

			String line;    // String that holds current file line

			// Read first line
			line = bufRead.readLine();

			// Read through file one line at time.
			while (line != null) {
				stopwords.add(line);
				line = bufRead.readLine();
			}

			bufRead.close();

		} catch (ArrayIndexOutOfBoundsException e) {
			/* If no file was passed on the command line, this exception is
			              generated. A message indicating how to the class should be
			              called is displayed */
			System.out.println("Usage: java ReadFile filename\n");          

		} catch (IOException e) {
			// If another exception is generated, print a stack trace
			e.printStackTrace();
		}
		
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
					fstream = new FileWriter(xmlfile);
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
	
	/* Parse XML file based on feelings */
	private static Map<String, Integer> parseFeelings(String filename) {
		Map<String, Integer> snippets = new HashMap<String, Integer>();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc;
			try {
				doc = docBuilder.parse (new File(filename));
				doc.getDocumentElement().normalize();

		        NodeList list = doc.getElementsByTagName("feeling");
		        int totalItems = list.getLength();

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
		} catch (ParserConfigurationException e) {e.printStackTrace();}
        return snippets;
	}
	
	/* Parse XML file based on topics */
	private static Map<String, Integer> parseTopics(String filename) {
		Map<String, Integer> snippets = new HashMap<String, Integer>();

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc;
			try {
				doc = docBuilder.parse (new File(filename));
				doc.getDocumentElement().normalize();

				NodeList list = doc.getElementsByTagName("feeling");
				int totalItems = list.getLength();

				Element f;
				String sentence;
				int count;
				String word;

				/* Iterate through every data point/snippet in the list */
				for (int s = 0; s < totalItems; s++) {
					f = (Element)list.item(s);
					sentence = f.getAttribute("sentence");

					StringTokenizer st = new StringTokenizer(sentence);

					/* Iterate through each word in the sentence */
					while (st.hasMoreTokens()) {

						word = st.nextToken();

						/* Put the word in the snippets set if it's not in the stopwords list, and
						 * also make sure it's more than one character long */
						if (!stopwords.contains(word) && word.length() > 1) {
							if (snippets.containsKey(word)) {
								count = snippets.get(word) + 1;
								snippets.put(word, count);
							} else {
								snippets.put(word, 1);
							}
						}

					}

				}
			} catch (SAXException e) {e.printStackTrace();
			} catch (IOException e) {e.printStackTrace();}	        
		} catch (ParserConfigurationException e) {e.printStackTrace();}
		return snippets;
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