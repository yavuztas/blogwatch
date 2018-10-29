package com.baeldung.crawler;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.baeldung.common.config.MyApplicationContextInitializer;
import com.baeldung.crawler4j.CrawlerForIncorrectlyLinkedURLs;
import com.baeldung.crawler4j.TutorialsRepoCrawlerController;
import com.baeldung.crawler4j.config.CrawlerMainCofig;

@ContextConfiguration(classes = { CrawlerMainCofig.class }, initializers = MyApplicationContextInitializer.class)
@ExtendWith(SpringExtension.class)
public class TutorialRepoUITest {
    
    protected Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    TutorialsRepoCrawlerController tutorialsRepoCrawlerController;
    
    @Test
    @Tag("crawler4j")
    public final void test1() {
        logger.info("No of CPU cores: "+ Runtime.getRuntime().availableProcessors());
        logger.info("Started at: " + new Date());
        tutorialsRepoCrawlerController.startCrawler(CrawlerForIncorrectlyLinkedURLs.class,Runtime.getRuntime().availableProcessors());
        //List<String> urls = tutorialsRepoCrawlerController.getFlaggedURL();
        System.out.println("============================================================================");
        for(Object object : tutorialsRepoCrawlerController.getMatchingURLs()) {
            List<String> urlList = (List<String>) object;
            System.out.println("List Size:"+ urlList.size());
            urlList.forEach( s->System.out.println(s));
        }        
        
        
        logger.info("Ended at at: " + new Date());
    }

}
