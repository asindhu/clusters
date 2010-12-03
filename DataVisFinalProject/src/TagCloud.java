import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Map;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.action.layout.SpecifiedLayout;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.data.Table;
import prefuse.data.io.CSVTableReader;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.collections.IntIterator;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;

public class TagCloud {
	
	/* The display, used in the main visualization window */
	public Display display;
	
	/* Internal visualization parameters */
	private Visualization vis;
	private Table table;
	private VisualTable vt;
	private LabelRenderer r;
	private ColorAction text;
	private ActionList color;
	private DataSizeAction size;
	private ActionList layout;
	private SpecifiedLayout grid;
	
	private String feeling;
	private int cols;
	private int numTags;
	private int width_bounds;
	private int height_bounds;
	private int graph_height;
	
	private double width;
	private double height;

	/* Constants */
	private final int vspace = 20;
	private final int hspace = 20;
	private final String filepath = "../data/data.csv";
	
	
	/* Constructor */
	public TagCloud(int vis_width, int vis_height, int cols, int rows, String feeling) {
		this.cols = cols;
		width_bounds = vis_width;
		height_bounds = vis_height - vis_width;
		graph_height = vis_width;
		numTags = cols * rows;
	
		/* Initialize the visualization with the provided feeling */
		visInit(feeling);
		
	}
	
	
	public void setFeeling(String feeling) {
		this.feeling = feeling;
		
		buildDataFile(feeling);
		loadData();
		
		visRebuild();
	}
	
	private void buildDataFile(String feeling) {
		Map<String, Integer> topics = DataFace.getTopics(feeling, numTags);
		
		try {
			DataFace.writeTopicsToCSV(topics, filepath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void visInit(String feeling) {
		vis = new Visualization();
		display = new Display(vis);
		
		this.feeling = feeling;
		buildDataFile(feeling);
		loadData();
		
		buildVis();
		initDisplay();
	}
	
	private void loadData() {
		table = null;
		try {
			table = new CSVTableReader().readTable(filepath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	private void visRebuild() {
		
		/* Reset the visualization */
		vis.reset();
		display.removeAll();
        
		buildVis();
        
		/* Reload all actions */
		vis.repaint();
		
		/* Recenter the display */
		recenterDisplay();
		
    }
	
	private void buildVis() {
		vt = vis.addTable("table", table);
		r = new LabelRenderer("word");
        vis.setRendererFactory(new DefaultRendererFactory(r));
        
        buildColors();
        buildSizes();
        buildLayout();
	}
	
	private void initDisplay() {
        display.setSize(width_bounds, height_bounds);
        display.setBackground(Color.BLACK);
        
        /* Position the tag cloud in the center of the bounds */
        double x_ofs = (width_bounds - width)/2;
        double y_ofs = graph_height + (height_bounds - height)/2;
        display.pan(x_ofs, y_ofs);
	}
	
	private void buildColors() {
		text = new ColorAction("table", VisualItem.TEXTCOLOR, ColorLib.rgb(237, 24, 83));
        
        /* Create, put, and run the action list for the colors */
        color = new ActionList();
        color.add(text);
        vis.putAction("color", color);
        
        Control hoverc = new ControlAdapter() {

        	public void itemEntered(VisualItem item, MouseEvent evt) {
        		item.setTextColor(ColorLib.rgb(255, 255, 255));
        		item.getVisualization().repaint();

        	}

        	public void itemExited(VisualItem item, MouseEvent evt) {
        		item.setTextColor(item.getEndTextColor());
        		item.getVisualization().repaint();

        	}
        };
        
        display.addControlListener(hoverc);
        
        vis.run("color");
	}
	
	private void buildSizes() {
		size = new DataSizeAction("table", "weight");
        vis.putAction("size", size);
		
		/* Run the size action list */
		vis.run("size");
	}
	
	private void buildLayout() {
	
		/* Add columns to the table for the x and y position of each label */
		vt.addColumn("xpos", double.class);
        vt.addColumn("ypos", double.class);
        
        IntIterator iter = vt.rows();
        int table_row, vis_row, i=0;
        VisualItem item;
        Rectangle2D rect;
        double xpos = 0, width, last_width = 0;
        
        double row_width;
        this.width = 0;
        
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
        		/* Save the maximum width if necessary */
        		row_width = xpos + last_width;
        		
        		//System.out.println("row width = " + row_width);
        		
        		if (row_width > this.width) this.width = row_width;
        		
        		xpos = 0;
        		last_width = 0;
        	}
        	
        	i++;
        }
        this.height = vspace * (numTags / cols);
        
        
        /* Create, put, and run the action list for the layout */
        layout = new ActionList();
        grid = new SpecifiedLayout("table","xpos","ypos");
        
        layout.add(grid);
        layout.add(new RepaintAction());
        
        vis.putAction("layout", layout);
        vis.run("layout");
	}
	
	private void recenterDisplay() {
		/* Get current x,y coordinate of display */
		double curX = display.getDisplayX() * -1;
		double curY = display.getDisplayY() * -1;
		
		double newX = (width_bounds - width)/2;
		double newY = graph_height + (height_bounds - height)/2;
		
		double x_ofs = newX - curX;
		double y_ofs = newY - curY;
		
		/*
		System.out.println("cur x = " + curX + ", cur y = " + curY);
		System.out.println("new x = " + newX + ", new y = " + newY);
		System.out.println("x offset = " + x_ofs + ", y offset = " + y_ofs); */ 
		
		display.pan(x_ofs, y_ofs);
	}


}
