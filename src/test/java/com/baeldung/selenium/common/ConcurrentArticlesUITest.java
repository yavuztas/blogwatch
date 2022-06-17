package com.baeldung.selenium.common;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;

import com.baeldung.common.ConcurrentTest;
import com.baeldung.common.GlobalConstants;
import com.baeldung.common.GlobalConstants.TestMetricTypes;
import com.baeldung.common.Utils;
import com.baeldung.site.SitePage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Experimental concurrent version of {@link ArticlesUITest}
 */
public class ConcurrentArticlesUITest extends ConcurrentBaseUISeleniumTest {

    @Value("${ignore.urls.newer.than.weeks}")
    private int ignoreUrlsNewerThanWeeks;

    private final TestSupport support = new TestSupport();

    private static SynchronizedIterator allArticlesList;
    private static final Multimap<String, String> badURLs = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    static class SynchronizedIterator implements Iterator<String> {

        private final Iterator<String> iterator;

        public SynchronizedIterator(Iterator<String> iterator) {
            this.iterator = iterator;
        }

        @Override
        public synchronized boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public synchronized String next() {
            return this.iterator.next();
        }
    }

    @BeforeAll
    static synchronized void loadArticleList() throws IOException {
        allArticlesList = new SynchronizedIterator(Utils.fetchAllArtilcesAsListIterator());
    }

    @BeforeEach
    public void setup() {
        logger.info("The test will ignore URls newer than {} weeks", ignoreUrlsNewerThanWeeks);
    }

    private boolean loadNextURL(SitePage page) {

        synchronized (this) {
            if (!allArticlesList.hasNext()) {
                return false;
            }
            page.setUrl(page.getBaseURL() + allArticlesList.next());
        }

        logger.info("Loading - {}", page.getUrl());
        page.loadUrl();
        if (page.isNewerThan(ignoreUrlsNewerThanWeeks)) {
            logger.info("Skipping {} as it's newer than {} weeks", page.getUrl(), ignoreUrlsNewerThanWeeks);
            loadNextURL(page);
        }

        if (shouldSkipUrl(page, GlobalConstants.givenAllLongRunningTests_whenHittingAllArticles_thenOK)) {
            loadNextURL(page);
        }

        return true;
    }

    /**
     * Runs a command on a new page
     */
    void runOnPage(Consumer<SitePage> cmd) {
        final SitePage page = get();
        page.openNewWindow();
        if (loadNextURL(page)) {
            cmd.accept(page);
        }
        page.quiet();
    }

    class TestSupport {

        public final void givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock);
            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock)) {
                return;
            }
            if (page.findEmptyCodeBlocks().size() > 0) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop);
            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop) || Utils.excludePage(page.getUrl(), GlobalConstants.ARTILCE_JAVA_WEEKLY, false)) {
                return;
            }
            if (page.findShortCodesAtTheTopOfThePage().size() != 1) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd) || Utils.excludePage(page.getUrl(), GlobalConstants.ARTILCE_JAVA_WEEKLY, false)) {
                return;
            }
            if (page.findShortCodesAtTheEndOfThePage().size() != 1) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute);

            final List<WebElement> imgTags = page.findImagesWithEmptyAltAttribute();
            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute)) {
                return;
            }
            if (imgTags.size() > 0) {
                recordMetrics(imgTags.size(), TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute, imgTags.size());
                badURLs.put(GlobalConstants.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute, page.getUrlWithNewLineFeed() + " ( " + imgTags.stream()
                    .map(webElement -> webElement.getAttribute("src") + " , ")
                    .collect(Collectors.joining()) + ")\n");
            }
        }

        public final void givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription)) {
                return;
            }

            final String metaDescriptionTag = page.getMetaDescriptionContent();
            final String excerptTag = page.getMetaExcerptContent();

            if (StringUtils.isBlank(excerptTag) || !Objects.equals(excerptTag, metaDescriptionTag)) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription, 1);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription, page.getUrlWithNewLineFeed() + " ( description : [" + metaDescriptionTag + "], excerpt : [" + excerptTag + "] ) ");
            }
        }

        public final void givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite);

            final List<WebElement> imgTags = page.findImagesPointingToDraftSiteOnTheArticle();
            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite)) {
                return;
            }

            if (imgTags.size() > 0) {
                recordMetrics(imgTags.size(), TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite, imgTags.size());
                badURLs.put(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite,
                    page.getUrlWithNewLineFeed() + " ( " + imgTags.stream().map(webElement -> webElement.getAttribute("src") + " , ").collect(Collectors.joining()) + ")\n");
            }

            final List<WebElement> anchorTags = page.findAnchorsPointingToAnImageAndDraftSiteOnTheArticle();
            if (anchorTags.size() > 0) {
                recordMetrics(anchorTags.size(), TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite, anchorTags.size());
                badURLs.put(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite,
                    page.getUrlWithNewLineFeed() + " ( " + anchorTags.stream().map(webElement -> webElement.getAttribute("href") + " , ").collect(Collectors.joining()) + ")\n");
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath)) {
                return;
            }

            if (!page.findMetaTagWithOGImagePointingToTheAbsolutePath() || !page.findMetaTagWithTwitterImagePointingToTheAbsolutePath()) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath);
                logger.info("og:image or twitter:image check failed for: {}", page.getUrl());
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly(SitePage page, boolean sleep) throws InterruptedException {
            recordExecution(GlobalConstants.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly) || Utils.excludePage(page.getUrl(), GlobalConstants.ARTILCE_JAVA_WEEKLY, false)) {
                return;
            }
            if (sleep) {
                Thread.sleep(1000);
            }
            if (page.hasBrokenCodeBlock()) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText)) {
                return;
            }

            if (page.containesOverlappingText()) {
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar, false)) {
                return;
            }
            if (page.getOptinsFromTheSideBar() != 1) {
                logger.info("page found which doesn't have a single Opt-in in the sidebar {}", page.getUrl());
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar, page.getUrlWithNewLineFeed());
            }
        }

        public final void givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent(SitePage page) {
            recordExecution(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent);

            if (shouldSkipUrl(page, GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent,false)) {
                return;
            }

            if (page.getOptinsFromTheAfterPostContent() != 1) {
                logger.info("page found which doesn't have a single Opt-in in the after post content {}", page.getUrl());
                recordMetrics(1, TestMetricTypes.FAILED);
                recordFailure(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent);
                badURLs.put(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent, page.getUrlWithNewLineFeed());
            }
        }

    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute() {
        log(GlobalConstants.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public void givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription() {
        log(GlobalConstants.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite() {
        log(GlobalConstants.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly() {
        log(GlobalConstants.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly);

        runOnPage(page -> {
            do {
                try {
                    support.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly(page, true);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    public final void givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent() {
        log(GlobalConstants.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent);

        runOnPage(page -> {
            do {
                support.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent(page);
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    @ConcurrentTest
    @Tag(GlobalConstants.TAG_TECHNICAL)
    public final void givenAllTestsRelatedTechnicalArea_whenHittingAllArticles_thenOK() {
        runOnPage(page -> {
            do {
                try {
                    support.givenAllArticles_whenAnArticleLoads_thenArticleHasNoEmptyCodeBlock(page);
                    support.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheTop(page);
                    support.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheSidebar(page);
                    support.givenAllArticles_whenAnArticleLoads_thenItIsHasASingleOptinInTheAfterPostContent(page);
                    support.givenAllArticles_whenAnArticleLoads_thenItHasSingleShortcodeAtTheEnd(page);
                    support.givenAllArticles_whenAnalysingImages_thenImagesDoNotPoinToTheDraftsSite(page);
                    support.givenAllArticles_whenAnArticleLoads_thenMetaOGImageAndTwitterImagePointToTheAbsolutePath(page);
                    support.givenAllArticles_whenAnalyzingCodeBlocks_thenCodeBlocksAreRenderedProperly(page, false);
                    support.givenAllArticles_whenAnArticleLoads_thenItDoesNotContainOverlappingText(page);
                    support.givenAllArticles_whenAnalyzingImages_thenImagesDoNotHaveEmptyAltAttribute(page);
                    support.givenAllArticles_whenAnalyzingExcerpt_thenItShouldNotBeEmptyAndShouldMatchDescription(page);
                } catch (Exception e) {
                    logger.error("Error occurred while processing: {}, error message: {}",
                        page.getUrl(), StringUtils.substring(e.getMessage(), 0, 100));
                }
            } while (loadNextURL(page));
        });

        if (badURLs.size() > 0) {
            triggerTestFailure(badURLs);
        }
    }

    private void log(String testName) {
        logger.info("Running Test - {}", testName);
    }

}
