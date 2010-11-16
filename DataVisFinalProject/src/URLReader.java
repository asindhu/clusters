import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;
import java.io.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

public class URLReader {
    
	public static void main(String[] args) throws Exception {
	
		/* Reads the data and puts it into an XML file */
		URL data = new URL("http://ws4.wefeelfine.org:8080/ShowFeelings?display=xml&returnfields=sentence,feeling&limit=10000&password=sepistheman");
		BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
		String inputLine;
		
		FileWriter fstream = new FileWriter("output.xml");
        BufferedWriter out = new BufferedWriter(fstream);
		
		while ((inputLine = in.readLine()) != null) {
			out.write(inputLine + "\n");
		}
		
		in.close();
		out.close();
		
		/* Parses the XML file */
		
		Map<String, Integer> allFeelings = new HashMap<String, Integer>();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File("output.xml"));

        doc.getDocumentElement().normalize();

        NodeList list = doc.getElementsByTagName("feeling");
        int totalItems = list.getLength();
        System.out.println("Total no of items : " + totalItems);

        Element f;
        String feeling;
        int count;
        
        for (int s = 0; s < totalItems; s++) {
        	f = (Element)list.item(s);
        	feeling = f.getAttribute("feeling");
        
        	if (allFeelings.containsKey(feeling)) {
        		count = allFeelings.get(feeling) + 1;
        		allFeelings.put(feeling, count);
        	} else {
        		allFeelings.put(feeling, 1);
        	}
        }
        
        
        /* Some weird shit I found online that apparently sorts the hash map to print it out.
         * No idea what's going on here, but at least it works.
         */
        ArrayList as = new ArrayList(allFeelings.entrySet());  
        
        Collections.sort( as , new Comparator() {  
            public int compare( Object o1 , Object o2 )  
            {  
                Map.Entry e1 = (Map.Entry)o1 ;  
                Map.Entry e2 = (Map.Entry)o2 ;  
                Integer first = (Integer)e1.getValue();  
                Integer second = (Integer)e2.getValue();  
                return first.compareTo( second );  
            }  
        });  
          
        Iterator i = as.iterator();  
        while ( i.hasNext() )  
        {  
            System.out.println( (Map.Entry)i.next() );  
        } 
	
	
	
	}
	
}
