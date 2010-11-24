import java.awt.Color;

import javax.swing.*;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.controls.PanControl;
import prefuse.data.Graph;
import prefuse.data.io.DataIOException;
import prefuse.data.io.GraphMLReader;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;

public class TagCloudVis extends JPanel {

	public static void main(String[] args) {
        
		/* 1: Load data */
		Graph graph = null;
		try {
			graph = new GraphMLReader().readGraph("../data/data.xml");
		} catch (DataIOException e) { 
			e.printStackTrace();
			System.err.println("Error loading graph. Exiting...");
			System.exit(1);
		}
               
        /* 2: Create visualization */
        
        Visualization vis = new Visualization();
        vis.add("graph", graph);
                
        System.out.println(ColorLib.color(Color.black));
        
        /* 3: Add renderer */
        
        LabelRenderer r = new LabelRenderer("word");
        //r.setRenderType(1);
        vis.setRendererFactory(new DefaultRendererFactory(r));
        
        /* 4: Color & layout actions */
        
        ColorAction text = new ColorAction("graph.nodes", VisualItem.TEXTCOLOR, ColorLib.gray(0));
        ActionList color = new ActionList();
        color.add(text);
        vis.putAction("color", color);
        
        DataSizeAction size = new DataSizeAction("graph.nodes", "weight");
        vis.putAction("size", size);
        
        ActionList layout = new ActionList(1);
        layout.add(new ForceDirectedLayout("graph"));
        layout.add(new RepaintAction());
        vis.putAction("layout", layout);
        
        /* 5: Initialize display */
        
        Display display = new Display(vis);
        display.setSize(720, 500);
        display.addControlListener(new PanControl());
		
		/* 6: Create frame & launch */
        
        JFrame frame = new JFrame("prefuse example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(display);
        frame.pack(); 
        frame.setVisible(true); 
        
        vis.run("color");
        vis.run("size");
        vis.run("layout");
    }
	
}
