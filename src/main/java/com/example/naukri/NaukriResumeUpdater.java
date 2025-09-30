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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class NaukriResumeUpdater {

    private static final Path SCREENSHOT_PATH = Paths.get("build", "screenshots", "debug_screenshot.png");

    public static void main(String[] args) throws MalformedURLException {
        String username = System.getenv("NAUKRI_USERNAME");
        String password = System.getenv("NAUKRI_PASSWORD");
        String resumePath = Paths.get("src", "main", "resources", "Ismail_Shaik.pdf").toAbsolutePath().toString();

        if (username == null || password == null) {
            System.out.println("Please set NAUKRI_USERNAME and NAUKRI_PASSWORD environment variables.");
            return;
        }

        ChromeOptions options = new ChromeOptions();

        // Standard flags for running in CI / container
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        // Keep headless for CI; remove this line if testing locally with UI
        options.addArguments("--headless=new");

        // Anti-detection / fingerprint tweaks (may help)
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Set a realistic user-agent
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        // Prevent webdriver property exposure in some cases
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // Connect to Selenium service (service container / local grid)
        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            System.out.println("Opening login page...");
            driver.get("https://www.naukri.com/nlogin/login");

            // quick check if server returned an Access Denied right away
            if (isAccessDenied(driver)) {
                System.out.println("Server returned Access Denied immediately after GET. Saving screenshot and exiting.");
                saveScreenshot(driver);
                return;
            }

            // attempt to close common popups (cookie banner / overlays)
            dismissPopups(driver);

            // locate username field (try id, then placeholder, then fallback by name)
            WebElement usernameField;
            try {
                usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("usernameField")));
            } catch (TimeoutException e1) {
                try {
                    usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[contains(@placeholder,'Email') or contains(@placeholder,'Username')]")));
                } catch (TimeoutException e2) {
                    // last resort: any input[type='text'] on the page
                    usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='text' or @type='email']")));
                }
            }

            // type username like a human
            humanType(usernameField, username);

            // password field
            WebElement passwordField;
            try {
                passwordField = driver.findElement(By.id("passwordField"));
            } catch (NoSuchElementException ex) {
                passwordField = driver.findElement(By.xpath("//input[@type='password']"));
            }
            humanType(passwordField, password);

            // click login (try several possible locators)
            try {
                WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),'login') or contains(., 'Sign in')]")));
                loginBtn.click();
            } catch (TimeoutException e) {
                System.out.println("Login button not found using primary xpath; trying alternative selectors.");
                try {
                    WebElement alt = driver.findElement(By.cssSelector("button[type='submit']"));
                    alt.click();
                } catch (Exception ex) {
                    System.out.println("Failed to click login. Saving screenshot for debug.");
                    saveScreenshot(driver);
                    return;
                }
            }

            // small wait + check for access denied after login click
            Thread.sleep(2000);
            if (isAccessDenied(driver)) {
                System.out.println("Server returned Access Denied after clicking login. Saving screenshot and exiting.");
                saveScreenshot(driver);
                return;
            }

            // navigate to profile
            System.out.println("Navigating to profile page...");
            driver.get("https://www.naukri.com/mnjuser/profile");

            // wait and click update resume
            WebElement updateLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a.secondary-content.typ-14Bold, a[title*='Update']")));
            updateLink.click();

            // locate file input and upload file
            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block';", fileInput);
            fileInput.sendKeys(resumePath);
            System.out.println("Resume file selected: " + resumePath);

            // click save/upload if available
            try {
                WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(., 'Save') or contains(., 'Upload')]")));
                saveBtn.click();
                System.out.println("Clicked Save/Upload");
            } catch (TimeoutException ignored) {
                System.out.println("Save/Upload button not found — upload may auto-save.");
            }

            System.out.println("✅ Resume upload attempted. Sleeping briefly to let server process...");
            Thread.sleep(4000);

            // final Access Denied check
            if (isAccessDenied(driver)) {
                System.out.println("Access Denied detected after upload. Saved screenshot.");
                saveScreenshot(driver);
            } else {
                System.out.println("Finished — no Access Denied detected (may still require manual verification).");
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e);
            saveScreenshot(driver);
        } finally {
            driver.quit();
        }
    }

    private static void humanType(WebElement element, String text) {
        element.click();
        // type char-by-char with tiny random delay
        for (char ch : text.toCharArray()) {
            element.sendKeys(Character.toString(ch));
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(50, 160)); // human-ish delay 50-160ms
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void dismissPopups(WebDriver driver) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            // common cookie / close buttons
            By[] selectors = new By[]{
                    By.cssSelector("button[aria-label='Close']"),
                    By.cssSelector("button[title='Close']"),
                    By.cssSelector("div.cookie-banner button, .cookie-consent button"),
                    By.xpath("//*[contains(text(),'Accept') and (self::button or self::a)]"),
                    By.xpath("//*[contains(text(),'I agree') and (self::button or self::a)]")
            };
            for (By sel : selectors) {
                try {
                    WebElement el = shortWait.until(ExpectedConditions.elementToBeClickable(sel));
                    el.click();
                    System.out.println("Dismissed popup using: " + sel);
                    Thread.sleep(300);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isAccessDenied(WebDriver driver) {
        try {
            String page = driver.getPageSource();
            if (page == null) return false;
            String lower = page.toLowerCase();
            // check phrases commonly present on Akamai/Access Denied pages
            return lower.contains("access denied") || lower.contains("reference #") || lower.contains("you don't have permission to access");
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveScreenshot(WebDriver driver) {
        try {
            // ensure folder exists
            Path folder = SCREENSHOT_PATH.getParent();
            if (folder != null && !Files.exists(folder)) {
                Files.createDirectories(folder);
            }
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), SCREENSHOT_PATH);
            System.out.println("Saved screenshot to: " + SCREENSHOT_PATH.toAbsolutePath());
        } catch (Exception ex) {
            System.out.println("Failed to save screenshot: " + ex);
        }
    }
}