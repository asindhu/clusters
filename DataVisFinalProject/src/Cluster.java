import java.util.*;


public class Cluster {

	/* Instance Variables */
	protected String center;
	protected int size;
	protected Map<String, Integer> moons;
	protected Set<Snippet> snippets;
	
	
	/* Constructor */
	public Cluster(String center, int size) {
		this.center = center;
		this.size = size; 
	}
	
	/* Returns the emotion or topic at the center of the cluster*/
	public String getCenter() {
		return center;
	}
	
	/* Returns the size of the center */
	public float getCenterSize() {
		return size;
	}
	
	/* Returns the cluster's moons */
	public Map<String, Integer> getMoons(int top) {
		return moons;
	}
	
	/* Returns the snippets associated with the cluster */
	public  Set<Snippet> getSnippets() {
		return snippets;
	}
	
}
