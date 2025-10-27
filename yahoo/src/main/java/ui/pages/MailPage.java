package ui.pages;

import org.openqa.selenium.WebDriver;

import static ui.DriverManager.getWebDriver;

public class MailPage {
    private final WebDriver webDriver = getWebDriver();

    public MailPage open(String url) {
        webDriver.get(url);
        return this;
    }

    // add actions: login, compose, etc.
    // e.g.
    // public MailPage login(String user, String pass) { ...; return this; }
}
