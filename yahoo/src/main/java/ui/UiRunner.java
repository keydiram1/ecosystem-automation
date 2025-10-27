package ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UiRunner {
    public static final String homepageURL;
    protected WebDriver webDriver;

    // Load .env -> system props once per JVM
    static {
        PropertiesHandler.addSystemProperties("../devops/install/yahoo/.env");
        homepageURL = PropertiesHandler.getParameter("YAHOO_BASE_URL");
    }

    @BeforeAll
    void startClassDriver() {
        webDriver = DriverManager.getWebDriver(); // one driver per test class/thread

        long implicitSec = Long.parseLong(
                System.getProperty("implicit.seconds",
                        System.getenv().getOrDefault("IMPLICIT_SECONDS", "0")));
        long pageLoadSec = Long.parseLong(
                System.getProperty("pageload.seconds",
                        System.getenv().getOrDefault("PAGELOAD_SECONDS", "60")));

        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitSec));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadSec));
        webDriver.manage().window().maximize();
    }

    @AfterAll
    void stopClassDriver() {
        DriverManager.quitWebDriver();
        webDriver = null;
    }
}
