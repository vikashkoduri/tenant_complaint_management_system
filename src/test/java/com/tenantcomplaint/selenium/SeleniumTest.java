package com.tenantcomplaint.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium WebDriver integration tests for critical user journeys.
 *
 * These tests expect the application to be running externally (e.g., via
 * Spring Boot or deployed on Tomcat) at the URL specified by the
 * {@code app.base.url} system property (default: http://localhost:8080).
 *
 * Run with:
 *   mvn failsafe:integration-test failsafe:verify -Dapp.base.url=http://localhost:8080
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL =
            System.getProperty("app.base.url", "http://localhost:8080");

    private static final String TENANT_EMAIL =
            "john@example.com";

    private static final String REVIEWER_EMAIL =
            "admin@tenant.com";

    private static final String REVIEWER_PASSWORD =
            "admin123";

    /** Shared across ordered tests via PER_CLASS lifecycle */
    private String submittedReferenceId;

    @BeforeAll
    void setUpAll() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterAll
    void tearDownAll() {
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterEach
    void screenshotAfterTest(TestInfo testInfo) {
        try {
            if (driver != null) {
                File screenshotDir =
                        new File("target/selenium-screenshots");

                if (!screenshotDir.exists()) {
                    screenshotDir.mkdirs();
                }

                File screenshot =
                        ((TakesScreenshot) driver)
                                .getScreenshotAs(OutputType.FILE);

                String safeName =
                        testInfo.getDisplayName()
                                .replaceAll("[^a-zA-Z0-9.-]", "_");

                Path destination =
                        new File(
                                screenshotDir,
                                safeName + ".png"
                        ).toPath();

                Files.copy(
                        screenshot.toPath(),
                        destination,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // TEST 1 - Submit Complaint
    // ============================================================

    @Test
    @Order(1)
    void testSubmitComplaint() {

        driver.get(BASE_URL + "/complaints/submit");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("title")
                )
        );

        fillField("tenantName", "John Doe");
        fillField("email", TENANT_EMAIL);
        fillField("phoneNumber", "9876543210");
        fillField("propertyInfo", "Building A, Room 101");
        fillField("title", "Water leak");
        fillField("description", "Water leakage is occurring in the bathroom ceiling causing damage.");

        selectFirstAvailable(
                "category",
                "PLUMBING",
                "Plumbing",
                "WATER_LEAKAGE",
                "Water Leakage"
        );

        selectFirstAvailable(
                "priority",
                "MEDIUM",
                "Medium",
                "HIGH",
                "High"
        );

        WebElement submit = findSubmitButton();
        submit.click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert-success")
                        ),
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath(
                                        "//*[contains(text(),'COMP-')]"
                                )
                        ),
                        ExpectedConditions.urlContains(
                                "/complaints/success"
                        )
                )
        );

        String pageSource = driver.getPageSource();

        int index = pageSource.indexOf("COMP-");

        if (index >= 0) {
            String reference = extractReferenceId(pageSource, index);
            if (reference != null) {
                submittedReferenceId = reference;
            }
        }

        assertTrue(
                pageSource.contains("Complaint")
                        || pageSource.contains("submitted")
                        || pageSource.contains("COMP-"),
                "Success page should contain complaint confirmation"
        );
    }


    // ============================================================
    // TEST 2 - Track Submitted Complaint
    // ============================================================

    @Test
    @Order(2)
    void testComplaintDetails() {

        driver.get(BASE_URL + "/complaints/track");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("referenceId")
                )
        );

        fillField("referenceId",
                submittedReferenceId != null
                        ? submittedReferenceId
                        : "COMP-123");

        fillField("email", TENANT_EMAIL);

        WebElement submit = findSubmitButton();
        submit.click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        if (submittedReferenceId != null) {
            assertTrue(
                    driver.getPageSource()
                            .contains(submittedReferenceId),
                    "Submitted complaint reference ID should be visible"
            );
        } else {
            assertTrue(
                    driver.getPageSource().contains("Water leak")
                            || driver.getPageSource().contains("Complaint")
            );
        }
    }


    // ============================================================
    // TEST 3 - Reviewer Login
    // ============================================================

    @Test
    @Order(3)
    void testReviewerLogin() {

        loginAsReviewer();

        assertTrue(
                driver.getCurrentUrl().contains("/reviewer")
                        || driver.getPageSource().contains("Dashboard"),
                "Reviewer should be on the dashboard after login"
        );
    }


    // ============================================================
    // TEST 4 - Reviewer Views Dashboard
    // ============================================================

    @Test
    @Order(4)
    void testReviewerDashboard() {

        loginAsReviewer();

        driver.get(BASE_URL + "/reviewer/dashboard");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );

        WebElement table = driver.findElement(By.id("complaintsTable"));
        assertNotNull(table, "Complaints table must be present");

        assertTrue(
                driver.getPageSource().contains("Reviewer Dashboard"),
                "Dashboard page should contain 'Reviewer Dashboard'"
        );
    }


    // ============================================================
    // TEST 5 - Reviewer Approves Complaint
    // ============================================================

    @Test
    @Order(5)
    void testReviewerApproveComplaint() {

        loginAsReviewer();

        driver.get(BASE_URL + "/reviewer/dashboard");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );

        /*
         * Find the complaint submitted by the test.
         */
        WebElement complaintRow = findComplaintRow();

        assertNotNull(
                complaintRow,
                "Complaint row must be present on reviewer dashboard"
        );

        /*
         * Get the View link directly from this row.
         */
        WebElement viewLink =
                complaintRow.findElement(
                        By.cssSelector("a.view-complaint")
                );

        String complaintUrl = viewLink.getAttribute("href");

        assertNotNull(
                complaintUrl,
                "Complaint View link must have an href"
        );

        /*
         * Open complaint detail page.
         */
        driver.get(complaintUrl);

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        assertTrue(
                driver.getPageSource().contains("Complaint")
                        || driver.getPageSource().contains("Reference ID")
        );

        /*
         * ========================================================
         * STEP 1:
         * Change SUBMITTED -> UNDER_REVIEW
         * ========================================================
         */

        WebElement statusSelect =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.id("status")
                        )
                );

        Select status = new Select(statusSelect);

        assertTrue(
                hasOption(status, "Under Review"),
                "Status dropdown must contain Under Review"
        );

        status.selectByVisibleText("Under Review");

        /*
         * Enter reviewer remarks.
         */
        WebElement remarks =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.id("remarks")
                        )
                );

        remarks.clear();
        remarks.sendKeys(
                "Complaint reviewed and moved to Under Review."
        );

        /*
         * Submit status update.
         */
        clickUpdateStatus();

        /*
         * Wait for response.
         */
        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains(
                                "/reviewer/complaint"
                        ),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert-success")
                        )
                )
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        assertTrue(
                driver.getPageSource()
                        .contains("Under Review"),
                "Complaint should be Under Review after first update"
        );


        /*
         * ========================================================
         * STEP 2:
         * Re-open dashboard and find same complaint.
         * ========================================================
         */

        driver.get(BASE_URL + "/reviewer/dashboard");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );

        WebElement updatedRow = findComplaintRow();

        assertNotNull(
                updatedRow,
                "Updated complaint must be visible on dashboard"
        );

        WebElement currentStatus =
                updatedRow.findElement(
                        By.cssSelector(".complaint-status")
                );

        assertEquals(
                "Under Review",
                currentStatus.getText().trim(),
                "Dashboard should show Under Review"
        );

        /*
         * Open the same complaint again.
         */
        WebElement updatedViewLink =
                updatedRow.findElement(
                        By.cssSelector("a.view-complaint")
                );

        String updatedComplaintUrl =
                updatedViewLink.getAttribute("href");

        driver.get(updatedComplaintUrl);

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("status")
                )
        );


        /*
         * ========================================================
         * STEP 3:
         * Change UNDER_REVIEW -> APPROVED
         * ========================================================
         */

        WebElement approvalSelect =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.id("status")
                        )
                );

        Select approvalStatus = new Select(approvalSelect);

        assertTrue(
                hasOption(approvalStatus, "Approved"),
                "Status dropdown must contain Approved"
        );

        approvalStatus.selectByVisibleText("Approved");

        /*
         * Enter approval remarks.
         */
        WebElement approvalRemarks =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.id("remarks")
                        )
                );

        approvalRemarks.clear();
        approvalRemarks.sendKeys(
                "Complaint reviewed and approved."
        );

        /*
         * Submit approval.
         */
        clickUpdateStatus();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert-info")
                        ),
                        ExpectedConditions.not(
                                ExpectedConditions.presenceOfElementLocated(
                                        By.id("status")
                                )
                        )
                )
        );

        assertTrue(
                driver.getPageSource()
                        .contains("Approved"),
                "Complaint should be Approved after final update"
        );


        /*
         * ========================================================
         * FINAL DASHBOARD VERIFICATION
         * ========================================================
         */

        driver.get(BASE_URL + "/reviewer/dashboard");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );

        WebElement finalRow = findComplaintRow();

        assertNotNull(
                finalRow,
                "Approved complaint must remain visible on dashboard"
        );

        WebElement finalStatus =
                finalRow.findElement(
                        By.cssSelector(".complaint-status")
                );

        assertEquals(
                "Approved",
                finalStatus.getText().trim(),
                "Final dashboard status must be Approved"
        );
    }


    // ============================================================
    // LOGIN - REVIEWER
    // ============================================================

    private void loginAsReviewer() {

        driver.get(BASE_URL + "/login");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.name("username")
                )
        );

        WebElement username =
                driver.findElement(By.name("username"));

        WebElement password =
                driver.findElement(By.name("password"));

        username.clear();
        username.sendKeys(REVIEWER_EMAIL);

        password.clear();
        password.sendKeys(REVIEWER_PASSWORD);

        findSubmitButton().click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains(
                                "/reviewer"
                        ),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert")
                        )
                )
        );
    }


    // ============================================================
    // FIND COMPLAINT ROW
    // ============================================================

    private WebElement findComplaintRow() {

        /*
         * First attempt:
         * Use the exact reference ID.
         */
        if (submittedReferenceId != null
                && !submittedReferenceId.isBlank()) {

            String xpath =
                    "//tr[@data-reference-id='"
                            + submittedReferenceId
                            + "']";
            try {
                return wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath(xpath)
                        )
                );
            } catch (Exception ignored) {
            }
        }

        /*
         * Second attempt:
         * Search by reference ID text.
         */
        if (submittedReferenceId != null
                && !submittedReferenceId.isBlank()) {

            try {
                return wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath(
                                        "//table[@id='complaintsTable']//tbody//tr[contains(.,'"
                                                + submittedReferenceId
                                                + "')]"
                                )
                        )
                );
            } catch (Exception ignored) {
            }
        }

        /*
         * Third attempt:
         * Find Water leak complaint.
         */
        try {
            return wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//table[@id='complaintsTable']//tbody//tr[contains(.,'Water leak')]"
                            )
                    )
            );
        } catch (Exception ignored) {
        }

        /*
         * Final fallback:
         * Return first actual complaint row.
         */
        try {
            return wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    "#complaintsTable tbody tr.complaint-row"
                            )
                    )
            );
        } catch (Exception e) {
            return null;
        }
    }


    // ============================================================
    // FIND SUBMIT BUTTON
    // ============================================================

    private WebElement findSubmitButton() {

        By[] selectors = {
                By.cssSelector("button[type='submit']"),
                By.cssSelector("input[type='submit']"),
                By.xpath("//button[contains(normalize-space(),'Submit')]"),
                By.xpath("//button[contains(normalize-space(),'Sign In')]"),
                By.xpath("//button[contains(normalize-space(),'Login')]"),
                By.xpath("//button[contains(normalize-space(),'Track')]"),
                By.xpath("//button[contains(normalize-space(),'Register')]")
        };

        for (By selector : selectors) {
            try {
                WebElement element = driver.findElement(selector);
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (Exception ignored) {
            }
        }

        throw new NoSuchElementException(
                "Unable to find submit button"
        );
    }


    // ============================================================
    // CLICK UPDATE STATUS
    // ============================================================

    private void clickUpdateStatus() {

        WebElement button =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.xpath(
                                        "//form[.//select[@id='status']]//button[@type='submit']"
                                )
                        )
                );

        button.click();
    }


    // ============================================================
    // CHECK SELECT OPTION
    // ============================================================

    private boolean hasOption(
            Select select,
            String visibleText) {

        for (WebElement option : select.getOptions()) {
            if (option.getText()
                    .trim()
                    .equals(visibleText)) {
                return true;
            }
        }

        return false;
    }


    // ============================================================
    // FILL INPUT FIELD
    // ============================================================

    private void fillField(String fieldId, String value) {

        try {
            // Try by id first, then by name
            WebElement element;
            try {
                element = driver.findElement(By.id(fieldId));
            } catch (NoSuchElementException e) {
                element = driver.findElement(By.name(fieldId));
            }
            element.clear();
            element.sendKeys(value);
        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // SELECT FIRST AVAILABLE OPTION
    // ============================================================

    private void selectFirstAvailable(
            String fieldId,
            String... values) {

        try {
            WebElement element;
            try {
                element = driver.findElement(By.id(fieldId));
            } catch (NoSuchElementException e) {
                element = driver.findElement(By.name(fieldId));
            }

            Select select = new Select(element);

            for (String value : values) {
                try {
                    select.selectByValue(value);
                    return;
                } catch (Exception ignored) {
                }

                try {
                    select.selectByVisibleText(value);
                    return;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // EXTRACT REFERENCE ID
    // ============================================================

    private String extractReferenceId(
            String text,
            int startIndex) {

        try {
            String remaining = text.substring(startIndex);

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < remaining.length(); i++) {
                char c = remaining.charAt(i);
                if (Character.isLetterOrDigit(c) || c == '-') {
                    result.append(c);
                } else {
                    break;
                }
            }

            String reference = result.toString();

            if (reference.startsWith("COMP-")) {
                return reference;
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}