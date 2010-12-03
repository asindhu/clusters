import java.awt.Color;
import java.awt.Component;
import java.awt.geom.Rectangle2D;
import javax.swing.*;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.action.layout.SpecifiedLayout;
import prefuse.controls.PanControl;
import prefuse.data.Table;
import prefuse.data.io.CSVTableReader;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.collections.IntIterator;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;

public class Vis extends JPanel {

	/* Displays */
	private static TagCloud tagcloud;
	private static FeelingsGraph feelingsgraph;
	
	private static JFrame frame;
	
	/* Constants */
	private static final int vis_width = 600;
	private static final int vis_height = 720;
	
	public static void main(String[] args) {
        DataFace.init();
		setupGraphVis();
        setupTagVis();
        setFrame();
    }
	
	
	private static void setFrame() {
		frame = new JFrame("C l u s t e r f * c k");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(feelingsgraph.display);
		frame.add(tagcloud.display);
		frame.pack(); 
		frame.setSize(vis_width, vis_height);
		frame.setVisible(true); 
	}
	
	
	/* Set up the graph visualization */
	private static void setupGraphVis() {
		FeelingsGraph graph = new FeelingsGraph(vis_width);
		Vis.feelingsgraph = graph;
	}
	
	
	/* Set up the tag visualization */
	private static void setupTagVis() {
		TagCloud tagcloud = new TagCloud(vis_width, vis_height, 8, 3, "resentful");
		Vis.tagcloud = tagcloud;
	}
	
	
	
}
