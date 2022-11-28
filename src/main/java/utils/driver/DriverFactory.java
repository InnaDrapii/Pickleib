package utils.driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import utils.Printer;
import utils.StringUtilities;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import static resources.Colors.*;

public class DriverFactory {

    private static final Printer log = new Printer(DriverFactory.class);

    public static RemoteWebDriver getDriver(String driverName){
        Properties properties = new Properties();
        StringUtilities strUtils = new StringUtilities();
        int frameWidth = Integer.parseInt(properties.getProperty("frame-width","1920"));
        int frameHeight = Integer.parseInt(properties.getProperty("frame-height","1080"));
        long timeout = Long.parseLong(properties.getProperty("driver-timeout", "15000"))/1000;
        boolean headless = Boolean.parseBoolean(properties.getProperty("headless", "false"));
        boolean deleteCookies = Boolean.parseBoolean(properties.getProperty("delete-cookies", "false"));
        boolean maximise = Boolean.parseBoolean(properties.getProperty("driver-maximize", "false"));
        RemoteWebDriver driver;
        try {
            properties.load(new FileReader("src/test/resources/test.properties"));

            if (driverName == null) driverName = strUtils.firstLetterCapped(properties.getProperty("browser", "chrome"));

            if (Boolean.parseBoolean(properties.getProperty("selenium-grid", "false"))){
                ImmutableCapabilities capabilities;
                switch (driverName.toLowerCase()) {
                    case "chrome" -> capabilities = new ImmutableCapabilities("browserName", "chrome");
                    case "firefox" -> capabilities = new ImmutableCapabilities("browserName", "firefox");
                    case "opera" -> capabilities = new ImmutableCapabilities("browserName", "opera");
                    default -> {
                        capabilities = null;
                        Assert.fail(YELLOW + "The driver type \"" + driverName + "\" was undefined." + RESET);
                    }
                }
                driver = new RemoteWebDriver(new URL(properties.getProperty("hub-url","")), capabilities);
            }
            else {
                driver = driverSwitch(headless, frameWidth, frameHeight, driverName);}
            assert driver != null;
            driver.manage().window().setSize(new Dimension(frameWidth, frameHeight));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
            if (deleteCookies) driver.manage().deleteAllCookies();
            if (maximise) driver.manage().window().maximize();
            log.new Important(driverName + GRAY + " was selected");
            return driver;
        }
        catch (SessionNotCreatedException sessionException){
            if (sessionException.getLocalizedMessage().contains("Could not start a new session. Response code 500. Message: session not created: This version of")){
                driver = driverSwitch(headless, frameWidth, frameHeight, driverName);
                assert driver != null;
                driver.manage().window().setSize(new Dimension(frameWidth, frameHeight));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
                if (deleteCookies) driver.manage().deleteAllCookies();
                if (maximise) driver.manage().window().maximize();
                log.new Important(driverName + GRAY + " was selected");
                return driver;
            }
            else {throw new RuntimeException(sessionException);}
        }
        catch (IOException malformedURLException) {throw new RuntimeException(malformedURLException);}
        catch (Exception gamma) {
            if(gamma.toString().contains("Could not start a new session. Possible causes are invalid address of the remote server or browser start-up failure")){
                log.new Info("Please make sure the "+PURPLE+"Selenium Grid "+GRAY+"is on & verify the port that its running on at 'resources/test.properties'."+RESET);
                throw new RuntimeException(gamma);
            }
            else
            {throw new RuntimeException(YELLOW+"Something went wrong while selecting a driver "+"\n\t"+RED+gamma+RESET);}
        }
    }

    static RemoteWebDriver driverSwitch(Boolean headless, Integer frameWidth, Integer frameHeight, String driverName){
        RemoteWebDriver driver;
        switch (Objects.requireNonNull(driverName).toLowerCase()) {
            case "chrome" -> {
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("disable-notifications");
                chromeOptions.setHeadless(headless);
                chromeOptions.addArguments("window-size=" + frameWidth + "," + frameHeight);
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(chromeOptions);
            }
            case "firefox" -> {
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("disable-notifications");
                firefoxOptions.setHeadless(headless);
                firefoxOptions.addArguments("window-size=" + frameWidth + "," + frameHeight);
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver(firefoxOptions);
            }
            case "safari" -> {
                SafariOptions safariOptions = new SafariOptions();
                WebDriverManager.safaridriver().setup();
                driver = new SafariDriver(safariOptions);
            }
            default -> {
                Assert.fail("No such driver was defined.");
                return null;
            }
        }
        return driver;
    }
}
