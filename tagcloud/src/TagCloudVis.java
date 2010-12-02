import java.awt.Color;
import java.awt.geom.Point2D;
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

public class TagCloudVis extends JPanel {

	/* Global visualization parameters */
	private static Visualization vis;
	private static Display display;
	private static Table table;
	private static VisualTable vt;
	private static LabelRenderer r;
	
	/* Constants */
	private static final int vspace = 20;
	private static final int hspace = 20;
	private static final int cols = 8;
	private static final int vis_width = 720;
	private static final int vis_height = 500;
	
	
	private static void loadData(String filepath) {
		table = null;
		try {
			table = new CSVTableReader().readTable(filepath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	private static void visInit() {
		/* Create the visualization */
		vis = new Visualization();
        vt = vis.addTable("table", table); 
        
        /* Create the label renderer for the word labels */
        r = new LabelRenderer("word");
        vis.setRendererFactory(new DefaultRendererFactory(r));
	}
	
	private static void setColors() {
		ColorAction text = new ColorAction("table", VisualItem.TEXTCOLOR, ColorLib.gray(0));
        
        /* Create, put, and run the action list for the colors */
        ActionList color = new ActionList();
        color.add(text);
        vis.putAction("color", color);
        
        vis.run("color");
	}
	
	private static void setSizes() {
		DataSizeAction size = new DataSizeAction("table", "weight");
        vis.putAction("size", size);
		
		/* Run the size action list */
		vis.run("size");
	}
	
	private static void setLayout() {
	
		/* Add columns to the table for the x and y position of each label */
		vt.addColumn("xpos", double.class);
        vt.addColumn("ypos", double.class);
        
        IntIterator iter = vt.rows();
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
        	vis_row = i / cols;
        	
        	/* Calculate the width of the current label and the new y position */
        	item = vt.getItem(table_row);        	
        	rect = item.getBounds();
        	width = rect.getWidth();
        	xpos += last_width / 2 + width / 2 + hspace;
        		
        	/* Set the x and y position of the label */
        	vt.set(table_row, "xpos", xpos);
        	vt.set(table_row, "ypos", vis_row * vspace);        	
        	
        	/* Save the last label width for next time */
        	last_width = rect.getWidth();
        	
        	/* When we are at the end of a row in the visualization, reset horizontal positioning */
        	if ((i+1) % cols == 0) {
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
        
        vis.putAction("layout", layout);
        vis.run("layout");
	}
	
	private static void setDisplay() {
		display = new Display(vis);
        display.setSize(vis_width, vis_height);
        display.addControlListener(new PanControl());
	}
	
	private static void setFrame() {
		JFrame frame = new JFrame("T a g   C l o u d");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(display);
		frame.pack(); 
		frame.setVisible(true); 
	}
	
	private static void launchVis() {
		visInit();
        setColors();
        setSizes();
        setLayout();
        setDisplay();
        setFrame();
	}

	public static void main(String[] args) {
        
		loadData("../data/data.csv");
			
        launchVis();
    }
	
}
