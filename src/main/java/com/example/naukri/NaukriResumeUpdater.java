package com.example.naukri;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
        options.addArguments("--headless=new");

        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // Always login first
            doLogin(driver, wait, username, password);

            // Navigate to profile
            driver.get("https://www.naukri.com/mnjuser/profile");

            // If redirected back to login, try login again
            if (driver.getCurrentUrl().contains("nlogin")) {
                System.out.println("⚠️ Redirected to login again, retrying...");
                doLogin(driver, wait, username, password);
                driver.get("https://www.naukri.com/mnjuser/profile");
            }

            // Find Update link
            WebElement updateLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[normalize-space()='Update' and contains(@class,'secondary-content')]")));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", updateLink);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", updateLink);

            System.out.println("✅ Clicked on Update Resume link");

            // File input
            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", fileInput);
            fileInput.sendKeys(resumePath);

            System.out.println("📄 Resume file selected");

            // Save/Upload button
            try {
                WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Save') or contains(text(),'Upload')]")));
                saveBtn.click();
                System.out.println("💾 Save/Upload clicked");
            } catch (TimeoutException ignored) {
                System.out.println("ℹ️ No Save button detected, upload may auto-save.");
            }

            System.out.println("✅ Resume uploaded successfully!");
            Thread.sleep(5000);

        } catch (Exception e) {
            System.out.println("❌ Exception: " + e);
            saveScreenshot(driver);
            saveDomDump(driver);
        } finally {
            driver.quit();
        }
    }

    private static void doLogin(WebDriver driver, WebDriverWait wait, String username, String password) {
        try {
            System.out.println("🔐 Performing login...");
            driver.get("https://www.naukri.com/nlogin/login");

            WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@id='usernameField' or @placeholder='Enter Email ID / Username']")));
            usernameField.clear();
            usernameField.sendKeys(username);

            WebElement passwordField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@id='passwordField' or @type='password']")));
            passwordField.clear();
            passwordField.sendKeys(password);

            WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Login']")));
            loginBtn.click();

            // Wait for a post-login element
            wait.until(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(),'My Naukri')]")), ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class,'nI-gNb-drawer__bars')]"))));

            System.out.println("✅ Login successful");
        } catch (Exception e) {
            System.out.println("❌ Login failed: " + e);
            saveScreenshot(driver);
            saveDomDump(driver);
            throw e;
        }
    }

    private static void saveScreenshot(WebDriver driver) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File("build/screenshots/debug_screenshot.png");
            dest.getParentFile().mkdirs();
            src.renameTo(dest);
            System.out.println("📸 Saved screenshot to: " + dest.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠️ Could not save screenshot: " + e);
        }
    }

    private static void saveDomDump(WebDriver driver) {
        try {
            String pageSource = driver.getPageSource();
            File domFile = new File("build/screenshots/dom-dump.html");
            domFile.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(domFile)) {
                fw.write(pageSource);
            }
            System.out.println("📝 Saved DOM dump to: " + domFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("⚠️ Could not save DOM dump: " + e);
        }
    }
}

