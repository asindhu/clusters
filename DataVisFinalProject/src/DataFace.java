import java.util.*;


public class DataFace {

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
		//CODE GOES HERE
		return topFeelings;
	}
	
	/* Returns the top Topics and their frequency to populate initial clusters */
	public static Map<String, Integer> getTopTopics(int top) {
		Map<String, Integer> topTopics = new HashMap<String, Integer>();
		//CODE GOES HERE
		return topTopics;
	}
	
}