import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.HoverActionControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.GraphMLReader;
import prefuse.data.parser.DataParser;
import prefuse.data.query.SearchQueryBinding;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.parser.StringParser;

@SuppressWarnings("serial")
public class radialview extends Display {
    
	private String m_label;
	
    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    private static final String linear = "linear";
	
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    
    public static NodeList feelings_database;
    private static Map<String,Node> fnodes;
    
    public static int palette[];

    public static boolean hasFrame;
    
	public radialview(Graph g, String label, boolean frame) {
        super(new Visualization());
        colors_loaded = false;
        hasFrame = frame;
 
        m_label = label;
        
        // -- set up visualization --
        m_vis.add(tree, g);
        m_vis.setInteractive(treeEdges, null, false);
        
        // -- set up renderers --
        m_nodeRenderer = new LabelRenderer(m_label);
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
        m_nodeRenderer.setRoundedCorner(8,8);
        m_nodeRenderer.setHorizontalPadding(3);
        m_nodeRenderer.setVerticalPadding(3);
        
        m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_LINE,Constants.EDGE_ARROW_NONE);
        
        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
        m_vis.setRendererFactory(rf);
        
        // -- set up processing actions --
        
        // colors
        ItemAction nodeColor = new NodeColorAction(treeNodes);
        ItemAction textColor = new TextColorAction(treeNodes);
        m_vis.putAction("textColor", textColor);
        
        ItemAction edgeColor = new EdgeColorAction(treeEdges);
        ItemAction edgeWeight = new EdgeStrokeAction(treeEdges);
        
        FontAction fonts = new FontAction(treeNodes, 
                FontLib.getFont("Tahoma", 10));
        fonts.add("ingroup('_focus_')", FontLib.getFont("Tahoma", 11));
        
        // recolor
        ActionList recolor = new ActionList();
        recolor.add(nodeColor);
        recolor.add(textColor);
        m_vis.putAction("recolor", recolor);
        
        // repaint
        ActionList repaint = new ActionList();
        repaint.add(recolor);
        repaint.add(new RepaintAction());
        m_vis.putAction("repaint", repaint);
        
        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);
        
        // create the tree layout action
        RadialMultiTreeLayout treeLayout = new RadialMultiTreeLayout(tree);
        treeLayout.setAutoScale(false);
        treeLayout.setRadiusIncrement(80.0);
    	
        m_vis.putAction("treeLayout", treeLayout);
        
        CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree);
        m_vis.putAction("subLayout", subLayout);
        
        // create the filtering and layout
        ActionList filter = new ActionList();
        filter.add(new TreeRootAction(tree));
        filter.add(fonts);
        filter.add(treeLayout);
        filter.add(subLayout);
        filter.add(textColor);
        filter.add(nodeColor);
        filter.add(edgeColor);
        filter.add(edgeWeight);
        m_vis.putAction("filter", filter);
        
        // animated transition
        ActionList animate = new ActionList(1500);
        animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new PolarLocationAnimator(treeNodes, linear));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        m_vis.alwaysRunAfter("filter", "animate");
        
        // ------------------------------------------------
        
        // initialize the display
        setSize(600,500);
        setItemSorter(new TreeDepthItemSorter());
        addControlListener(new DragControl());
        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new PanControl());
        addControlListener(new FocusControl(1, "filter"));
        addControlListener(new HoverActionControl("repaint"));
        
        // ------------------------------------------------
        
        // filter graph and perform layout
        m_vis.run("filter");
        
        // maintain a set of items that should be interpolated linearly
        // this isn't absolutely necessary, but makes the animations nicer
        // the PolarLocationAnimator should read this set and act accordingly
        m_vis.addFocusGroup(linear, new DefaultTupleSet());
        m_vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(
            new TupleSetListener() {
                public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                    TupleSet linearInterp = m_vis.getGroup(linear);
                    if ( add.length < 1 ) return; linearInterp.clear();
                    for ( Node n = (Node)add[0]; n!=null; n=n.getParent() )
                        linearInterp.addTuple(n);
                }
            }
        );
        
        SearchTupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                m_vis.cancel("animatePaint");
                m_vis.run("recolor");
                m_vis.run("animatePaint");
            }
        });
	}
	
	public static void main(String argv[]) {
	    String label = "name";
	    
	    
	    /*if ( argv.length > 1 ) {
	        infile = argv[0];
	        label = argv[1];
	    }*/
	    
	    UILib.setPlatformLookAndFeel();
	    
	    JFrame frame = new JFrame("p r e f u s e  |  r a d i a l g r a p h v i e w");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
        /*Graph g = null;
        try {
            g = new GraphMLReader().readGraph(infile);
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit(1);
        }*/
	    
        
		feelings_database = getAssociatedFeelingsFromXMLFile("emotion_database.xml");
		//System.out.println(nl.getLength());
        
        Graph g2 = buildGraph(feelings_database,"blessed");
        
        
	    frame.setContentPane(radialpanel(g2,label));
	    frame.pack();
	    frame.setVisible(true);
	}
	
    public static JPanel radialpanel(Graph g, final String label) {        
        // create a new radial tree view
        final radialview gview = new radialview(g, label, false);
        Visualization vis = gview.getVisualization();
        

        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(gview, BorderLayout.CENTER);
        
        Box box = buildBottomBox(gview, vis, label);
        
        panel.add(box, BorderLayout.SOUTH);
        
        Color BACKGROUND = Color.BLACK;
        Color FOREGROUND = Color.WHITE;
        UILib.setColor(panel, BACKGROUND, FOREGROUND);
        
        return panel;
    }
    
    public static Box buildBottomBox(radialview gview, Visualization vis, final String label) {
        // create a search panel for the tree map
        SearchQueryBinding sq = new SearchQueryBinding(
             (Table)vis.getGroup(treeNodes), label,
             (SearchTupleSet)vis.getGroup(Visualization.SEARCH_ITEMS));
        JSearchPanel search = sq.createSearchPanel();
        search.setShowResultCount(true);
        search.setBorder(BorderFactory.createEmptyBorder(5,5,4,0));
        search.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 11));
        
        search.setBackground(Color.BLACK);
        search.setForeground(Color.WHITE);
        
        final JFastLabel title = new JFastLabel("M I X E D F E E L I N G S");
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
        title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
        
        title.setBackground(Color.BLACK);
        title.setForeground(new Color (237, 24, 83));
        
        gview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                if ( item.canGetString(label) )
                    title.setText(item.getString(label));
            }
            public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);
            }
        });
        
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
        box.add(search);
        box.add(Box.createHorizontalStrut(3));
        
        
        return box;
    }
    
	public static NodeList getAssociatedFeelingsFromXMLFile(String filename) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse (new File(filename));
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("feelingpair");
			return list;
		} 
		catch (SAXException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();} 
		catch (ParserConfigurationException e) {e.printStackTrace();}
		return null;
	}
    
    
	public static Graph buildGraph(NodeList list, String root) {
		Graph g = new Graph(true);
		g.addColumn("name", String.class);
		g.addColumn("shown", String.class);
		g.addColumn("index", Integer.class);
		g.getEdgeTable().addColumn("show", String.class);
		fnodes = new HashMap<String, Node>();
		Set<String> feelings = new HashSet<String>();
		
		ArrayList<Integer> tmplette = new ArrayList<Integer>();
		
		for (int s = 0; s < list.getLength(); s++) {
        	Element element = (Element)list.item(s);
        	String feeling = element.getAttribute("feeling");
        	if (!feelings.contains(feeling))
        	{
        		Node n = g.addNode();
        		n.setString("name", feeling);
        		feelings.add(feeling);
        		fnodes.put(feeling, n);
        		n.set("index", new Integer(tmplette.size()));
        		tmplette.add(getFeelingColor(feeling));
        	}
        	String feeling2 = element.getAttribute("feeling2");
        	if (!feelings.contains(feeling2))
        	{
        		Node n = g.addNode();
        		n.setString("name", feeling2);
        		feelings.add(feeling2);
        		fnodes.put(feeling2, n);
        		n.set("index", new Integer(tmplette.size()));
        		tmplette.add(getFeelingColor(feeling2));
        	}
        	//System.out.println("feeling: " + feeling + ", feeling2: " + feeling2);
        	if (g.getEdge(fnodes.get(feeling), fnodes.get(feeling2))==null)
        	{
        		Edge ne = g.addEdge(fnodes.get(feeling), fnodes.get(feeling2));
     			ne.setString("show", "yes");
        	}
        }
		palette = new int[tmplette.size()];
        for (int i=0; i<tmplette.size(); i++)
        	palette[i] = tmplette.get(i);
		
		return g;
	}
    
	private static Map<String, Integer> feelingColors;
	private static Boolean colors_loaded = false;
	
    public static int getFeelingColor(String f) {
    	if (!colors_loaded)
    	{
    		feelingColors = new HashMap<String,Integer>();
    		Scanner colorscan;
			try {
				colorscan = new Scanner(new File("feelings.txt"));
	    		while (colorscan.hasNext())
	    		{
	    			String feeling = colorscan.next();
	    			
	    			colorscan.next();
	    			String color = colorscan.next();
	    			
	    			int r = Integer.parseInt(color.substring(0,2),16);
	    			int g = Integer.parseInt(color.substring(2,4),16);
	    			int b = Integer.parseInt(color.substring(4,6),16);
	    			//System.out.println(feeling + ": " + r+","+g+","+b);
	    			
	    			feelingColors.put(feeling, ColorLib.rgba((r+3*255)/4, (g+3*255)/4, (b+3*255)/4, 255));
	    			//feelingColors.put(feeling, ColorLib.rgba(r, g, b, 255));
	    		}
	    		colors_loaded = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}

    	if (feelingColors.containsKey(f))
    		return feelingColors.get(f);
    	else
    		return ColorLib.rgba(128, 255, 196, 255);
    }
    
    public static class TreeRootAction extends GroupAction {
        public TreeRootAction(String graphGroup) {
            super(graphGroup);
        }
        public void run(double frac) {
            TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
            if ( focus==null || focus.getTupleCount() == 0 ) return;
            
            Graph g = (Graph)m_vis.getGroup(m_group);
            
            
            Node f = null;
            @SuppressWarnings("rawtypes")
			Iterator tuples = focus.tuples();
            while (tuples.hasNext() && !g.containsTuple(f=(Node)tuples.next()))
            {
                f = null;
            }
            if ( f == null ) return;
            
            
            g.getSpanningTree(f);
            
            if (hasFrame)
            {
            	Vis.changeTagCloud(f.getString("name"));
            }
        }
    }
    
    
    
    /**
     * Set node fill colors
     */
    public static class NodeColorAction extends DataColorAction {
        public NodeColorAction(String group) {
            super(group, "index", Constants.ORDINAL, VisualItem.FILLCOLOR, palette);
        	//super(group, VisualItem.FILLCOLOR);
            add("_hover", ColorLib.gray(220,230));
            add("ingroup('_search_')", ColorLib.rgb(200,16,18));
            //add("ingroup('_focus_')", ColorLib.rgb(198,229,229));
            add("[shown]=='no'", ColorLib.rgba(0,0,0,0));
        }
    }
    
    /**
     * Set node text colors
     */
    public static class TextColorAction extends ColorAction {
        public TextColorAction(String group) {
            super(group, VisualItem.TEXTCOLOR, ColorLib.gray(0));
            add("_hover", ColorLib.rgb(255,0,0));
            add("ingroup('_search_')", ColorLib.rgb(230,255,255));
            add("[shown]=='no'", ColorLib.rgba(0,0,0,0));
        }
    }
	
    public static class EdgeColorAction extends ColorAction {
        public EdgeColorAction(String group) {
            super(group, VisualItem.STROKECOLOR, ColorLib.rgba(110,110,110,255));
            add(ExpressionParser.predicate("[show]=='no'"), ColorLib.rgba(0,0,0,0));
            add(ExpressionParser.predicate("[show]=='bold'"), ColorLib.rgba(237, 24, 83, 255));
        }
    }
    
    public static class EdgeStrokeAction extends StrokeAction {
    	public EdgeStrokeAction(String group) {
    		super(group, new BasicStroke(1.0f));
            add(ExpressionParser.predicate("[show]=='bold'"), new BasicStroke(2.0f));
    	}
    }
    
}
