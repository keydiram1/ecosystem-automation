package ui;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

public final class DriverManager {
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private DriverManager() {}

    /** Return the current thread's WebDriver (create if missing). */
    public static WebDriver getWebDriver() {
        WebDriver webDriver = threadLocalDriver.get();
        if (webDriver == null) {
            webDriver = createWebDriver();
            threadLocalDriver.set(webDriver);
        }
        return webDriver;
    }

    /** Quit and remove this thread's WebDriver. Call in @AfterAll or @AfterEach. */
    public static void quitWebDriver() {
        WebDriver webDriver = threadLocalDriver.get();
        if (webDriver != null) {
            try { webDriver.quit(); } finally { threadLocalDriver.remove(); }
        }
    }

    // --- factory ---

    private static WebDriver createWebDriver() {
        String browserName = System.getProperty("BROWSER",
                System.getProperty("browser", "chrome")).toLowerCase();
        boolean headlessEnabled = Boolean.parseBoolean(System.getProperty("HEADLESS", "false"));

        Duration implicitWait = Duration.ofSeconds(
                Long.parseLong(System.getProperty("implicit.seconds", "0")));
        Duration pageLoadTimeout = Duration.ofSeconds(
                Long.parseLong(System.getProperty("pageload.seconds", "60")));

        WebDriver webDriver = selectLocalDriver(browserName, headlessEnabled);

        webDriver.manage().timeouts().implicitlyWait(implicitWait);
        webDriver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        webDriver.manage().window().maximize();
        return webDriver;
    }

    private static WebDriver selectLocalDriver(String browserName, boolean headlessEnabled) {
        return switch (browserName) {
            case "firefox" -> new FirefoxDriver(firefoxOptions(headlessEnabled));
            case "edge"    -> new EdgeDriver(edgeOptions(headlessEnabled));
            default        -> new ChromeDriver(chromeOptions(headlessEnabled));
        };
    }

    private static ChromeOptions chromeOptions(boolean headlessEnabled) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setAcceptInsecureCerts(true);
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        if (headlessEnabled) chromeOptions.addArguments("--headless=new");
        chromeOptions.addArguments("--disable-dev-shm-usage", "--no-sandbox", "--disable-gpu");
        return chromeOptions;
    }

    private static FirefoxOptions firefoxOptions(boolean headlessEnabled) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setAcceptInsecureCerts(true);
        if (headlessEnabled) firefoxOptions.addArguments("-headless");
        return firefoxOptions;
    }

    private static EdgeOptions edgeOptions(boolean headlessEnabled) {
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.setAcceptInsecureCerts(true);
        if (headlessEnabled) edgeOptions.addArguments("--headless=new");
        return edgeOptions;
    }
}
