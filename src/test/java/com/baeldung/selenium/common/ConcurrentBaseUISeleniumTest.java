package com.baeldung.selenium.common;

import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.baeldung.common.ConcurrentBaseTest;
import com.baeldung.common.AllUrlsConcurrentExtension;
import com.baeldung.common.config.CommonConfig;
import com.baeldung.common.config.MyApplicationContextInitializer;
import com.baeldung.crawler4j.config.Crawler4jMainCofig;
import com.baeldung.selenium.config.SeleniumContextConfiguration;
import com.baeldung.selenium.config.headlessBrowserConfig;
import com.baeldung.site.SitePage;

/**
 * A base class to supply prototype-scoped {@link SitePage} bean for thread-safe concurrent executions.
 */
@ContextConfiguration(classes = {
    CommonConfig.class,
    SeleniumContextConfiguration.class,
    Crawler4jMainCofig.class,
    ConcurrentBaseUISeleniumTest.SitePageConfiguration.class
}, initializers = MyApplicationContextInitializer.class)
@ExtendWith(SpringExtension.class)
public class ConcurrentBaseUISeleniumTest extends ConcurrentBaseTest implements Supplier<SitePage> {

    @Autowired
    ApplicationContext appContext;

    @Configuration
    static class SitePageConfiguration {

        @Bean("onDemandSitePage")
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        SitePage sitePage() {
            return new SitePage(seleniumHeadlessBrowserConfig());
        }

        @Bean("onDemandBrowserConfig")
        @Profile("headless-browser")
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public headlessBrowserConfig seleniumHeadlessBrowserConfig() {
            return new headlessBrowserConfig();
        }

    }

    @Override
    public SitePage get() {
        return appContext.getBean("onDemandSitePage", SitePage.class);
    }

}
