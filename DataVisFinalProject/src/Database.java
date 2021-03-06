import java.io.*;
import java.util.*;
import java.net.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


public class Database {
	
	/* Instance Variables */
	private static final int QUERY_SIZE = 10000;
	private static final int THRESHOLD = 20;
	private static final String XMLFILE = "output.xml";	
	private static Set<String> stopwords;
	private static Map<String, Double> benchmarkFeelings;
	private static Map<String, Double> benchmarkTopics;
	

	/* Returns the top topics associated with a particular feeling */
	public static Map<String, Double> getTopics(String feeling, int num) {
		return getAssociations(feeling, num, benchmarkTopics, true);
	}
	
	/* Returns the top feelings associated with a particular feeling */
	public static Map<String, Double> getFeelings(String feeling, int num) {
		return getAssociations(feeling, num, benchmarkFeelings, false);
	}
	
	/* Generic code that handles both searches for topics and feelings */
	private static Map<String, Double> getAssociations(String feeling, int num, Map<String, Double> benchmark, boolean filter) {
		generateXMLFile("&feeling=" + feeling);
		NodeList nodelist = getNodeListFromXMLFile(XMLFILE);	
		Map<String, Integer> histogram = getHistogram(nodelist, benchmark);
		Map<String, Double> factors = getFactors(histogram, benchmark);
		if (filter) {
			removeStopwords(factors, benchmark);
		} else {
			factors.remove(feeling);
		}
		if (num != -1) factors = getTopFactors(factors, num);
		return factors;
	}
	
	
	

/* ****************************************************************************************************** */
// Counting
/* ****************************************************************************************************** */

	/* Generates a histogram for words contained in each sentence" */
	private static Map<String, Integer> getHistogram(NodeList list, Map<String, Double> benchmark) {
		Map<String, Integer> histogram = new HashMap<String, Integer>();
		int total = 0;
		for (int s = 0; s < list.getLength(); s++) {
			Element element = (Element) list.item(s);
			String sentence = element.getAttribute("sentence");
			StringTokenizer tokenizer = new StringTokenizer(sentence);

			while (tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken().toLowerCase();
				if (benchmark.keySet().contains(word)) {
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
		return histogram;
	}
	
	private static void removeStopwords(Map<String, Double> factors, Map<String, Double> benchmark) {
		Set<String> keys = benchmark.keySet();
		for (String key: keys) {
			if (stopwords.contains(key)) {
				factors.remove(key);
			}
		}
	}
	
	private static Map<String, Double> getFactors(Map<String, Integer> histogram, Map<String, Double> benchmark) {
		Map<String, Double> factors = new HashMap<String, Double>();
		Set<String> keys = histogram.keySet();
		
		int total = 0;
		for (String key: keys) total += histogram.get(key);
				
		for (String key: keys) {
			int count = histogram.get(key);
			if (count < THRESHOLD) continue;  //Filter out words appearing less than x times
			double frequencyCurrent = (double) count / total;
			double frequencyGeneral = benchmark.get(key);
			double factor = frequencyCurrent / frequencyGeneral;
			factors.put(key, factor);
		}
		
		return factors;
	}
	
	
	private static Map<String, Double> getTopFactors(final Map<String, Double> histogram, int num) {
		ArrayList<String> arr = new ArrayList<String>(histogram.keySet());
		Collections.sort(arr, new Comparator<String>() {  
			public int compare(String a, String b) {    
				return (int) ((histogram.get(b) - histogram.get(a))*100000);  
			}  
		});  
		
		Map<String, Double> tops = new HashMap<String, Double>();
		int range = Math.min(num, histogram.size());
		for (int i = 0; i < range; i++) {
			String key = arr.get(i);
			Double value = histogram.get(key);
			//System.out.println(key + ": " + value);
			tops.put(key, value); 
		}		
		return tops;
	}
		
/* ****************************************************************************************************** */
// Initializes the stopwords lists
/* ****************************************************************************************************** */
	
	public static void init() {
		stopwords = buildStopwords("stopwords.txt");
		benchmarkFeelings = buildBenchmark("benchmark_feelings.txt");
		benchmarkTopics = buildBenchmark("benchmark_nouns.txt");
	}
	
	
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
	
	
	private static Map<String, Double> buildBenchmark(String filename) {
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
	
	
	private static void writeXMLFileForGraph(String feeling, Map<String, Double> histogram, String filename) {
		try {
			FileWriter fstream = new FileWriter(filename);
	        BufferedWriter out = new BufferedWriter(fstream);
	        
	        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
	        out.write("<feelingpairs>\n");
	        Set<String> keys = histogram.keySet();
	        for (String key: keys) {
	        	out.write("<feelingpair feeling='"+ feeling +"' feeling2='"+ key +"'  freq='"+ histogram.get(key) +"'></feelingpair>\n");
	        }
	        out.write("</feelingpairs>\n");
	        out.close();
		}  
		catch (IOException e) {e.printStackTrace();}
	}
	
	private static void writeXMLFileForEmotionWeb(String filename) {
		try {
			FileWriter fstream = new FileWriter(filename);
	        BufferedWriter out = new BufferedWriter(fstream);
	        
	        Set<String> feelings = benchmarkFeelings.keySet();
	        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
	        out.write("<feelingpairs>\n");
	        
	        for (String feeling: feelings) {
	        	Map<String, Double> results = getFeelings(feeling, 8);
	        	Set<String> links = results.keySet();
	        	for (String link: links) {
	        		out.write("<feelingpair feeling='"+ feeling +"' feeling2='"+ link +"'  freq='"+ results.get(link) +"'></feelingpair>\n");
	        	}
	        }
	        out.write("</feelingpairs>\n");
	        out.close();
		}  
		catch (IOException e) {e.printStackTrace();}
	}


/* ****************************************************************************************************** */
// Testing Code
/* ****************************************************************************************************** */	
	
//	public static void main(String [] args) {
//		init();
//		writeXMLFileForEmotionWeb("emotion_database.xml");
//		getTopics("awake", 10);
//		System.out.println();
//		Map<String, Double> results = getFeelings("pissed", 10);
//		writeXMLFileForGraph("wonderful", results, "eddie.txt");
//	}
	
}