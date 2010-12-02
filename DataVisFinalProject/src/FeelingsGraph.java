import java.awt.Color;

import prefuse.Display;
import prefuse.Visualization;


public class FeelingsGraph {

	/* The display, used in the main visualization window */
	public Display display;
	
	/* Internal visualization parameters */
	private Visualization vis;
	
	/* The width and height of the graph */
	private int width;
	private int height;
	
	/* Constructor */
	public FeelingsGraph(int vis_width) {
		
		/* The graph itself is a square of dimension "vis_width" */
		width = vis_width;
		height = vis_width;
		
		graphVisInit();
		setGraphDisplay();
	}
	
	
	private void graphVisInit() {
		vis = new Visualization();
	}
	
	private void setGraphDisplay() {
		display = new Display(vis);
        display.setSize(width, height);
        display.setBackground(Color.LIGHT_GRAY);
	} 
	
	
}
