package com.tenantcomplaint.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeleniumTest {

```
private static WebDriver driver;
private static WebDriverWait wait;
private static String baseUrl;
private static String submittedReferenceId;

@BeforeAll
static void setup() {
    baseUrl = System.getProperty(
            "app.base.url",
            "http://localhost:8080"
    );

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-gpu");
    options.addArguments("--window-size=1920,1080");

    driver = new ChromeDriver(options);

    wait = new WebDriverWait(
            driver,
            Duration.ofSeconds(10)
    );
}

@AfterAll
static void teardown() {
    if (driver != null) {
        driver.quit();
    }
}

@AfterEach
void screenshotOnFailure(TestInfo testInfo) {
    // Screenshots are handled inside each test's catch block.
}

private void takeScreenshot(String name) {
    try {
        File screenshot = ((TakesScreenshot) driver)
                .getScreenshotAs(OutputType.FILE);

        Path targetDir = Paths.get(
                "target",
                "selenium-screenshots"
        );

        Files.createDirectories(targetDir);

        Files.copy(
                screenshot.toPath(),
                targetDir.resolve(name + ".png")
        );

    } catch (IOException e) {
        System.err.println(
                "Failed to save screenshot: " + e.getMessage()
        );
    }
}

@Test
@Order(1)
@DisplayName("TEST 1: Tenant submits a valid complaint")
void testSubmitValidComplaint() {

    try {
        driver.get(baseUrl + "/complaints/submit");

        driver.findElement(By.id("tenantName"))
                .sendKeys("John Doe");

        driver.findElement(By.id("email"))
                .sendKeys("john.selenium@example.com");

        driver.findElement(By.id("phoneNumber"))
                .sendKeys("9876543210");

        driver.findElement(By.id("propertyInfo"))
                .sendKeys("Flat 302, Block A");

        new Select(
                driver.findElement(By.id("category"))
        ).selectByValue("WATER_LEAKAGE");

        new Select(
                driver.findElement(By.id("priority"))
        ).selectByValue("HIGH");

        driver.findElement(By.id("title"))
                .sendKeys(
                        "Water leak in bathroom ceiling"
                );

        driver.findElement(By.id("description"))
                .sendKeys(
                        "There is a constant water leak in the " +
                        "bathroom ceiling causing damage to the " +
                        "walls and floor tiles."
                );

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.urlContains(
                        "/complaints/success/"
                )
        );

        WebElement refElement = driver.findElement(
                By.cssSelector(".detail-item .value")
        );

        submittedReferenceId =
                refElement.getText().trim();

        assertNotNull(
                submittedReferenceId,
                "Reference ID should not be null"
        );

        assertTrue(
                submittedReferenceId.startsWith("COMP-"),
                "Reference ID should start with COMP-"
        );

        String pageSource = driver.getPageSource();

        assertTrue(
                pageSource.contains(
                        "Complaint Submitted Successfully"
                ),
                "Success message should be displayed"
        );

        takeScreenshot("test1_submit_success");

    } catch (Exception e) {

        takeScreenshot("test1_submit_failure");

        throw e;
    }
}

@Test
@Order(2)
@DisplayName(
        "TEST 2: Invalid complaint data is rejected " +
        "with validation messages"
)
void testSubmitInvalidComplaint() {

    try {
        driver.get(baseUrl + "/complaints/submit");

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".form-error")
                )
        );

        var errors = driver.findElements(
                By.cssSelector(".form-error")
        );

        assertFalse(
                errors.isEmpty(),
                "Validation errors should be displayed"
        );

        assertTrue(
                errors.size() >= 3,
                "Multiple validation errors should appear"
        );

        takeScreenshot("test2_validation_errors");

    } catch (Exception e) {

        takeScreenshot("test2_validation_failure");

        throw e;
    }
}

@Test
@Order(3)
@DisplayName(
        "TEST 3: Tenant tracks a submitted complaint"
)
void testTrackComplaint() {

    try {
        assertNotNull(
                submittedReferenceId,
                "Reference ID from Test 1 must be available"
        );

        driver.get(
                baseUrl + "/complaints/track"
        );

        driver.findElement(
                By.id("referenceId")
        ).sendKeys(submittedReferenceId);

        driver.findElement(
                By.id("trackEmail")
        ).sendKeys(
                "john.selenium@example.com"
        );

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".detail-grid")
                )
        );

        String pageSource = driver.getPageSource();

        assertTrue(
                pageSource.contains(
                        submittedReferenceId
                ),
                "Reference ID should be displayed"
        );

        assertTrue(
                pageSource.contains("SUBMITTED") ||
                pageSource.contains("Submitted"),
                "Status should be shown"
        );

        takeScreenshot("test3_track_success");

    } catch (Exception e) {

        takeScreenshot("test3_track_failure");

        throw e;
    }
}

@Test
@Order(4)
@DisplayName(
        "TEST 4: Reviewer logs in and views complaint"
)
void testReviewerLogin() {

    try {
        driver.get(baseUrl + "/login");

        driver.findElement(
                By.id("username")
        ).sendKeys(
                "admin@tenant.com"
        );

        driver.findElement(
                By.id("password")
        ).sendKeys(
                "admin123"
        );

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.urlContains(
                        "/reviewer/dashboard"
                )
        );

        String pageSource = driver.getPageSource();

        assertTrue(
                pageSource.contains(
                        "Reviewer Dashboard"
                ),
                "Dashboard should be visible"
        );

        assertTrue(
                pageSource.contains(
                        submittedReferenceId
                ) ||
                pageSource.contains("COMP-"),
                "Complaints should be listed"
        );

        WebElement viewButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//table//tbody//tr[contains(.,'" +
                                submittedReferenceId +
                                "')]//a[contains(@class,'btn')]"
                        )
                )
        );

        viewButton.click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".detail-grid")
                )
        );

        assertTrue(
                driver.getPageSource().contains(
                        submittedReferenceId
                ),
                "Complaint details should be shown"
        );

        takeScreenshot("test4_reviewer_view");

    } catch (Exception e) {

        takeScreenshot("test4_reviewer_failure");

        throw e;
    }
}

@Test
@Order(5)
@DisplayName(
        "TEST 5: Reviewer approves complaint " +
        "and status is updated"
)
void testReviewerApproveComplaint() {

    try {
        assertNotNull(
                submittedReferenceId,
                "Reference ID from Test 1 must be available"
        );

        driver.get(
                baseUrl + "/reviewer/dashboard"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("table")
                )
        );

        WebElement viewButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//table//tbody//tr[contains(.,'" +
                                submittedReferenceId +
                                "')]//a[contains(@class,'btn')]"
                        )
                )
        );

        viewButton.click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("status")
                )
        );

        /*
         * STEP 1:
         * Change complaint status to Under Review.
         */

        Select statusSelect = new Select(
                driver.findElement(By.id("status"))
        );

        statusSelect.selectByVisibleText(
                "Under Review"
        );

        driver.findElement(
                By.id("remarks")
        ).clear();

        driver.findElement(
                By.id("remarks")
        ).sendKeys(
                "Taking this complaint for review"
        );

        driver.findElement(
                By.cssSelector(
                        "form button[type='submit']"
                )
        ).click();

        /*
         * Wait until the status field is available
         * again after the update.
         */

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("status")
                )
        );

        /*
         * Verify that Under Review was actually saved.
         */

        Select updatedStatus = new Select(
                driver.findElement(By.id("status"))
        );

        assertEquals(
                "Under Review",
                updatedStatus
                        .getFirstSelectedOption()
                        .getText()
                        .trim(),
                "Status should be changed to Under Review"
        );

        takeScreenshot(
                "test5_under_review_success"
        );

        /*
         * STEP 2:
         * Change complaint status to Approved.
         */

        updatedStatus.selectByVisibleText(
                "Approved"
        );

        driver.findElement(
                By.id("remarks")
        ).clear();

        driver.findElement(
                By.id("remarks")
        ).sendKeys(
                "Issue has been resolved. " +
                "Approving complaint."
        );

        driver.findElement(
                By.cssSelector(
                        "form button[type='submit']"
                )
        ).click();

        /*
         * Wait until the status field is available
         * again after the second update.
         */

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("status")
                )
        );

        /*
         * Verify that Approved was actually saved.
         */

        Select approvedStatus = new Select(
                driver.findElement(By.id("status"))
        );

        assertEquals(
                "Approved",
                approvedStatus
                        .getFirstSelectedOption()
                        .getText()
                        .trim(),
                "Status should be changed to Approved"
        );

        takeScreenshot(
                "test5_approve_success"
        );

    } catch (Exception e) {

        takeScreenshot(
                "test5_approve_failure"
        );

        throw e;
    }
}

}
