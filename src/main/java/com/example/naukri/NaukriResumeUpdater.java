package com.example.naukri;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.nio.file.Paths;
import java.time.Duration;

public class NaukriResumeUpdater {
    public static void main(String[] args) {
        String username = System.getenv("NAUKRI_USERNAME");
        String password = System.getenv("NAUKRI_PASSWORD");
        String resumePath = Paths.get("src/main/resources/Ismail_Shaik.pdf").toAbsolutePath().toString();
//        WebDriver driver = new ChromeDriver();
//        driver.manage().window().maximize();
//        WebDriverManager.chromedriver().setup();
//
//        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome"); // 👈 important for GitHub Actions
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.get("https://www.naukri.com/nlogin/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            System.out.println("Opened web page");
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("usernameField"))).sendKeys(username);
//            driver.findElement(By.id("passwordField")).sendKeys(password);
            WebElement usernameField;
            try {
                usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("usernameField")));
            } catch (TimeoutException e) {
                usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@placeholder='Enter Email ID / Username']")));
            }
            usernameField.sendKeys(username);

            WebElement passwordField;
            try {
                passwordField = driver.findElement(By.id("passwordField"));
            } catch (Exception e) {
                passwordField = driver.findElement(By.xpath("//input[@type='password']"));
            }
            passwordField.sendKeys(password);

            driver.findElement(By.xpath("//button[text()='Login']")).click();
            System.out.println("Clicked on login");
            System.out.println("waiting...");
            Thread.sleep(2000);
            System.out.println("waiting completed");
            driver.get("https://www.naukri.com/mnjuser/profile");
            WebElement updateLink = wait.until(ExpectedConditions
                    .elementToBeClickable(By.cssSelector("a.secondary-content.typ-14Bold")));
            System.out.println("updateLink");
            updateLink.click();
            WebElement fileInput = wait.until(ExpectedConditions
                    .presenceOfElementLocated(By.cssSelector("input[type='file']")));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.display='block';", fileInput);
            System.out.println("fileInput");
            fileInput.sendKeys(resumePath);
            System.out.println("done");
            try {
                WebElement saveBtn = wait.until(ExpectedConditions
                        .elementToBeClickable(By
                                .xpath("//button[contains(text(),'Save') or contains(text(),'Upload')]")));
                System.out.println("Saving");
                saveBtn.click();
            } catch (TimeoutException ignored) {
                System.out.println("No Save button detected, upload may auto-save.");
            }

            System.out.println("✅ Resume uploaded successfully!");

            Thread.sleep(6000); // keep browser open for manual confirmation
        } catch (Exception e) {
            System.out.println("Exception e" + e);
        } finally {
            driver.quit();
        }
    }
}
