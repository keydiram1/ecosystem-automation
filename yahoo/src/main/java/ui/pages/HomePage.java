package ui.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ui.DriverManager;
import ui.SeleniumUtils;
import ui.UiRunner;

import java.time.Duration;

import static ui.DriverManager.getWebDriver;

public class HomePage {
    private final WebDriver webDriver;

    public HomePage() {
        this.webDriver = getWebDriver();
        webDriver.get(UiRunner.homepageURL);
        acceptConsentIfPresent();
        if (!webDriver.getTitle().toLowerCase().contains("yahoo")) {
            throw new IllegalStateException("Home title does not contain 'yahoo'");
        }
    }

    public MailPage openMail() {
        SeleniumUtils.clickByXpath("//span[normalize-space(.)='Mail']");
        SeleniumUtils.switchToTab(2);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new MailPage(); // MailPage pulls driver from DriverManager
    }

    public SearchResultPage search(String query) {
        WebElement searchBox = SeleniumUtils.waitClickableByName("p");
        searchBox.clear();
        searchBox.sendKeys(query, Keys.ENTER);

        SeleniumUtils.switchToNewTab();                // search opens a new tab
        SeleniumUtils.waitUrlContains("search");       // wait for results page

        return new SearchResultPage();
    }


    private void acceptConsentIfPresent() {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(6));
        try {
            webDriver.switchTo().defaultContent();

            var consentIframes = webDriver.findElements(By.cssSelector(
                    "iframe[src*='consent'], iframe[id*='sp_message_iframe']"));
            if (!consentIframes.isEmpty()) {
                webDriver.switchTo().frame(consentIframes.get(0));
            }

            By acceptPrimary = By.cssSelector("button[name='agree'], button.accept-all");
            By acceptByText = By.xpath(
                    "//button[normalize-space()='לקבל הכל' or normalize-space()='לקבל הכול' " +
                            "or normalize-space()='Accept all' or normalize-space()='Agree to all']");

            WebElement acceptBtn;
            try {
                acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(acceptPrimary));
            } catch (TimeoutException e) {
                acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(acceptByText));
            }
            acceptBtn.click();
        } catch (TimeoutException ignored) {
            // no consent shown
        } finally {
            webDriver.switchTo().defaultContent();
        }
    }
}
