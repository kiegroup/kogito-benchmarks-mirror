package org.kie.kogito.benchmarks.framework;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebpageTester {
    private static final Logger LOGGER = Logger.getLogger(WebpageTester.class.getName());

    /**
     * Patiently try to wait for a web page and examine it
     *
     * @param url address
     * @param timeoutS in seconds
     * @param stringToLookFor string must be present on the page
     */
    public static long testWeb(String url, long timeoutS, String stringToLookFor, boolean measureTime) throws InterruptedException, IOException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url must not be empty");
        }
        if (timeoutS < 0) {
            throw new IllegalArgumentException("timeoutS must be positive");
        }
        if (StringUtils.isBlank(stringToLookFor)) {
            throw new IllegalArgumentException("stringToLookFor must contain a non-empty string");
        }
        String webPage = "";

        long now = System.currentTimeMillis();
        System.out.println("Now in the testWeb method: " + now);
        final long startTime = now;
        boolean found = false;
        long foundTimestamp = -1L;
        while (now - startTime < 1000 * timeoutS) {
            URLConnection c = new URL(url).openConnection();
            c.setRequestProperty("Accept", "*/*");
            c.setConnectTimeout(500);
            long requestStart = System.currentTimeMillis();
            try (Scanner scanner = new Scanner(c.getInputStream(), StandardCharsets.UTF_8.toString())) {
                scanner.useDelimiter("\\A");
                webPage = scanner.hasNext() ? scanner.next() : "";
            } catch (Exception e) {
                LOGGER.debug("Waiting `" + stringToLookFor + "' to appear on " + url);
            }
            if (webPage.contains(stringToLookFor)) {
                found = true;
                if (measureTime) {
                    foundTimestamp = System.currentTimeMillis();
                    System.out.println("Found timestamp " + foundTimestamp);
                    System.out.println("Request took " + (foundTimestamp - requestStart));
                }
                break;
            }
            if (!measureTime) {
                Thread.sleep(500);
            } else {
                Thread.sleep(0, 100000);
            }
            now = System.currentTimeMillis();
        }

        String failureMessage = "Timeout " + timeoutS + "s was reached. " +
                (StringUtils.isNotBlank(webPage) ? webPage + " must contain string: " : "Empty webpage does not contain string: ") +
                "`" + stringToLookFor + "'";
        if (!found) {
            LOGGER.info(failureMessage);
        }
        assertTrue(found, failureMessage);
        return foundTimestamp - startTime;
    }
}