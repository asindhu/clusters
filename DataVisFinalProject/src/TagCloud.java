import java.util.*;
import javax.swing.*;
import java.awt.*;

public class TagCloud extends JPanel {

	private static final int QUERY = 50;
	private static final int WIDTH = 600;
	private static final int HEIGHT = 185;
	private static final int MARGIN = 20;
	private static final int SPACE = 10;
	private static final int ROW_HEIGHT = 32;
	private static final int ROW_WIDTH = WIDTH - MARGIN;
	private ArrayList<Label> labels;
	
	public TagCloud() {
		setLayout(null);
	    setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.BLACK);
		
		labels = new ArrayList<Label>();
		for (int i =0; i < QUERY; i++) {
			Label label = new Label();
			labels.add(label);
			add(label);
		}
	}
	
	public void setFeeling(String feeling) {
		for (Label label: labels) {
			label.setText("");
			label.setLocation(500,500);
			label.setSize(0,0);
		}
		
		final Map<String, Double> data = Database.getTopics(feeling, QUERY);		
		ArrayList<String> topics = new ArrayList<String>(data.keySet());
		Collections.sort(topics, new Comparator<String>() {  
			public int compare(String a, String b) {    
				return (int) ((data.get(b) - data.get(a))*100000);  
			}  
		});
		double max = findMax(data);
		
		int x = MARGIN;
		int y = MARGIN;
		
		for (int i = 0; i < topics.size(); i++) {
			String topic = topics.get(i);
			Label label = labels.get(i);
			
			label.setText(topic);
			label.setAlignment(Label.LEFT);
			label.setBackground(Color.BLACK);
			label.setForeground( new Color(237, 24, 83));
			label.setFont(new Font("SansSerif", Font.BOLD, scale(data.get(topic), max)));
			label.setLocation(x, y);
			
			
			
			int endX = x + label.getWidth();
			if (endX > ROW_WIDTH) {
				x = MARGIN;
				y += ROW_HEIGHT;
				label.setLocation(x,y);
				if (y > HEIGHT - MARGIN) {
					//label.setText("");
					//label.setLocation(0, 0);
					break;
				}
			}
			x += label.getWidth() + SPACE;
		}
	}
	
	private static int scale(double factor, double max) {
		int val = (int) ((Math.log1p(factor) / Math.log1p(max))*30);
		if (val < 10) val = 10;
		return val;
	}
	
	private static double findMax(Map<String, Double> data) {
		double max = 0;
		Set<String> topics = data.keySet();
		for (String topic: topics) {
			double curr = data.get(topic);
			if (curr > max) max = curr; 
		}
		return max;
	}

/* *************************************************************** */
// Testing Code	
/* *************************************************************** */
	
//	public static void main(String[] args) {
//		Database.init();
//        JFrame frame = new JFrame();
//        frame.setBackground(Color.BLACK);
//        TagCloud cloud = new TagCloud(frame);
//        frame.add(cloud);
//        
//        
//        
//		frame.pack();
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setVisible(true);
//		cloud.setFeeling("angry");
//    
//    }
	
}
