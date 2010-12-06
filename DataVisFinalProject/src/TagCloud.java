import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
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
import prefuse.controls.PanControl;
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
	
	/* Instance variables */
	private String feeling;
	private int cols;
	private int rows;
	private int numTags;
	private int width_bounds;
	private int height_bounds;
	private int graph_height;
	
	
	private double tagvis_width;
	private double tagvis_height;

	/* Constants */
	private double SIZE_FACTOR = 12;
	private static final int VSPACE = 25;
	private static final int HSPACE = 10;
	private static final String FILEPATH = "../data/data.csv";
	
	
	/* Constructor */
	public TagCloud(String feeling, int width_bounds, int height_bounds, int cols, int rows) {
		this.feeling = feeling;
		this.width_bounds = width_bounds;
		this.graph_height = width_bounds;
		this.height_bounds = height_bounds - graph_height;
		this.cols = cols;
		this.rows = rows;
		this.numTags = cols * rows;
		visInit();		
	}
	
	/* Public method to change the "feeling" for the tagcloud */
	public void setFeeling(String feeling) {
		this.feeling = feeling;
		buildDataFile();
		loadData();
		visRebuild();
	}
	
	/* Initialize the visualization with the provided feeling */
	private void visInit() {
		vis = new Visualization();
		display = new Display(vis);
	
		buildDataFile();
		loadData();
		buildVis();
		scale();
		initDisplay();
	}
	
	private void buildDataFile() {
		Map<String, Double> topics = Database.getTopics(feeling, numTags);
		
		try {
			writeTopicsToCSV(topics, FILEPATH);
		} catch (IOException e) {e.printStackTrace();}
	}
	

	private void loadData() {
		table = null;
		try {
			table = new CSVTableReader().readTable(FILEPATH);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	}	

	
	private void scale() {
		while (tagvis_width > width_bounds) {
			SIZE_FACTOR -= 0.5;
		}
		visRebuild();
	}
	
	
	private void visRebuild() {
		vis.reset();		//Reset the visualization
		display.removeAll();    
		buildVis();
		vis.repaint();		//Reload all actions
		centerDisplay(); 	//Recenter the display
    }
	
	
/* ****************************************************************************************************** */
// Sets up parameters for the visualization	
/* ****************************************************************************************************** */
	
	private void buildVis() {
		vt = vis.addTable("table", table);
		r = new LabelRenderer("word");
        vis.setRendererFactory(new DefaultRendererFactory(r));
        
        buildColors();
        buildSizes();
        initLayout();
        buildLayout();
        setLayout();
	}
	
	
	private void initDisplay() {
        display.setSize(width_bounds, height_bounds);
        display.setBackground(Color.BLACK);
        centerDisplay();
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
        size.setMaximumSize(SIZE_FACTOR);
		vis.putAction("size", size);
		vis.run("size");  // Run the size action list
	}	
	
	
	private void initLayout() {
		/* Add columns to the table for the x and y position of each label */
		vt.addColumn("xpos", double.class);
        vt.addColumn("ypos", double.class);
        buildLayout();
	}
	
/* ****************************************************************************************************** */
// Generates Layout for the Visualization	
/* ****************************************************************************************************** */
	
	private void buildLayout() {  
        /* Sum the widths of all words and divide by the number of rows to get
         * a lower bound for the width of each row.
         */
        
        IntIterator iter = vt.rows();
        double total_width = HSPACE, width, avg_width;
        int table_row;
        VisualItem item;
        Rectangle2D rect;
        
        while (iter.hasNext()) {
        	table_row = iter.nextInt();
        	item = vt.getItem(table_row);        	
        	rect = item.getBounds();
        	width = rect.getWidth();
        	
        	total_width = total_width + width + HSPACE;
        }
        
        avg_width = total_width/rows;
                
        /* Now calculate the x and y position of each word in the layout */
        iter = vt.rows();
        double xpos = 0, ypos = 0, max_row_width = 0, row_width, last_width = 0;
        int row_index = 0;
        
        while (iter.hasNext()) {
        	table_row = iter.nextInt();
        	item = vt.getItem(table_row);        	
        	rect = item.getBounds();
        	width = rect.getWidth();
        	
        	/* Advance the x position */
        	xpos = xpos + width/2 + HSPACE + last_width/2;
        	
        	/* Set the x and y position of the label */
        	vt.set(table_row, "xpos", xpos);
        	vt.set(table_row, "ypos", ypos);
        	
        	last_width = width;
        	
        	/* If the new x position is over the avg row width,
        	 * we reset to a new row.
        	 */
        	
        	if (xpos > avg_width) {
        		/* Set max row width */
        		row_width = xpos + width/2 + HSPACE;
        		if (row_width > max_row_width) max_row_width = row_width;
        		row_index++;
        		xpos = 0;
        		last_width = 0;
        		ypos = row_index * VSPACE;
        	}
        }
        
        this.tagvis_height = VSPACE * rows;
        this.tagvis_width = max_row_width;
	}
	
	private void setLayout() {
		/* Create, put, and run the action list for the layout */
        layout = new ActionList();
        grid = new SpecifiedLayout("table","xpos","ypos");
        
        layout.add(grid);
        layout.add(new RepaintAction());
        
        vis.putAction("layout", layout);
        vis.run("layout");
	}
	
	
	private void centerDisplay() {
		/* Get current x,y coordinate of display */
		double curX = display.getDisplayX() * -1;
		double curY = display.getDisplayY() * -1;
				
		double newX = (width_bounds - tagvis_width)/2;
		double newY = graph_height + (height_bounds - tagvis_height)/2;
		
		double x_ofs = newX - curX;
		double y_ofs = newY - curY;
				
		display.pan(x_ofs, y_ofs);
	}


/* ****************************************************************************************************** */
// Tag cloud file building functions
/* ****************************************************************************************************** */
		
		public static void writeTopicsToCSV(Map<String, Double> topics, String filename) throws IOException {
			FileWriter fstream = new FileWriter(filename);
	        BufferedWriter out = new BufferedWriter(fstream);
			
	        out.write("word,weight\n");  //CSV header
	        Set<String> keys = topics.keySet();
			for (String key: keys) {
				int weight = (int)((double) topics.get(key));
				if (weight == 0) weight = 1;   //Rectify any zero values
				weight = (int)(Math.log1p(weight)*100);  //Exponential Weighting
				out.write(key + "," + weight + "\n");
			}
			out.close();
		}
}
