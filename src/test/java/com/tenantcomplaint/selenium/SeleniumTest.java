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
                Duration.ofSeconds(20)
        );
    }

    @AfterAll
    static void teardown() {

        if (driver != null) {
            driver.quit();
        }
    }

    private void takeScreenshot(String name) {

        try {

            File screenshot =
                    ((TakesScreenshot) driver)
                            .getScreenshotAs(OutputType.FILE);

            Path targetDir =
                    Paths.get(
                            "target",
                            "selenium-screenshots"
                    );

            Files.createDirectories(targetDir);

            Path target =
                    targetDir.resolve(name + ".png");

            Files.deleteIfExists(target);

            Files.copy(
                    screenshot.toPath(),
                    target
            );

        } catch (IOException e) {

            System.err.println(
                    "Failed to save screenshot: "
                            + e.getMessage()
            );
        }
    }

    private void loginAsReviewer() {

        driver.get(
                baseUrl + "/login"
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("username")
                )
        );

        WebElement username =
                driver.findElement(
                        By.id("username")
                );

        username.clear();

        username.sendKeys(
                "admin@tenant.com"
        );

        WebElement password =
                driver.findElement(
                        By.id("password")
                );

        password.clear();

        password.sendKeys(
                "admin123"
        );

        driver.findElement(
                By.cssSelector(
                        "button[type='submit']"
                )
        ).click();

        wait.until(
                ExpectedConditions.urlContains(
                        "/reviewer/dashboard"
                )
        );

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );
    }

    private WebElement findComplaintRow() {

        assertNotNull(
                submittedReferenceId,
                "Submitted complaint reference ID must exist"
        );

        return wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath(
                                "//table//tbody//tr[contains(.,'"
                                        + submittedReferenceId
                                        + "')]"
                        )
                )
        );
    }

    private WebElement findStatusSelectInRow(
            WebElement row
    ) {

        String statusXPath =
                ".//select[" +
                        ".//option[" +
                        "normalize-space()='Under Review'" +
                        "]" +
                        " and " +
                        ".//option[" +
                        "normalize-space()='Approved'" +
                        "]" +
                        " and " +
                        ".//option[" +
                        "normalize-space()='Rejected'" +
                        "]" +
                        "]";

        try {

            return row.findElement(
                    By.xpath(statusXPath)
            );

        } catch (NoSuchElementException ignored) {

            return null;
        }
    }

    private WebElement findStatusSelectOnPage() {

        String statusXPath =
                "//select[" +
                        ".//option[" +
                        "normalize-space()='Under Review'" +
                        "]" +
                        " and " +
                        ".//option[" +
                        "normalize-space()='Approved'" +
                        "]" +
                        " and " +
                        ".//option[" +
                        "normalize-space()='Rejected'" +
                        "]" +
                        "]";

        try {

            return driver.findElement(
                    By.xpath(statusXPath)
            );

        } catch (NoSuchElementException ignored) {

            return null;
        }
    }

    private WebElement findStatusSelect() {

        WebElement statusSelect =
                findStatusSelectOnPage();

        if (statusSelect != null) {
            return statusSelect;
        }

        if (submittedReferenceId != null) {

            try {

                WebElement row =
                        findComplaintRow();

                statusSelect =
                        findStatusSelectInRow(row);

                if (statusSelect != null) {
                    return statusSelect;
                }

            } catch (Exception ignored) {
            }
        }

        fail(
                "Could not find reviewer status dropdown containing " +
                        "Under Review, Approved and Rejected. " +
                        "Current URL: " +
                        driver.getCurrentUrl()
        );

        return null;
    }

    private WebElement findSubmitButtonForStatus(
            WebElement statusSelect
    ) {

        try {

            WebElement form =
                    statusSelect.findElement(
                            By.xpath(
                                    "./ancestor::form[1]"
                            )
                    );

            try {

                return form.findElement(
                        By.cssSelector(
                                "button[type='submit']"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }

            try {

                return form.findElement(
                        By.cssSelector(
                                "input[type='submit']"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }

        } catch (NoSuchElementException ignored) {
        }

        WebElement row = null;

        try {

            row = statusSelect.findElement(
                    By.xpath(
                            "./ancestor::tr[1]"
                    )
            );

        } catch (NoSuchElementException ignored) {
        }

        if (row != null) {

            try {

                return row.findElement(
                        By.cssSelector(
                                "button[type='submit']"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }

            try {

                return row.findElement(
                        By.cssSelector(
                                "input[type='submit']"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }
        }

        try {

            return driver.findElement(
                    By.cssSelector(
                            "button[type='submit']"
                    )
            );

        } catch (NoSuchElementException ignored) {
        }

        try {

            return driver.findElement(
                    By.cssSelector(
                            "input[type='submit']"
                    )
            );

        } catch (NoSuchElementException ignored) {
        }

        fail(
                "Could not find submit button for reviewer status update"
        );

        return null;
    }

    private void enterRemarks(String remarks) {

        WebElement remarksField = null;

        try {

            remarksField =
                    driver.findElement(
                            By.id("remarks")
                    );

        } catch (NoSuchElementException ignored) {
        }

        if (remarksField == null) {

            try {

                remarksField =
                        driver.findElement(
                                By.cssSelector(
                                        "textarea[name='remarks']"
                                )
                        );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (remarksField == null) {

            try {

                remarksField =
                        driver.findElement(
                                By.cssSelector(
                                        "textarea"
                                )
                        );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (remarksField != null) {

            remarksField.clear();

            remarksField.sendKeys(
                    remarks
            );
        }
    }

    private void selectStatus(
            String status
    ) {

        WebElement statusSelect =
                findStatusSelect();

        Select select =
                new Select(statusSelect);

        select.selectByVisibleText(
                status
        );

        String selected =
                select.getFirstSelectedOption()
                        .getText()
                        .trim();

        assertEquals(
                status,
                selected,
                "Reviewer should be able to select "
                        + status
        );
    }

    private void submitStatusUpdate() {

        WebElement statusSelect =
                findStatusSelect();

        WebElement submitButton =
                findSubmitButtonForStatus(
                        statusSelect
                );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        submitButton
                )
        );

        submitButton.click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );

        try {

            Thread.sleep(1000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    private void openComplaintDetails() {

        WebElement row =
                findComplaintRow();

        WebElement action = null;

        try {

            action =
                    row.findElement(
                            By.xpath(
                                    ".//a[contains(" +
                                            "translate(normalize-space(.)," +
                                            "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                                            "'abcdefghijklmnopqrstuvwxyz')," +
                                            "'view'" +
                                            ")]"
                            )
                    );

        } catch (NoSuchElementException ignored) {
        }

        if (action == null) {

            try {

                action =
                        row.findElement(
                                By.cssSelector(
                                        "a.btn"
                                )
                        );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (action == null) {

            try {

                action =
                        row.findElement(
                                By.cssSelector(
                                        "button"
                                )
                        );

            } catch (NoSuchElementException ignored) {
            }
        }

        assertNotNull(
                action,
                "Complaint view/action button should exist"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        action
                )
        );

        action.click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.tagName("body")
                )
        );
    }

    @Test
    @Order(1)
    @DisplayName(
            "TEST 1: Tenant submits a valid complaint"
    )
    void testSubmitValidComplaint() {

        try {

            driver.get(
                    baseUrl + "/complaints/submit"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.id("tenantName")
                    )
            );

            driver.findElement(
                    By.id("tenantName")
            ).sendKeys(
                    "John Doe"
            );

            driver.findElement(
                    By.id("email")
            ).sendKeys(
                    "john.selenium@example.com"
            );

            driver.findElement(
                    By.id("phoneNumber")
            ).sendKeys(
                    "9876543210"
            );

            driver.findElement(
                    By.id("propertyInfo")
            ).sendKeys(
                    "Flat 302, Block A"
            );

            new Select(
                    driver.findElement(
                            By.id("category")
                    )
            ).selectByValue(
                    "WATER_LEAKAGE"
            );

            new Select(
                    driver.findElement(
                            By.id("priority")
                    )
            ).selectByValue(
                    "HIGH"
            );

            driver.findElement(
                    By.id("title")
            ).sendKeys(
                    "Water leak in bathroom ceiling"
            );

            driver.findElement(
                    By.id("description")
            ).sendKeys(
                    "There is a constant water leak in the bathroom ceiling "
                            + "causing damage to the walls and floor tiles."
            );

            driver.findElement(
                    By.cssSelector(
                            "button[type='submit']"
                    )
            ).click();

            wait.until(
                    ExpectedConditions.urlContains(
                            "/complaints/success/"
                    )
            );

            WebElement refElement =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector(
                                            ".detail-item .value"
                                    )
                            )
                    );

            submittedReferenceId =
                    refElement.getText().trim();

            assertNotNull(
                    submittedReferenceId
            );

            assertFalse(
                    submittedReferenceId.isEmpty()
            );

            assertTrue(
                    submittedReferenceId.startsWith(
                            "COMP-"
                    ),
                    "Reference ID should start with COMP-"
            );

            assertTrue(
                    driver.getPageSource().contains(
                            "Complaint Submitted Successfully"
                    ),
                    "Complaint success message should appear"
            );

            takeScreenshot(
                    "test1_submit_success"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test1_submit_failure"
            );

            throw e;
        }
    }

    @Test
    @Order(2)
    @DisplayName(
            "TEST 2: Invalid complaint data is rejected"
    )
    void testSubmitInvalidComplaint() {

        try {

            driver.get(
                    baseUrl + "/complaints/submit"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    "button[type='submit']"
                            )
                    )
            );

            driver.findElement(
                    By.cssSelector(
                            "button[type='submit']"
                    )
            ).click();

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    ".form-error"
                            )
                    )
            );

            var errors =
                    driver.findElements(
                            By.cssSelector(
                                    ".form-error"
                            )
                    );

            assertFalse(
                    errors.isEmpty(),
                    "Validation errors should be displayed"
            );

            assertTrue(
                    errors.size() >= 3,
                    "Multiple validation errors should appear"
            );

            takeScreenshot(
                    "test2_validation_errors"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test2_validation_failure"
            );

            throw e;
        }
    }

    @Test
    @Order(3)
    @DisplayName(
            "TEST 3: Tenant tracks submitted complaint"
    )
    void testTrackComplaint() {

        try {

            assertNotNull(
                    submittedReferenceId
            );

            driver.get(
                    baseUrl + "/complaints/track"
            );

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.id("referenceId")
                    )
            );

            driver.findElement(
                    By.id("referenceId")
            ).sendKeys(
                    submittedReferenceId
            );

            driver.findElement(
                    By.id("trackEmail")
            ).sendKeys(
                    "john.selenium@example.com"
            );

            driver.findElement(
                    By.cssSelector(
                            "button[type='submit']"
                    )
            ).click();

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    ".detail-grid"
                            )
                    )
            );

            String page =
                    driver.getPageSource();

            assertTrue(
                    page.contains(
                            submittedReferenceId
                    ),
                    "Reference ID should be displayed"
            );

            assertTrue(
                    page.contains("SUBMITTED")
                            || page.contains("Submitted"),
                    "Submitted status should be displayed"
            );

            takeScreenshot(
                    "test3_track_success"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test3_track_failure"
            );

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

            assertNotNull(
                    submittedReferenceId
            );

            loginAsReviewer();

            String page =
                    driver.getPageSource();

            assertTrue(
                    page.contains(
                            "Reviewer Dashboard"
                    ),
                    "Reviewer Dashboard should be visible"
            );

            assertTrue(
                    page.contains(
                            submittedReferenceId
                    ),
                    "Submitted complaint should be listed"
            );

            WebElement row =
                    findComplaintRow();

            assertTrue(
                    row.getText().contains(
                            submittedReferenceId
                    ),
                    "Correct complaint row should be displayed"
            );

            openComplaintDetails();

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    ".detail-grid"
                            )
                    )
            );

            String details =
                    driver.getPageSource();

            assertTrue(
                    details.contains(
                            submittedReferenceId
                    )
                            || details.contains(
                            "Water leak in bathroom ceiling"
                    ),
                    "Complaint details should be shown"
            );

            takeScreenshot(
                    "test4_reviewer_view"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test4_reviewer_failure"
            );

            throw e;
        }
    }

    @Test
    @Order(5)
    @DisplayName(
            "TEST 5: Reviewer updates complaint status"
    )
    void testReviewerApproveComplaint() {

        try {

            assertNotNull(
                    submittedReferenceId,
                    "Reference ID must be available"
            );

            /*
             * IMPORTANT:
             *
             * The previous version clicked the complaint View button
             * and then searched for the status dropdown.
             *
             * Jenkins proved that the View page does NOT contain
             * the status dropdown.
             *
             * Therefore this test works directly from the
             * Reviewer Dashboard and searches the exact complaint row
             * for the status control.
             */

            loginAsReviewer();

            WebElement row =
                    findComplaintRow();

            assertTrue(
                    row.getText().contains(
                            submittedReferenceId
                    ),
                    "Correct complaint must be selected"
            );

            WebElement statusSelect =
                    findStatusSelectInRow(row);

            /*
             * If the status dropdown is present directly
             * in the complaint row, use it.
             */
            if (statusSelect == null) {

                statusSelect =
                        findStatusSelectOnPage();
            }

            assertNotNull(
                    statusSelect,
                    "Reviewer status dropdown with Under Review, "
                            + "Approved and Rejected options must exist"
            );

            Select dropdown =
                    new Select(statusSelect);

            assertTrue(
                    dropdown.getOptions()
                            .stream()
                            .anyMatch(
                                    option ->
                                            option.getText()
                                                    .trim()
                                                    .equals(
                                                            "Under Review"
                                                    )
                            ),
                    "Under Review option should exist"
            );

            assertTrue(
                    dropdown.getOptions()
                            .stream()
                            .anyMatch(
                                    option ->
                                            option.getText()
                                                    .trim()
                                                    .equals(
                                                            "Approved"
                                                    )
                            ),
                    "Approved option should exist"
            );

            assertTrue(
                    dropdown.getOptions()
                            .stream()
                            .anyMatch(
                                    option ->
                                            option.getText()
                                                    .trim()
                                                    .equals(
                                                            "Rejected"
                                                    )
                            ),
                    "Rejected option should exist"
            );

            /*
             * STEP 1:
             * Submitted -> Under Review
             */
            dropdown.selectByVisibleText(
                    "Under Review"
            );

            assertEquals(
                    "Under Review",
                    dropdown.getFirstSelectedOption()
                            .getText()
                            .trim(),
                    "Status should be Under Review"
            );

            enterRemarks(
                    "Taking this complaint for review"
            );

            WebElement submitButton =
                    findSubmitButtonForStatus(
                            statusSelect
                    );

            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            submitButton
                    )
            );

            submitButton.click();

            try {

                Thread.sleep(1200);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }

            takeScreenshot(
                    "test5_under_review"
            );

            /*
             * STEP 2:
             * Return to dashboard.
             */
            driver.get(
                    baseUrl + "/reviewer/dashboard"
            );

            wait.until(
                    ExpectedConditions.urlContains(
                            "/reviewer/dashboard"
                    )
            );

            WebElement rowAfterReview =
                    findComplaintRow();

            assertTrue(
                    rowAfterReview.getText().contains(
                            submittedReferenceId
                    ),
                    "Complaint should remain on reviewer dashboard"
            );

            /*
             * STEP 3:
             * Find the status dropdown again.
             */
            WebElement approvedStatus =
                    findStatusSelectInRow(
                            rowAfterReview
                    );

            if (approvedStatus == null) {

                approvedStatus =
                        findStatusSelectOnPage();
            }

            assertNotNull(
                    approvedStatus,
                    "Status dropdown should be available "
                            + "after Under Review update"
            );

            Select approvedDropdown =
                    new Select(
                            approvedStatus
                    );

            /*
             * STEP 4:
             * Under Review -> Approved
             */
            approvedDropdown.selectByVisibleText(
                    "Approved"
            );

            assertEquals(
                    "Approved",
                    approvedDropdown
                            .getFirstSelectedOption()
                            .getText()
                            .trim(),
                    "Status should be Approved"
            );

            enterRemarks(
                    "Issue has been resolved. Approving complaint."
            );

            WebElement approveButton =
                    findSubmitButtonForStatus(
                            approvedStatus
                    );

            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            approveButton
                    )
            );

            approveButton.click();

            try {

                Thread.sleep(1500);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }

            takeScreenshot(
                    "test5_approve_success"
            );

            /*
             * STEP 5:
             * Verify the final status from the dashboard.
             */
            driver.get(
                    baseUrl + "/reviewer/dashboard"
            );

            wait.until(
                    ExpectedConditions.urlContains(
                            "/reviewer/dashboard"
                    )
            );

            WebElement finalRow =
                    findComplaintRow();

            String finalRowText =
                    finalRow.getText();

            assertTrue(
                    finalRowText.contains(
                            "Approved"
                    )
                            || finalRowText.contains(
                            "APPROVED"
                    ),
                    "Complaint should finally show Approved status"
            );

            takeScreenshot(
                    "test5_final_approved"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test5_approve_failure"
            );

            throw e;
        }
    }
}