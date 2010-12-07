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
	private int width;
	private int height;

	/* Constants */
	private double size_factor = 12;
	private static final int VSPACE = 25;
	private static final int HSPACE = 20;
	private static final String FILEPATH = "../data/data.csv";
	private static final int QUERY_NUM = 50;
	private static final int MAX_ROWS = 5;
	
	
	/* Constructor */
	public TagCloud(String feeling, int width, int height) {
		this.feeling = feeling;
		this.width = width;
		this.height = height;
		visInit();
	}
	
	public class visRebuilder extends Thread {
		private String feeling;
		public visRebuilder(String f) {
			feeling = f;
		}
		
		public void run() {
			setFeeling(feeling);
		}
	}
	
	public void setFeelingThread(String feeling) {
		visRebuilder vr = new visRebuilder(feeling);
		vr.start();
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
		initDisplay();
	}
	
	private void buildDataFile() {
		Map<String, Double> topics = Database.getTopics(feeling, QUERY_NUM);
		
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
		r.setHorizontalTextAlignment(prefuse.Constants.LEFT);
        vis.setRendererFactory(new DefaultRendererFactory(r));
        
        buildColors();
        buildSizes();
        initLayout();
        buildLayout();
        setLayout();
	}
	
	
	private void initDisplay() {
        display.setSize(width, height);
        display.addControlListener(new PanControl());
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
		size.setMaximumSize(size_factor);
        
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

        //Initialize Variables
        IntIterator iter = vt.rows();
        double xpos = HSPACE;
        double ypos = 0;
        int row_index = 0;
        
        //Calc X&Y for each word; add to display
        while (iter.hasNext()) {
        	int table_row = iter.nextInt();
        	VisualItem item = vt.getItem(table_row);        	
        	Rectangle2D rect = item.getBounds();
        	double word_width = rect.getWidth();
        	
        	/* If this word will go past the end margin, go to next line */
        	double nextX = xpos + word_width + HSPACE;
        	if (nextX > width) {
        		row_index++;
        		xpos = HSPACE;
        		ypos += VSPACE;
        	}
        	
        	if (row_index >= MAX_ROWS) {
        		item.setVisible(false);
        	} else {
        	/* Set the x and y position of the label */
        		vt.set(table_row, "xpos", xpos + word_width/2);
        		vt.set(table_row, "ypos", ypos);
        	}
        	
        	/* Advance the x position */
        	xpos += word_width + HSPACE;
        	
        }
        
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

		//double newY = graph_height + (height_bounds - tagvis_height)/2;
		
		double newY = height;
		
		double y_offset = newY - curY;
				
		display.pan(0, y_offset);
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
