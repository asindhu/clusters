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
	
	private String feeling;
	private int cols;
	private int rows;
	private int numTags;
	private int width_bounds;
	private int height_bounds;
	private int graph_height;
	private double size_factor = 12;
	
	private double tagvis_width;
	private double tagvis_height;

	/* Constants */
	private final int vspace = 25;
	private final int hspace = 10;
	private final String filepath = "../data/data.csv";
	
	
	/* Constructor */
	public TagCloud(int vis_width, int vis_height, int cols, int rows, String feeling) {
		this.cols = cols;
		width_bounds = vis_width;
		
		/* The extra room past the square for the tree is used for the tag cloud */
		height_bounds = vis_height - vis_width;
		graph_height = vis_width;
		numTags = cols * rows;
		this.rows = rows;
	
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
		scale();
		initDisplay();
	}
	
	private void scale() {
		while (tagvis_width > width_bounds) {
			size_factor -= 0.5;
			visRebuild();
		}
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
		centerDisplay();
		
    }
	
	
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
        
        /* Position the tag cloud in the center of the bounds */
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
        size.setMaximumSize(size_factor);
		
		vis.putAction("size", size);
		
		/* Run the size action list */
		vis.run("size");
	}	

	
	private void initLayout() {
		/* Add columns to the table for the x and y position of each label */
		vt.addColumn("xpos", double.class);
        vt.addColumn("ypos", double.class);
        
        buildLayout();
	}
	
	private void buildLayout() {
		
        
        /* Sum the widths of all words and divide by the number of rows to get
         * a lower bound for the width of each row.
         */
        
        IntIterator iter = vt.rows();
        double total_width = hspace, width, avg_width;
        int table_row;
        VisualItem item;
        Rectangle2D rect;
        
        while (iter.hasNext()) {
        	table_row = iter.nextInt();
        	item = vt.getItem(table_row);        	
        	rect = item.getBounds();
        	width = rect.getWidth();
        	
        	total_width = total_width + width + hspace;
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
        	xpos = xpos + width/2 + hspace + last_width/2;
        	
        	/* Set the x and y position of the label */
        	vt.set(table_row, "xpos", xpos);
        	vt.set(table_row, "ypos", ypos);
        	
        	last_width = width;
        	
        	/* If the new x position is over the avg row width,
        	 * we reset to a new row.
        	 */
        	
        	if (xpos > avg_width) {
        		/* Set max row width */
        		row_width = xpos + width/2 + hspace;
        		if (row_width > max_row_width) max_row_width = row_width;
        		row_index++;
        		xpos = 0;
        		last_width = 0;
        		ypos = row_index * vspace;
        	}
        }
        
        this.tagvis_height = vspace * rows;
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


}
