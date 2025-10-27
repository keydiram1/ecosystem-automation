package ui;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Wait helpers + tab/window helpers with per-thread tab order. */
public final class SeleniumUtils {
    private SeleniumUtils() {}

    // ---- configuration ----
    private static Duration defaultTimeout() {
        long timeoutSeconds = Long.parseLong(System.getProperty("wait.seconds", "30"));
        return Duration.ofSeconds(timeoutSeconds);
    }

    // ---- driver access ----
    private static WebDriver webDriver() { return DriverManager.getWebDriver(); }

    // ---- waits ----
    public static WebElement waitClickable(By locator) {
        return waitClickable(locator, defaultTimeout());
    }
    public static WebElement waitClickable(By locator, Duration timeout) {
        return new WebDriverWait(webDriver(), timeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Convenience: wait until element with name is clickable. */
    public static WebElement waitClickableByName(String name) {
        return waitClickable(By.name(name), defaultTimeout());
    }
    public static WebElement waitClickableByName(String name, Duration timeout) {
        return waitClickable(By.name(name), timeout);
    }

    public static WebElement waitVisible(By locator) {
        return waitVisible(locator, defaultTimeout());
    }
    public static WebElement waitVisible(By locator, Duration timeout) {
        return new WebDriverWait(webDriver(), timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitPresence(By locator) {
        return waitPresence(locator, defaultTimeout());
    }
    public static WebElement waitPresence(By locator, Duration timeout) {
        return new WebDriverWait(webDriver(), timeout)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static boolean waitUrlContains(String urlFragment) {
        return waitUrlContains(urlFragment, defaultTimeout());
    }
    public static boolean waitUrlContains(String urlFragment, Duration timeout) {
        return new WebDriverWait(webDriver(), timeout)
                .until(ExpectedConditions.urlContains(urlFragment));
    }

    // ---- tabs/windows (thread-local ordered list) ----
    private static final ThreadLocal<List<String>> threadLocalTabOrder =
            ThreadLocal.withInitial(ArrayList::new);

    /** Sync internal order with current browser tabs, preserving known order. */
    public static void syncTabs() {
        Set<String> currentHandles = webDriver().getWindowHandles();
        List<String> orderedHandles = threadLocalTabOrder.get();

        orderedHandles.removeIf(handle -> !currentHandles.contains(handle));      // drop closed
        for (String handle : currentHandles) if (!orderedHandles.contains(handle)) orderedHandles.add(handle); // add new

        String currentHandle = webDriver().getWindowHandle();
        if (!orderedHandles.contains(currentHandle)) orderedHandles.add(currentHandle);
    }

    public static String currentHandle() { return webDriver().getWindowHandle(); }

    public static List<String> orderedTabHandles() {
        syncTabs();
        return List.copyOf(threadLocalTabOrder.get());
    }

    public static int currentTabIndex() {
        syncTabs();
        return threadLocalTabOrder.get().indexOf(webDriver().getWindowHandle());
    }

    /** Switch to tab by index (0-based). */
    public static void switchToTab(int index) {
        syncTabs();
        List<String> orderedHandles = threadLocalTabOrder.get();
        if (index < 0 || index >= orderedHandles.size()) {
            throw new IndexOutOfBoundsException("Tab " + index + " out of 0.." + (orderedHandles.size() - 1));
        }
        webDriver().switchTo().window(orderedHandles.get(index));
    }

    /** Wait for a new tab relative to current set, then switch to it; returns its handle. */
    public static String switchToNewTab() { return switchToNewTab(defaultTimeout()); }

    public static String switchToNewTab(Duration timeout) {
        WebDriver d = webDriver();
        Set<String> before = new HashSet<>(d.getWindowHandles()); // copy!

        new WebDriverWait(d, timeout).until(dr -> {
            Set<String> now = new HashSet<>(dr.getWindowHandles());
            now.removeAll(before);
            return !now.isEmpty();
        });

        // find the new handle and switch
        Set<String> now = new HashSet<>(d.getWindowHandles());
        now.removeAll(before);
        String newHandle = now.iterator().next();

        d.switchTo().window(newHandle);
        syncTabs();
        return newHandle;
    }

    /** Wait until at least N tabs, then switch to the newest. Returns its handle. */
    public static String waitForAtLeastTabsAndSwitchLast(int minimumCount) {
        return waitForAtLeastTabsAndSwitchLast(minimumCount, defaultTimeout());
    }
    public static String waitForAtLeastTabsAndSwitchLast(int minimumCount, Duration timeout) {
        new WebDriverWait(webDriver(), timeout)
                .until(driver -> driver.getWindowHandles().size() >= minimumCount);
        syncTabs();
        List<String> orderedHandles = threadLocalTabOrder.get();
        if (orderedHandles.size() < minimumCount) {
            throw new TimeoutException("Expected >= " + minimumCount + " tabs");
        }
        String newestHandle = orderedHandles.get(orderedHandles.size() - 1);
        webDriver().switchTo().window(newestHandle);
        return newestHandle;
    }

    /** Close current tab and switch to previous if exists, else first. */
    public static void closeCurrentAndReturn() {
        syncTabs();
        String currentHandle = webDriver().getWindowHandle();
        List<String> orderedHandles = threadLocalTabOrder.get();
        int currentIndex = orderedHandles.indexOf(currentHandle);

        webDriver().close();
        syncTabs();

        if (orderedHandles.isEmpty()) return;
        int targetIndex = Math.max(0, currentIndex - 1);
        if (targetIndex >= orderedHandles.size()) targetIndex = orderedHandles.size() - 1;
        webDriver().switchTo().window(orderedHandles.get(targetIndex));
    }

    // ---- XPath helpers ----
    public static WebElement waitClickableByXpath(String xpath) {
        return waitClickable(By.xpath(xpath), defaultTimeout());
    }
    public static WebElement waitClickableByXpath(String xpath, Duration timeout) {
        return waitClickable(By.xpath(xpath), timeout);
    }
    public static void clickByXpath(String xpath) {
        WebElement el = waitClickableByXpath(xpath);
        try {
            el.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            // JS fallback
            ((JavascriptExecutor) webDriver()).executeScript("arguments[0].click();", el);
        }
    }
}
