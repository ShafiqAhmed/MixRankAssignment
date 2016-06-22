package LogoCollectionSingleThread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

 
public class LogoTagSingleThread {
	
	private static Logger log = Logger.getLogger(LogoTagSingleThread.class.getName());
	
	private static final String SRC = "[src]";
	private static final String IMG = "img";
	private static final String ABSOLUTESRC = "abs:src";
	private static final String EQUALSYM = "=";
	private static final String CONTENT = "content";
	private static final String DOT = "\\.";
	private static final String PNG = ".png";
	private static final String ICO = ".ico";
	private static final String JPG = ".jpg";
	private static final String IMAGE = "image";
	private static final String LOGO = "logo";
	private static final String METACONTENT = "meta[content]";
	private static final String ICON = "icon";
	private static final String HTTP = "http";
	private static final String HREF = "href";
	private static final String REL = "rel";
	private static final String LINK = "link";
	private static final String MIXRANK = "https://mixrank.com/web/sites";
	private static final String MIXRANKOFFSET = "https://mixrank.com/web/sites?offset=";
	private static final String OUTPUTFILENAME = "C:\\Users\\Dilip\\Desktop\\Shafiq\\logos.csv";
	
	/* Queue to save domains/websites from a single page of mix at a time*/
	private static Queue<String> domains = new LinkedList<String>();
	
	/* HashMap to save all the logos of websites */
	private static HashMap<String, List<String>> allLogos = new HashMap<String, List<String>>();
	
	/* List to save websites which have authentication issue */
	private static List<String> failingWebsites = new ArrayList<String>();
	
	/* List of websites which redirect to some other source code */
	private static List<String> NoLogosFoundUsingLinkAndMeta = new ArrayList<String>();
	
	public static void main(String [] args) throws Exception{
		String urls = new String(MIXRANKOFFSET);
		String temp = null;
		try{
		for(int i = 0; i<40;i++){
			if(i == 0){
				domainCollection(MIXRANK);
				start();
			}	
			else{
				temp = urls + 25*i;
				domainCollection(temp);
				start();
			}
		}	
		}
		catch (Exception ex){
			log.info("###########" + temp + "#########" + "==> No response. Please try after sometime");
		}
		writeToFile();
			log.info(getAllLogos().size() + " " + getFailingWebsites().size() + " " + getNoLogosFoundUsingLinkAndMeta().size());
    }
	
	/*
	 * triggers image (logo) collection on each website
	 */
	public static void start(){
		while(!domains.isEmpty()){
			String domain = domains.peek();
			try{
			String url = "http://" + domain + "/";
			imageCollection(url,domain);
			}catch (Exception e){
				try{
				String url = "https://" + domain + "/";
				imageCollection(url, domain);
				}
				catch(Exception ex){
					getFailingWebsites().add(domains.poll());
				}
			}finally{
				if(domain == domains.peek())
					domains.poll();
			}
		}
	}
	
	/*
	 * collecting all websites from mixrank website
	 */
	public static void domainCollection(String url) throws Exception{
 		String website[] = new String [2];
 		Document doc = Jsoup.connect(url).get();
 		Elements image = doc.select(SRC);
 		for(Element src : image){
 			if(src.tagName().equals(IMG)){
 				String domain = src.attr(ABSOLUTESRC);
 				if(domain.contains(EQUALSYM)){
 					website = domain.split(EQUALSYM);
 					}
 				 if (website[1] != null){
 					domains.add(website[1]);
 				 }
 				 log.info(website[1]);
 			}
 		}
 				log.info(domains.size());
	}
	 
	/*
	 * collects logo from website
	 */
	public static void imageCollection(String url, String domain) throws Exception{
		Document doc = Jsoup.connect(url).get();
        Elements links = doc.select(LINK);
        Elements meta = doc.select(METACONTENT);
        Elements image = doc.select(SRC);
        List<String> list = new ArrayList<String>();
        String d [] = domain.split(DOT);
        
        //initially looking for ICON in link tag. Most of the logos are in this tags.
        for (Element src : links) {
           	String attrhref = src.attr(HREF);
        	if(src.attr(REL).toString().contains(ICON)){
        		if(attrhref.contains(PNG) || attrhref.contains(JPG) || attrhref.contains(ICO)){
        			list.add(attrhref);
        			log.info(attrhref);
        		}
        	
        	}
        }
        
        //If we don't get any logos in link, we check for logos in Meta tags with IMAGE
        if(list.size() == 0){
        for(Element m : meta){
        	String attrcontent = m.attr(CONTENT);
        	if(m.toString().contains(IMAGE)){
        		if(attrcontent.contains(PNG) || attrcontent.contains(JPG)){
        			list.add(attrcontent);
        			log.info(attrcontent);
        		}	
        	}
        }
        }
        
       // If we still don't get any logos from Link or Meta we further go deep to check images.
        if(list.size() == 0){
        	for(Element i : image){
        		String attrsrc = i.attr(SRC);
            	if(i.toString().contains(HTTP) && i.toString().contains(d[0]) && (i.toString().contains(LOGO) || i.toString().contains(ICON) || i.toString().contains(IMAGE))){
            		if(attrsrc.contains(PNG) || attrsrc.contains(JPG)){
            			list.add(attrsrc);
            			log.info(attrsrc);
            		}	
            	}
            }
        } 
        
        //website is added to NoLogosFoundUsingLinkAndMeta as we are redirected to some anonymous source code where we won't find logos (sometimes)
        if(list.size() == 0){
        	getNoLogosFoundUsingLinkAndMeta().add(url);
        }
        else
        	getAllLogos().put(url, list);
	}
	
	/*
	 * writes Domain, List of logos to csv file
	 */
	public static void writeToFile(){
		try {
			File file = new File(OUTPUTFILENAME);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			Set<String> keys = getAllLogos().keySet();		 
		    Iterator<String> itr = keys.iterator();
		    while (itr.hasNext()) { 
		       String str = itr.next();
		       bw.write("Domain: "+ str +" \t" +" Logos: "+ getAllLogos().get(str) + "\n");
		       log.info("Domain: "+ str +" \t" +" Logos: "+ getAllLogos().get(str) + "\n");
		    } 
			bw.close();
				log.info("Done");

		} catch (IOException e) {
				log.error("Problem while writting in File");
		}
	}

	public static HashMap<String, List<String>> getAllLogos() {
		return allLogos;
	}

	public static void setAllLogos(HashMap<String, List<String>> allLogos) {
		LogoTagSingleThread.allLogos = allLogos;
	}

	public static List<String> getFailingWebsites() {
		return failingWebsites;
	}

	public static void setFailingWebsites(List<String> failingWebsites) {
		LogoTagSingleThread.failingWebsites = failingWebsites;
	}

	public static List<String> getNoLogosFoundUsingLinkAndMeta() {
		return NoLogosFoundUsingLinkAndMeta;
	}

	public static void setNoLogosFoundUsingLinkAndMeta(
			List<String> noLogosFoundUsingLinkAndMeta) {
		NoLogosFoundUsingLinkAndMeta = noLogosFoundUsingLinkAndMeta;
	}
}
