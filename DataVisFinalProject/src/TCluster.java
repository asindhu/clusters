
public class TCluster extends Cluster {

	/* Constructs a "Topic" cluster */
	public TCluster(String topic, int size) {
		super(topic, size);
		snippets = DataFace.getFeelings(topic);
		//moon = Create moons from snippets
	}
}
