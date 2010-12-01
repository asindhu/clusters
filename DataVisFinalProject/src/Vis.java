import java.awt.Color;
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
	
	/* Global visualization parameters: graph */
	private static Visualization graphVis;
	private static Display graphDisplay;
	
	/* Global visualization parameters: tag cloud */
	private static Visualization tagVis;
	private static Display tagDisplay;
	private static Table tagTable;
	private static VisualTable tagVisTable;
	private static LabelRenderer tagLabelRenderer;
	
	/* Constants */
	private static final int vspace = 20;
	private static final int hspace = 20;
	private static final int tag_cols = 8;
	private static final int vis_width = 600;
	private static final int vis_height = 720;
	private static final int tag_height = 100;
	private static final int graph_height = 600;
	private static final int tag_padding = 30;
	
	public static void main(String[] args) {
        
		loadTagData("../data/data.csv");
		
		setupGraphVis();
        setupTagVis();
        setFrame();
    }
	
	private static void setFrame() {
		JFrame frame = new JFrame("C l u s t e r f * c k");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(graphDisplay);
		frame.add(tagDisplay);
		frame.pack(); 
		frame.setSize(vis_width, vis_height);
		frame.setVisible(true); 
	}
	
	/*----------------------------------------
	 * 
	 * GRAPH DISPLAY
	 * 
	 *---------------------------------------*/
	
	/* Method to set up the tag visualization */
	private static void setupGraphVis() {
		graphVisInit();
		setGraphDisplay();
	}
	
	private static void graphVisInit() {
		/* Create the visualization */
		graphVis = new Visualization();
	}
	
	private static void setGraphDisplay() {
		graphDisplay = new Display(graphVis);
        graphDisplay.setSize(vis_width, graph_height);
        graphDisplay.setBackground(Color.LIGHT_GRAY);
	}
	
	/*----------------------------------------
	 * 
	 * TAG CLOUD DISPLAY
	 * 
	 *---------------------------------------*/
	 

	/* Method to set up the tag visualization */
	private static void setupTagVis() {
		tagVisInit();
        setTagColors();
        setTagSizes();
        setTagLayout();
        setTagDisplay();
	}
	
	private static void loadTagData(String filepath) {
		tagTable = null;
		try {
			tagTable = new CSVTableReader().readTable(filepath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	private static void tagVisInit() {
		/* Create the visualization */
		tagVis = new Visualization();
        tagVisTable = tagVis.addTable("table", tagTable); 
        
        /* Create the label renderer for the word labels */
        tagLabelRenderer = new LabelRenderer("word");
        tagVis.setRendererFactory(new DefaultRendererFactory(tagLabelRenderer));
	}
	
	private static void setTagColors() {
		ColorAction text = new ColorAction("table", VisualItem.TEXTCOLOR, ColorLib.gray(0));
        
        /* Create, put, and run the action list for the colors */
        ActionList color = new ActionList();
        color.add(text);
        tagVis.putAction("color", color);
        
        tagVis.run("color");
	}
	
	private static void setTagSizes() {
		DataSizeAction size = new DataSizeAction("table", "weight");
        tagVis.putAction("size", size);
		
		/* Run the size action list */
		tagVis.run("size");
	}
	
	private static void setTagLayout() {
	
		/* Add columns to the table for the x and y position of each label */
		tagVisTable.addColumn("xpos", double.class);
        tagVisTable.addColumn("ypos", double.class);
        
        IntIterator iter = tagVisTable.rows();
        int table_row, vis_row, i=0;
        VisualItem item;
        Rectangle2D rect;
        double xpos = 0, width, last_width = 0;
        
        /* Iterate over the table items and calculate the x and
         * y values for each label.
         */
        while (iter.hasNext()) {
        	/* The row in the backing table corresponding to the current data item */
        	table_row = iter.nextInt();
        	
        	/* The row index in the actual visualization */
        	vis_row = i / tag_cols;
        	
        	/* Calculate the width of the current label and the new y position */
        	item = tagVisTable.getItem(table_row);        	
        	rect = item.getBounds();
        	width = rect.getWidth();
        	xpos += last_width / 2 + width / 2 + hspace;
        		
        	/* Set the x and y position of the label */
        	tagVisTable.set(table_row, "xpos", xpos);
        	tagVisTable.set(table_row, "ypos", vis_row * vspace);        	
        	
        	/* Save the last label width for next time */
        	last_width = rect.getWidth();
        	
        	/* When we are at the end of a row in the visualization, reset horizontal positioning */
        	if ((i+1) % tag_cols == 0) {
        		xpos = 0;
        		last_width = 0;
        	}
        	
        	i++;
        }
        
        /* Create, put, and run the action list for the layout */
        ActionList layout = new ActionList();
        SpecifiedLayout grid = new SpecifiedLayout("table","xpos","ypos");
        layout.add(grid);
        layout.add(new RepaintAction());
        
        tagVis.putAction("layout", layout);
        tagVis.run("layout");
	}
	
	private static void setTagDisplay() {
		tagDisplay = new Display(tagVis);
        tagDisplay.setSize(vis_width, tag_height);
        tagDisplay.setBackground(Color.WHITE);
        tagDisplay.pan(tag_padding, graph_height + tag_padding);
	}
	
}
