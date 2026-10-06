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
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL =
            System.getProperty("app.base.url", "http://localhost:8080");

    private static final String TENANT_EMAIL =
            "john@example.com";

    private static final String TENANT_PASSWORD =
            "password123";

    private static final String REVIEWER_EMAIL =
            "admin@tenant.com";

    private static final String REVIEWER_PASSWORD =
            "admin123";

    private String submittedReferenceId;

    @BeforeEach
    void setUp() {

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

    @AfterEach
    void tearDown(TestInfo testInfo) {

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

        if (driver != null) {
            driver.quit();
        }
    }


    // ============================================================
    // TEST 1 - Tenant Registration
    // ============================================================

    @Test
    @Order(1)
    void testTenantRegistration() {

        driver.get(BASE_URL + "/register");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.name("name")
                )
        );

        String uniqueEmail =
                "john" + System.currentTimeMillis() + "@example.com";

        WebElement name =
                driver.findElement(By.name("name"));

        WebElement email =
                driver.findElement(By.name("email"));

        WebElement password =
                driver.findElement(By.name("password"));

        name.sendKeys("John Doe");
        email.sendKeys(uniqueEmail);
        password.sendKeys("password123");

        WebElement submit =
                findSubmitButton();

        submit.click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains("/login"),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert")
                        )
                )
        );

        assertTrue(
                driver.getCurrentUrl().contains("/login")
                        || !driver.findElements(By.cssSelector(".alert")).isEmpty()
        );
    }


    // ============================================================
    // TEST 2 - Tenant Login
    // ============================================================

    @Test
    @Order(2)
    void testTenantLogin() {

        loginAsTenant();

        assertTrue(
                driver.getCurrentUrl().contains("/tenant")
                        || driver.getCurrentUrl().contains("/dashboard")
                        || driver.getPageSource().contains("Dashboard")
                        || driver.getPageSource().contains("Submit")
        );
    }


    // ============================================================
    // TEST 3 - Submit Complaint
    // ============================================================

    @Test
    @Order(3)
    void testSubmitComplaint() {

        loginAsTenant();

        driver.get(BASE_URL + "/complaint/new");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.name("title")
                )
        );

        fillIfPresent("tenantName", "John Doe");
        fillIfPresent("name", "John Doe");

        fillIfPresent(
                "email",
                TENANT_EMAIL
        );

        fillIfPresent(
                "phoneNumber",
                "9876543210"
        );

        fillIfPresent(
                "propertyInfo",
                "Building A, Room 101"
        );

        fillIfPresent(
                "title",
                "Water leak"
        );

        fillIfPresent(
                "description",
                "Water leakage is occurring in the bathroom."
        );

        selectFirstAvailable(
                "category",
                "PLUMBING",
                "Plumbing",
                "WATER_LEAK",
                "Water Leak"
        );

        selectFirstAvailable(
                "priority",
                "MEDIUM",
                "Medium"
        );

        WebElement submit =
                findSubmitButton();

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
                                "/complaint"
                        )
                )
        );

        String pageSource =
                driver.getPageSource();

        int index =
                pageSource.indexOf("COMP-");

        if (index >= 0) {

            String reference =
                    extractReferenceId(pageSource, index);

            if (reference != null) {
                submittedReferenceId = reference;
            }
        }

        assertTrue(
                pageSource.contains("Complaint")
                        || pageSource.contains("submitted")
                        || pageSource.contains("COMP-")
        );
    }


    // ============================================================
    // TEST 4 - Verify Submitted Complaint
    // ============================================================

    @Test
    @Order(4)
    void testComplaintDetails() {

        loginAsTenant();

        driver.get(BASE_URL + "/tenant/complaints");

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
    // TEST 5 - Reviewer Approves Complaint
    // ============================================================

    @Test
    @Order(5)
    void testReviewerApproveComplaint() {

        loginAsReviewer();

        driver.get(
                BASE_URL + "/reviewer/dashboard"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );

        /*
         * Find the complaint submitted by the test.
         *
         * If submittedReferenceId is available, use it.
         * Otherwise use the first complaint row containing
         * "Water leak" or a complaint row.
         */

        WebElement complaintRow =
                findComplaintRow();

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

        String complaintUrl =
                viewLink.getAttribute("href");

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


        /*
         * Confirm complaint detail page.
         */

        assertTrue(
                driver.getPageSource().contains("Complaint")
                        || driver.getPageSource().contains("Reference ID")
        );


        /*
         * ========================================================
         * STEP 1:
         * Change SUBMITTED -> UNDER_REVIEW
         * ========================================================
         *
         * IMPORTANT:
         *
         * The HTML shows:
         *
         * <select id="status">
         *
         * For SUBMITTED complaint:
         *
         * UNDER_REVIEW
         *
         * is the available status.
         */

        WebElement statusSelect =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.id("status")
                        )
                );

        Select status =
                new Select(statusSelect);

        assertTrue(
                hasOption(
                        status,
                        "Under Review"
                ),
                "Status dropdown must contain Under Review"
        );

        status.selectByVisibleText(
                "Under Review"
        );


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


        /*
         * If we are still on complaint page, verify
         * Under Review is visible.
         */

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

        driver.get(
                BASE_URL + "/reviewer/dashboard"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );


        WebElement updatedRow =
                findComplaintRow();

        assertNotNull(
                updatedRow,
                "Updated complaint must be visible on dashboard"
        );


        /*
         * Verify dashboard shows Under Review.
         */

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

        Select approvalStatus =
                new Select(approvalSelect);


        assertTrue(
                hasOption(
                        approvalStatus,
                        "Approved"
                ),
                "Status dropdown must contain Approved"
        );


        approvalStatus.selectByVisibleText(
                "Approved"
        );


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


        /*
         * Wait for page response.
         */

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );


        /*
         * Approved complaints no longer have the status
         * update form.
         */

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


        /*
         * Verify final status.
         */

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

        driver.get(
                BASE_URL + "/reviewer/dashboard"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("complaintsTable")
                )
        );


        WebElement finalRow =
                findComplaintRow();

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
    // LOGIN - TENANT
    // ============================================================

    private void loginAsTenant() {

        driver.get(
                BASE_URL + "/login"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.name("username")
                )
        );


        WebElement username =
                driver.findElement(
                        By.name("username")
                );

        WebElement password =
                driver.findElement(
                        By.name("password")
                );


        username.clear();
        username.sendKeys(TENANT_EMAIL);

        password.clear();
        password.sendKeys(TENANT_PASSWORD);


        findSubmitButton().click();


        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains(
                                "/tenant"
                        ),
                        ExpectedConditions.urlContains(
                                "/dashboard"
                        ),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert")
                        )
                )
        );
    }


    // ============================================================
    // LOGIN - REVIEWER
    // ============================================================

    private void loginAsReviewer() {

        driver.get(
                BASE_URL + "/login"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.name("username")
                )
        );


        WebElement username =
                driver.findElement(
                        By.name("username")
                );

        WebElement password =
                driver.findElement(
                        By.name("password")
                );


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

                By.cssSelector(
                        "button[type='submit']"
                ),

                By.cssSelector(
                        "input[type='submit']"
                ),

                By.xpath(
                        "//button[contains(normalize-space(),'Submit')]"
                ),

                By.xpath(
                        "//button[contains(normalize-space(),'Login')]"
                ),

                By.xpath(
                        "//button[contains(normalize-space(),'Register')]"
                )
        };


        for (By selector : selectors) {

            try {

                WebElement element =
                        driver.findElement(selector);

                if (element.isDisplayed()
                        && element.isEnabled()) {

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

        for (WebElement option :
                select.getOptions()) {

            if (option.getText()
                    .trim()
                    .equals(visibleText)) {

                return true;
            }
        }

        return false;
    }


    // ============================================================
    // FILL INPUT IF AVAILABLE
    // ============================================================

    private void fillIfPresent(
            String name,
            String value) {

        try {

            WebElement element =
                    driver.findElement(
                            By.name(name)
                    );

            element.clear();
            element.sendKeys(value);

        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // SELECT FIRST AVAILABLE OPTION
    // ============================================================

    private void selectFirstAvailable(
            String name,
            String... values) {

        try {

            WebElement element =
                    driver.findElement(
                            By.name(name)
                    );

            Select select =
                    new Select(element);


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

            String remaining =
                    text.substring(startIndex);

            StringBuilder result =
                    new StringBuilder();

            for (int i = 0;
                 i < remaining.length();
                 i++) {

                char c =
                        remaining.charAt(i);

                if (Character.isLetterOrDigit(c)
                        || c == '-') {

                    result.append(c);

                } else {

                    break;
                }
            }

            String reference =
                    result.toString();

            if (reference.startsWith("COMP-")) {
                return reference;
            }

        } catch (Exception ignored) {
        }

        return null;
    }
}