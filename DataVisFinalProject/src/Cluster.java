import java.util.*;


public class Cluster {

	/* Instance Variables */
	private String planet;
	private int size;
	private Map<String, Integer> moons;
	private Set<Snippet> snippets;
	
	
	/* Constructor */
	public Cluster(String feeling, int size) {
		this.planet = feeling;
		this.size = size;
		moons = null;
		snippets = null; 
	}
	
	/* Returns the emotion or topic at the center of the cluster*/
	public String getPlanet() {
		return planet;
	}
	
	/* Returns the size of the center */
	public float getPlanetSize() {
		return size;
	}
	
	
	public Map<String, Integer> getMoons(int top) {
		return moons;
	}
	
	public  Set<Snippet> getSnippets() {
		return snippets;
	}
	
	public void refresh() {
		
	}
	
}
