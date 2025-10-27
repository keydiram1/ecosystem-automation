package ui.pages;

import org.openqa.selenium.WebDriver;
import ui.SeleniumUtils;

import static ui.DriverManager.getWebDriver;

public class SearchResultPage {
    private final WebDriver webDriver;

    public SearchResultPage() {
        this.webDriver = getWebDriver(); // per-thread driver
    }

    public void getBackToHomePage() {
        SeleniumUtils.switchToTab(0);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
