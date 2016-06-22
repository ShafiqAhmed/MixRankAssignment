package LogoCollectionMultiThread;

import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LogoTag implements Runnable{
	
	private static Logger log = Logger.getLogger(LogoTag.class.getName());
	
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
	private static final String OUTPUTFILENAME = "C:\\Users\\Dilip\\Desktop\\Shafiq\\logos.csv";
	
	/* Queue to save domains/websites from a single page of mix at a time*/
	private ConcurrentLinkedQueue<String> domains = new ConcurrentLinkedQueue<String>();
	
	/* HashMap to save all the logos of websites */
	private static ConcurrentHashMap<String, List<String>> allLogos = new ConcurrentHashMap<String, List<String>>();
	
	/* List to save websites which have authentication issue */
	private static ConcurrentHashMap<String,Integer> failingWebsites = new ConcurrentHashMap<String, Integer>();
	
	/* List of websites which redirect to some other source code */
	private static ConcurrentHashMap<String,Integer> NoLogosFoundUsingLinkAndMeta = new ConcurrentHashMap<String, Integer>();
	
	public String input;
	
	public LogoTag(String input){
		this.input = input;
	}
	
	/*
	 * collects domains and triggers image (logo) collection on each website
	 */
	@Override
	public void run() {
		try {
			domainCollection(input);
		} catch (Exception e1) {
			log.info("###########" + input + "#########" + "==> No response. Please try after sometime");
		}
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
					getFailingWebsites().put(domains.poll(),1);  
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
	public void domainCollection(String url) throws Exception{
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
	public  void imageCollection(String url, String domain) throws Exception{
		Document doc = Jsoup.connect(url).get();
        Elements links = doc.select(LINK);
        Elements meta = doc.select(METACONTENT);
        Elements image = doc.select(SRC);
        List<String> list = Collections.synchronizedList(new ArrayList<String>());
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
        	getNoLogosFoundUsingLinkAndMeta().put(url,1);
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

	public static ConcurrentHashMap<String, List<String>> getAllLogos() {
		return allLogos;
	}

	public static void setAllLogos(ConcurrentHashMap<String, List<String>> allLogos) {
		LogoTag.allLogos = allLogos;
	}

	public static ConcurrentHashMap<String,Integer> getFailingWebsites() {
		return failingWebsites;
	}

	public static void setFailingWebsites(ConcurrentHashMap<String,Integer> failingWebsites) {
		LogoTag.failingWebsites = failingWebsites;
	}

	public static ConcurrentHashMap<String,Integer> getNoLogosFoundUsingLinkAndMeta() {
		return NoLogosFoundUsingLinkAndMeta;
	}

	public static void setNoLogosFoundUsingLinkAndMeta(
			ConcurrentHashMap<String,Integer> noLogosFoundUsingLinkAndMeta) {
		NoLogosFoundUsingLinkAndMeta = noLogosFoundUsingLinkAndMeta;
	}
}
