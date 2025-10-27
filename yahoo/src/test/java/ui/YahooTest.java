package ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ui.pages.HomePage;
import ui.pages.SearchResultPage;

@Tag("YAHOO-UI")
class YahooTest extends UiRunner {

    private HomePage home;
    private SearchResultPage searchResultPage;

    @BeforeAll
    void initHome() {
        home = new HomePage();
    }

    @Test
    void homePageTitle_containsYahoo() {
        searchResultPage = home.search("news");
        searchResultPage.getBackToHomePage();

    }
}
