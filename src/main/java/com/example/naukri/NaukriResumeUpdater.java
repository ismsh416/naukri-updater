package com.example.naukri;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

public class NaukriResumeUpdater {

    public static void main(String[] args) throws MalformedURLException {
        String username = System.getenv("NAUKRI_USERNAME");
        String password = System.getenv("NAUKRI_PASSWORD");
        String resumePath = Paths.get("src/main/resources/Ismail_Shaik.pdf").toAbsolutePath().toString();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--headless=new"); // headless for CI/CD

        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // ✅ Correct login URL
            driver.get("https://www.naukri.com/nlogin/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            System.out.println("Opened login page");

            // ✅ Try dismissing popup/cookie
            try {
                WebElement closePopup = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("div[ng-click*='dismiss'], button[aria-label='Close'], span[title='Close']")));
                closePopup.click();
                System.out.println("Closed popup");
            } catch (TimeoutException ignored) {
                System.out.println("No popup detected");
            }

            // ✅ Locate username field (id or placeholder)
            WebElement usernameField;
            try {
                usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("usernameField")));
            } catch (TimeoutException e) {
                usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[contains(@placeholder, 'Email')]")));
            }
            usernameField.sendKeys(username);

            // ✅ Locate password field (id or type=password)
            WebElement passwordField;
            try {
                passwordField = driver.findElement(By.id("passwordField"));
            } catch (NoSuchElementException e) {
                passwordField = driver.findElement(By.xpath("//input[@type='password']"));
            }
            passwordField.sendKeys(password);

            // ✅ Click login button
            WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Login')]")));
            loginBtn.click();
            System.out.println("Clicked on login");

            // ✅ Go to profile page
            driver.get("https://www.naukri.com/mnjuser/profile");

            // ✅ Click update resume
            WebElement updateLink = wait.until(ExpectedConditions
                    .elementToBeClickable(By.cssSelector("a.secondary-content.typ-14Bold")));
            updateLink.click();
            System.out.println("Clicked update resume link");

            // ✅ Upload resume file
            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", fileInput);
            fileInput.sendKeys(resumePath);
            System.out.println("Resume file selected: " + resumePath);

            // ✅ Click Save/Upload if available
            try {
                WebElement saveBtn = wait.until(ExpectedConditions
                        .elementToBeClickable(By.xpath("//button[contains(text(),'Save') or contains(text(),'Upload')]")));
                saveBtn.click();
                System.out.println("Clicked Save/Upload button");
            } catch (TimeoutException ignored) {
                System.out.println("No Save button detected, upload may auto-save.");
            }

            System.out.println("✅ Resume uploaded successfully!");
            Thread.sleep(5000);

        } catch (Exception e) {
            System.out.println("❌ Exception: " + e);

            // Debug screenshot
            try {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Files.copy(screenshot.toPath(), Paths.get("debug_screenshot.png"));
                System.out.println("Saved screenshot to debug_screenshot.png");
            } catch (Exception ex) {
                System.out.println("Failed to capture screenshot: " + ex);
            }

        } finally {
            driver.quit();
        }
    }
}