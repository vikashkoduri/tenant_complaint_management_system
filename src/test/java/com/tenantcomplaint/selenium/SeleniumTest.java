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
                Duration.ofSeconds(15)
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

            Files.copy(
                    screenshot.toPath(),
                    targetDir.resolve(name + ".png")
            );

        } catch (IOException e) {

            System.err.println(
                    "Failed to save screenshot: "
                            + e.getMessage()
            );
        }
    }

    private void loginAsReviewer() {

        driver.get(baseUrl + "/login");

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("username")
                )
        );

        driver.findElement(By.id("username"))
                .clear();

        driver.findElement(By.id("username"))
                .sendKeys("admin@tenant.com");

        driver.findElement(By.id("password"))
                .clear();

        driver.findElement(By.id("password"))
                .sendKeys("admin123");

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.urlContains(
                        "/reviewer/dashboard"
                )
        );
    }

    private WebElement findComplaintRow() {

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

    private WebElement findStatusSelect() {

        return wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath(
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
                                        "]"
                        )
                )
        );
    }

    private void openComplaintForReview() {

        WebElement row = findComplaintRow();

        WebElement actionLink = null;

        try {

            actionLink = row.findElement(
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

        if (actionLink == null) {

            try {

                actionLink = row.findElement(
                        By.xpath(
                                ".//a[contains(" +
                                        "translate(normalize-space(.)," +
                                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                                        "'abcdefghijklmnopqrstuvwxyz')," +
                                        "'review'" +
                                        ")]"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (actionLink == null) {

            try {

                actionLink = row.findElement(
                        By.xpath(
                                ".//a[contains(" +
                                        "translate(normalize-space(.)," +
                                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                                        "'abcdefghijklmnopqrstuvwxyz')," +
                                        "'edit'" +
                                        ")]"
                        )
                );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (actionLink == null) {

            try {

                actionLink = row.findElement(
                        By.cssSelector("a.btn")
                );

            } catch (NoSuchElementException ignored) {
            }
        }

        if (actionLink == null) {

            try {

                actionLink = row.findElement(
                        By.cssSelector("button")
                );

            } catch (NoSuchElementException ignored) {
            }
        }

        assertNotNull(
                actionLink,
                "Complaint action button/link should be available"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        actionLink
                )
        );

        actionLink.click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath(
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
                                                "]"
                                )
                        ),
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("form")
                        )
                )
        );
    }

    private void selectStatus(String status) {

        WebElement statusSelect =
                findStatusSelect();

        Select select =
                new Select(statusSelect);

        select.selectByVisibleText(status);

        String selectedText =
                select.getFirstSelectedOption()
                        .getText()
                        .trim();

        assertEquals(
                status,
                selectedText,
                "Selected complaint status should be "
                        + status
        );
    }

    private void enterRemarks(String remarks) {

        try {

            WebElement remarksField =
                    driver.findElement(
                            By.id("remarks")
                    );

            remarksField.clear();
            remarksField.sendKeys(remarks);

            return;

        } catch (NoSuchElementException ignored) {
        }

        try {

            WebElement textarea =
                    driver.findElement(
                            By.cssSelector("textarea")
                    );

            textarea.clear();
            textarea.sendKeys(remarks);

        } catch (NoSuchElementException ignored) {

            System.out.println(
                    "Remarks field not available; continuing."
            );
        }
    }

    private void submitStatusForm() {

        WebElement statusSelect =
                findStatusSelect();

        WebElement form =
                statusSelect.findElement(
                        By.xpath("./ancestor::form[1]")
                );

        WebElement submitButton = null;

        try {

            submitButton =
                    form.findElement(
                            By.cssSelector(
                                    "button[type='submit']"
                            )
                    );

        } catch (NoSuchElementException ignored) {
        }

        if (submitButton == null) {

            try {

                submitButton =
                        form.findElement(
                                By.cssSelector(
                                        "input[type='submit']"
                                )
                        );

            } catch (NoSuchElementException ignored) {
            }
        }

        assertNotNull(
                submitButton,
                "Status update submit button should be available"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        submitButton
                )
        );

        submitButton.click();

        wait.until(
                ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".alert-success")
                        ),
                        ExpectedConditions.textToBePresentInElementLocated(
                                By.tagName("body"),
                                "successfully"
                        ),
                        ExpectedConditions.urlContains(
                                "/reviewer"
                        )
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
            ).sendKeys("John Doe");

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
                    submittedReferenceId,
                    "Reference ID should not be null"
            );

            assertFalse(
                    submittedReferenceId.isEmpty(),
                    "Reference ID should not be empty"
            );

            assertTrue(
                    submittedReferenceId.startsWith("COMP-"),
                    "Reference ID should start with COMP-"
            );

            String pageSource =
                    driver.getPageSource();

            assertTrue(
                    pageSource.contains(
                            "Complaint Submitted Successfully"
                    ),
                    "Success message should be displayed"
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
                    submittedReferenceId,
                    "Reference ID from Test 1 must be available"
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

            String pageSource =
                    driver.getPageSource();

            assertTrue(
                    pageSource.contains(
                            submittedReferenceId
                    ),
                    "Reference ID should be displayed"
            );

            assertTrue(
                    pageSource.contains("SUBMITTED")
                            || pageSource.contains("Submitted"),
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
                    submittedReferenceId,
                    "Reference ID from Test 1 must be available"
            );

            loginAsReviewer();

            String pageSource =
                    driver.getPageSource();

            assertTrue(
                    pageSource.contains(
                            "Reviewer Dashboard"
                    ),
                    "Reviewer Dashboard should be visible"
            );

            assertTrue(
                    pageSource.contains(
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

            openComplaintForReview();

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    ".detail-grid"
                            )
                    )
            );

            String detailsPage =
                    driver.getPageSource();

            assertTrue(
                    detailsPage.contains(
                            submittedReferenceId
                    )
                            || detailsPage.contains(
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
                    "Reference ID from Test 1 must be available"
            );

            loginAsReviewer();

            findComplaintRow();

            openComplaintForReview();

            WebElement statusSelect =
                    findStatusSelect();

            assertNotNull(
                    statusSelect,
                    "Reviewer status dropdown should exist"
            );

            Select statusDropdown =
                    new Select(statusSelect);

            var options =
                    statusDropdown.getOptions();

            assertTrue(
                    options.stream()
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
                    options.stream()
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
                    options.stream()
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

            selectStatus(
                    "Under Review"
            );

            enterRemarks(
                    "Taking this complaint for review"
            );

            submitStatusForm();

            takeScreenshot(
                    "test5_under_review"
            );

            driver.get(
                    baseUrl
                            + "/reviewer/dashboard"
            );

            findComplaintRow();

            openComplaintForReview();

            findStatusSelect();

            selectStatus(
                    "Approved"
            );

            enterRemarks(
                    "Issue has been resolved. Approving complaint."
            );

            submitStatusForm();

            takeScreenshot(
                    "test5_approve_success"
            );

            String finalPage =
                    driver.getPageSource();

            assertTrue(
                    finalPage.contains(
                            "Approved"
                    )
                            || finalPage.contains(
                            "APPROVED"
                    ),
                    "Complaint should show Approved status"
            );

        } catch (Exception e) {

            takeScreenshot(
                    "test5_approve_failure"
            );

            throw e;
        }
    }
}