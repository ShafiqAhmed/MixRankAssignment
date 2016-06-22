package LogoCollectionMultiThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

public class LogoTagDriver {
	
	private static Logger log = Logger.getLogger(LogoTagDriver.class.getName());
	
	private static final String MIXRANK = "https://mixrank.com/web/sites";
	private static final String MIXRANKOFFSET = "https://mixrank.com/web/sites?offset=";

	public static void main(String[] args) {
		 
		ExecutorService executor = Executors.newFixedThreadPool(40);
		String urls = new String(MIXRANKOFFSET);
		for(int i = 0; i<40;i++){
			if(i == 0){
				Runnable worker = new LogoTag(MIXRANK);
	            executor.execute(worker);
			}	
			else{
				String temp = urls;
				Runnable worker = new LogoTag(temp + 25*i);
	            executor.execute(worker);
			}
		}
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        LogoTag.writeToFile();
        
        log.info("Finished all threads");	
        log.info(LogoTag.getAllLogos().size() + " " + LogoTag.getFailingWebsites().size() + " " + LogoTag.getNoLogosFoundUsingLinkAndMeta().size());
	}

}
