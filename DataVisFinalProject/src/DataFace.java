import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;


public class DataFace {
	
	/* TODO: we MUST do a check inside the "getTopics" method or any other method that relies on pulling
	 * a feeling query. The API does not check whether the provided feeling is valid or not, so we have
	 * to check whether the provided feeling is valid and is on the list.
	 */
	
	private static final int QUERY_SIZE = 1000;
	private static final String xmlfile = "output.xml";
	
	/* Factor by which tag cloud weights are normalized */
	private static final int normal_factor = 5;
	
	/* Exponential factor for tag cloud weights */
	private static final double exp_factor = 1;
	
	private static Set<String> stopwords;
	private static Set<String> stopfeelings;
	private static Map<String, Double> benchmarkFeelings;
	private static Map<String, Double> benchmarkWords;
	
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
		generateXMLFile("&feeling=" + feeling);
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
			
	
/* ****************************************************************************************************** */
// Initializes the stopwords list
/* ****************************************************************************************************** */
	
	public static void init() {
		benchmarkFeelings = buildFeelingsBenchmark("feelings_benchmark.txt");
		benchmarkWords = buildWordsBenchmark("words_benchmark.txt");
		stopwords = buildStopwords("stopwords_withfeelings.txt");
	}
	
	/* Build stopwords list */
	private static Set<String> buildStopwords(String filename) {
		Set<String> set = new HashSet<String>();
		try {
			FileReader input = new FileReader(filename);
			BufferedReader buffer = new BufferedReader(input);

			String line = buffer.readLine();
			while (line != null) {
				set.add(line);
				line = buffer.readLine();
			}
			buffer.close();
		} 
		catch (IOException e) {e.printStackTrace();	}
		return set;
	}
	
	
	private static Map<String, Double> buildFeelingsBenchmark(String filename) {
		Map<String, Double> histogram = new HashMap<String, Double>();
		try {
			FileReader input = new FileReader(filename);
			BufferedReader buffer = new BufferedReader(input);

			String line = buffer.readLine();
			while (line != null) {
				StringTokenizer tokenizer = new StringTokenizer(line);
				
				String word = tokenizer.nextToken();
				Double frequency = Double.parseDouble(tokenizer.nextToken());
				histogram.put(word, frequency);
				
				line = buffer.readLine();
			}
			buffer.close();
		} 
		catch (IOException e) {e.printStackTrace();	}
		return histogram;
	}

	
	private static Map<String, Double> buildWordsBenchmark(String filename) {
		Map<String, Double> histogram = new HashMap<String, Double>();
		try {
			FileReader input = new FileReader(filename);
			BufferedReader buffer = new BufferedReader(input);

			String line = buffer.readLine();
			while (line != null) {
				StringTokenizer tokenizer = new StringTokenizer(line);
				
				String word = tokenizer.nextToken();
				Double frequency = Double.parseDouble(tokenizer.nextToken());
				histogram.put(word, frequency);
				
				line = buffer.readLine();
			}
			buffer.close();
		} 
		catch (IOException e) {e.printStackTrace();	}
		return histogram;
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
		int total = 0;
		for (int s = 0; s < list.getLength(); s++) {
			Element element = (Element) list.item(s);
			String sentence = element.getAttribute("sentence");
			StringTokenizer tokenizer = new StringTokenizer(sentence);

			

			while (tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken();
				if (benchmarkWords.keySet().contains(word) && word.length() > 1 && !stopwords.contains(word)) {
					if (histogram.containsKey(word)) {
						int count = histogram.get(word) + 1;
						histogram.put(word, count);
						total++;
					} else {
						histogram.put(word, 1);
					}
				}
			}
		}
		//histogram.put("TOTAL_COUNT", total);
		return histogram;
	}

	
	private static Map<String, Double> getUniqueTopics(Map<String, Integer> histogram) {
		Map<String, Double> factors = new HashMap<String, Double>();
		Set<String> keys = histogram.keySet();
		
		int total = histogram.get("TOTAL_COUNT");
		
		for (String s: keys) {
			if (s == "TOTAL_COUNT") continue;
			double frequencyCurrent = (double) histogram.get(s) / total;
			double frequencyGeneral = benchmarkWords.get(s);
			double factor = frequencyCurrent / frequencyGeneral;
			factors.put(s, factor);
		}
		
		return factors;
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
	
	private static Map<String, Double> getTopFactors(final Map<String, Double> histogram, int num) {
		ArrayList<String> arr = new ArrayList<String>(histogram.keySet());
		Collections.sort(arr, new Comparator<String>() {  
			public int compare(String a, String b) {    
				return (int) (histogram.get(b) - histogram.get(a));  
			}  
		});  
		
		Map<String, Double> tops = new HashMap<String, Double>();
		for (int i = 0; i < num; i++) {
			String key = arr.get(i);
			Double value = histogram.get(key);
			System.out.println(key + ": " + value);
			tops.put(key, value); 
		}		
		return tops;
	}
	
	
/* ****************************************************************************************************** */
// Get Feelings Associated With Feelings
/* ****************************************************************************************************** */
	
	public static Map<String, Integer> getRelatedFeelings(String feeling, int num) {
		generateXMLFile("&feeling=" + feeling);
		NodeList list = getNodeListFromXMLFile(xmlfile);
		Map<String, Integer> feelings = getFeelingsFromFeelingsNodeList(feeling, list);
		if (num != -1) feelings = getTopNum(feelings, num);
		return feelings;
	}
	
	private static Map<String, Integer> getFeelingsFromFeelingsNodeList(String feeling, NodeList list) {
		Map<String, Integer> histogram = new HashMap<String, Integer>();
		
		int total = 0;
		for (int s = 0; s < list.getLength(); s++) {
			Element element = (Element) list.item(s);
			String sentence = element.getAttribute("sentence");
			StringTokenizer tokenizer = new StringTokenizer(sentence);

			while (tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken();
				if (benchmarkFeelings.keySet().contains(word) && !word.equals(feeling) && word.length() > 1) {
					if (histogram.containsKey(word)) {
						int count = histogram.get(word) + 1;
						histogram.put(word, count);
					} else {
						histogram.put(word, 1);
					}
					total++;
				}
			}
		}
		histogram.put("TOTAL_COUNT", total);
		return histogram;
	}
	
	private static Map<String, Double> getUniqueFeelings(Map<String, Integer> histogram) {
		Map<String, Double> factors = new HashMap<String, Double>();
		Set<String> keys = histogram.keySet();
		
		int total = histogram.get("TOTAL_COUNT");
		
		for (String s: keys) {
			if (s == "TOTAL_COUNT") continue;
			double frequencyCurrent = (double) histogram.get(s) / total;
			double frequencyGeneral = benchmarkFeelings.get(s);
			double factor = frequencyCurrent / frequencyGeneral;
			factors.put(s, factor);
		}
		
		return factors;
	}
	
	
/* ****************************************************************************************************** */
// Printing functions
/* ****************************************************************************************************** */
			
		/* Print a hashmap in sorted order */
		private static void printMap(final Map<String, Integer> histogram) {
			if (histogram.isEmpty()) {
				System.out.println("No results!");
				return;
			}
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
		
		private static void printBenchmark(final Map<String, Double> histogram) {
			if (histogram.isEmpty()) {
				System.out.println("No results!");
				return;
			}
			ArrayList<String> arr = new ArrayList<String>(histogram.keySet());  
			Collections.sort(arr, new Comparator<String>() {  
				public int compare(String a, String b) {    
					return (int) (histogram.get(b) - histogram.get(a));  
				}  
			});    

			for (String s: arr) {
				System.out.println(s + ": " + histogram.get(s));  
			} 
		}

/* ****************************************************************************************************** */
// Tag cloud file building functions
/* ****************************************************************************************************** */

		public static void writeTopicsToXML(Map<String, Integer> topics, String filename) throws IOException {
			FileWriter fstream = new FileWriter(filename);
	        BufferedWriter out = new BufferedWriter(fstream);
			
	        /* XML preamble */
			out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			out.write("<graphml xmlns='http://graphml.graphdrawing.org/xmlns'>\n");
			out.write("<graph edgedefault='undirected'>\n");
			out.write("<key id='word' for='node' attr.name='word' attr.type='string'/>\n");
			out.write("<key id='weight' for='node' attr.name='weight' attr.type='integer'/>\n");
			
			/* Create a center node */
			/*
			out.write("<node id='0'>\n");
			out.write("<data key='word'></data>\n");
			out.write("<data key='weight'>1</data>\n");
			out.write("</node>\n");
			*/
			/* Write each "node" into the XML file */
			ArrayList<String> keys = new ArrayList<String>(topics.keySet());
	        String topic;
	        int weight;
	        
			for (int i = 0; i < keys.size(); i++) {
	        	topic = keys.get(i);
	        	weight = topics.get(topic);
	        	
	        	/* Create node */
				out.write("<node id='" + (i+1) + "'>\n");
				out.write("<data key='word'>" + topic + "</data>\n");
				out.write("<data key='weight'>" + weight + "</data>\n");
				out.write("</node>\n");
				
				/* Create edge from center node to this node */
				/*out.write("<edge source='0' target='" + (i+1) + "'></edge>\n");*/
			}
			
			/* XML end */
			out.write("</graph>\n");
			out.write("</graphml>\n");
			
			out.close();
		}
		
		public static void writeTopicsToCSV(Map<String, Integer> topics, String filename) throws IOException {
			ArrayList<String> keys = new ArrayList<String>(topics.keySet());
			ArrayList<Integer> weights = new ArrayList<Integer>();
			
			String topic;
			double weight;
	        double max_weight, min_weight;
			
			/* Iterate through the entries and set the weights */
			for (int i = 0; i < keys.size(); i++) {
	        	topic = keys.get(i);
	        	
	        	/* Exponentially weight the sizes */
	        	//weight = Math.pow(topics.get(topic), exp_factor);
	        	
	        	/* Add the weight to the ArrayList */
	        	weights.add((int)topics.get(topic));

			}
			
			/* Get max weight */
			max_weight = Collections.max(weights);
			
			/* Normalize the weights to a range between 1 and normal_factor */
			/*for (int i = 0; i < weights.size(); i++) {
				weight = weights.get(i);
				
				weight = weight * (normal_factor/max_weight);
				
				weights.set(i, (int)weight);
			}*/
			
			/* Write to file */
			FileWriter fstream = new FileWriter(filename);
	        BufferedWriter out = new BufferedWriter(fstream);
			
	        /* Write CSV header */
	        out.write("word,weight\n");
	        
	        int int_weight;
			
			for (int i = 0; i < keys.size(); i++) {
				topic = keys.get(i);
				int_weight = weights.get(i);
				
				/* Rectify any zero values */
				if (int_weight == 0) int_weight = 1;
				
				/* Write to file */
				out.write(topic + "," + int_weight + "\n");
			}
			
			out.close();
		}
		
/* ****************************************************************************************************** */
// Main method for testing
/* ****************************************************************************************************** */
	
	/* Main method to execute file */
	public static void main(String[] args) throws Exception {
		DataFace.init();

		//Map<String, Integer> testing = getFeelings("unemployment", 10);
		//Map<String, Integer> testing = getTopics("happy", 10);
		//Map<String, Integer> testing2 = getTopics("sad", 10);
		//Map<String, Integer> testing = getRelatedFeelings("happy", 10);
		//printMap(testing);
		//printMap(testing2);
		
		
//		String query = "family";
//		System.out.println("Feelings associated with: " + query);
//		Map<String, Integer> counts = getTopics(query);
//		Map<String, Double> factors = getUniqueTopics(counts);
//		getTopFactors(factors, 10);
		
//		String query = "angry";
//		System.out.println("Feelings associated with: " + query);
//		Map<String, Integer> counts = getRelatedFeelings(query, -1);
//		Map<String, Double> factors = getUniqueFeelings(counts);
//		getTopFactors(factors, 10);
		
		
		Map<String, Integer> topics = getTopics("angry", 24);
		/*writeTopicsToXML(topics, "../data/data.xml");*/
		writeTopicsToCSV(topics, "../data/data.csv");
		
	}
	
}