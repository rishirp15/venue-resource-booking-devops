package com.rishi.venue.selenium;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.VenueRepository;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seleniume2etest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VenueBookingE2ESeleniumTest {

    @LocalServerPort
    private int port;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private WebDriver driver;

    private static final String REG_USER_NAME = "Selenium New User";
    private static final String REG_USER_EMAIL = "selenium_new_user@venue.com";
    private static final String REG_USER_PASS = "NewPass123";

    private static final String DEMO_USER_EMAIL = "user@venue.com";
    private static final String DEMO_USER_PASS = "user123";

    private static final String ADMIN_EMAIL = "admin@venue.com";
    private static final String ADMIN_PASS = "admin123";

    private static final String TEST_PURPOSE = "E2E Automated Testing Conference";

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void initDriver() {
        HtmlUnitDriver hud = new HtmlUnitDriver(true);
        hud.getWebClient().getOptions().setThrowExceptionOnScriptError(false);
        this.driver = hud;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    private void loginAs(String email, String password) {
        driver.get(getBaseUrl() + "/login");
        WebElement emailInput = driver.findElement(By.name("email"));
        WebElement passInput = driver.findElement(By.name("password"));
        emailInput.sendKeys(email);
        passInput.sendKeys(password);
        driver.findElement(By.tagName("form")).submit();
    }

    @Test
    @Order(1)
    @DisplayName("Scenario 1: End-to-End User Registration Journey")
    void testUserRegistrationJourney() {
        driver.get(getBaseUrl() + "/register");

        assertTrue(driver.getPageSource().contains("Create Account") ||
                   driver.getPageSource().contains("Register"),
                "Registration page should render correctly");

        WebElement nameInput = driver.findElement(By.name("name"));
        WebElement emailInput = driver.findElement(By.name("email"));
        WebElement passInput = driver.findElement(By.name("password"));

        nameInput.sendKeys(REG_USER_NAME);
        emailInput.sendKeys(REG_USER_EMAIL);
        passInput.sendKeys(REG_USER_PASS);

        WebElement form = driver.findElement(By.tagName("form"));
        form.submit();

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/login"),
                "User should be redirected to login page after successful registration");
        assertTrue(driver.getPageSource().contains("Account created successfully") ||
                   currentUrl.contains("registered"),
                "Registration success notification should be visible");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario 2: End-to-End User Login Journey")
    void testUserLoginJourney() {
        loginAs(DEMO_USER_EMAIL, DEMO_USER_PASS);

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Welcome back") ||
                   pageSource.contains("Dashboard") ||
                   pageSource.contains("Demo User") ||
                   pageSource.contains("Book a Venue"),
                "Logged-in user dashboard should display user name or dashboard elements");
    }

    @Test
    @Order(3)
    @DisplayName("Scenario 3: End-to-End Venue Browsing Journey")
    void testVenueBrowsingJourney() {
        loginAs(DEMO_USER_EMAIL, DEMO_USER_PASS);
        driver.get(getBaseUrl() + "/venues");

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Explore Available Venues") || pageSource.contains("Venues"),
                "Venue catalog page should render");

        List<Venue> venues = venueRepository.findAll();
        assertFalse(venues.isEmpty(), "At least one venue should exist in the repository");

        for (Venue v : venues) {
            if (v.isActive()) {
                assertTrue(pageSource.contains(v.getName()),
                        "Active venue '" + v.getName() + "' should be visible in the catalog");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Scenario 4: End-to-End Booking Submission Journey")
    void testBookingSubmissionJourney() {
        loginAs(DEMO_USER_EMAIL, DEMO_USER_PASS);

        Venue targetVenue = venueRepository.findAll().stream()
                .filter(Venue::isActive)
                .findFirst()
                .orElseThrow();

        driver.get(getBaseUrl() + "/book?venueId=" + targetVenue.getId());

        String targetDate = LocalDate.now().plusDays(10).toString();

        WebElement dateInput = driver.findElement(By.name("bookingDate"));
        dateInput.clear();
        dateInput.sendKeys(targetDate);

        if (driver instanceof JavascriptExecutor js) {
            js.executeScript("document.getElementById('startTime').value = '09:00'; document.getElementById('startTime').setAttribute('value', '09:00');");
            js.executeScript("document.getElementById('endTime').value = '11:00'; document.getElementById('endTime').setAttribute('value', '11:00');");
        }

        WebElement attendeesInput = driver.findElement(By.name("attendees"));
        attendeesInput.clear();
        attendeesInput.sendKeys("15");

        WebElement purposeInput = driver.findElement(By.name("purpose"));
        purposeInput.clear();
        purposeInput.sendKeys(TEST_PURPOSE);

        // Submit booking form
        WebElement bookingForm = driver.findElement(By.cssSelector("form[action*='/book'][method='post']"));
        bookingForm.submit();

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/my-bookings"),
                "User should be redirected to my-bookings upon submission. Actual URL: " + currentUrl);

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("PENDING") || pageSource.contains(targetVenue.getName()),
                "Newly submitted booking should appear with PENDING status in history");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario 5: End-to-End Admin Login Journey")
    void testAdminLoginJourney() {
        loginAs(ADMIN_EMAIL, ADMIN_PASS);

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/admin") || currentUrl.equals(getBaseUrl() + "/"),
                "Admin user should be directed to administrative area");

        driver.get(getBaseUrl() + "/admin/dashboard");
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Admin Dashboard") ||
                   pageSource.contains("Total Bookings") ||
                   pageSource.contains("System Statistics"),
                "Admin dashboard statistics should render for admin");
    }

    @Test
    @Order(6)
    @DisplayName("Scenario 6: End-to-End Admin Booking Approval Journey")
    void testAdminBookingApprovalJourney() {
        loginAs(ADMIN_EMAIL, ADMIN_PASS);
        driver.get(getBaseUrl() + "/admin/bookings?status=PENDING");

        List<Booking> pending = bookingRepository.findByStatusOrderByBookingDateDesc("PENDING");
        assertFalse(pending.isEmpty(), "Pending bookings should exist");

        Booking target = pending.get(0);

        WebElement approveForm = driver.findElement(By.cssSelector("form[action*='/admin/bookings/" + target.getId() + "/approve']"));
        assertNotNull(approveForm, "Approval form should be visible on admin bookings page");
        approveForm.submit();

        Booking updated = bookingRepository.findById(target.getId()).orElseThrow();
        assertEquals("APPROVED", updated.getStatus(),
                "Booking status must transition to APPROVED after admin approval");
    }

    @Test
    @Order(7)
    @DisplayName("Scenario 7: End-to-End User Checks Approved Booking Status Journey")
    void testUserChecksApprovedBookingStatusJourney() {
        loginAs(DEMO_USER_EMAIL, DEMO_USER_PASS);
        driver.get(getBaseUrl() + "/my-bookings");

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Booking History") || pageSource.contains("My Bookings"),
                "User bookings page must list booking history");
        assertTrue(pageSource.contains("APPROVED"),
                "User bookings page must display APPROVED status badge for approved booking");
    }
}
