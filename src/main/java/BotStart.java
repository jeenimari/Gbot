import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;

public class BotStart {
    private static final String COOKIE_PATH = "cookies.json";

    public static void main(String[] args) {
        // ✅ 자동으로 chromedriver 다운로드 및 설정
        WebDriverManager.chromedriver().setup();


        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Set<String> visitedSellers = new HashSet<>();

        try {
            driver.get("https://www.gmarket.co.kr");
            System.out.println("🟢 G마켓 접속 완료");

            Thread.sleep(1000);

            ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            Thread.sleep(1000 + new Random().nextInt(2000));

            loadCookies(driver, COOKIE_PATH);
            driver.navigate().refresh();
            Thread.sleep(2000);

            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("form__search-keyword")));
            //검색어 입력
            String keyword = "";
            for (char ch : keyword.toCharArray()) {
                searchBox.sendKeys(String.valueOf(ch));
                Thread.sleep(160);
            }

            Thread.sleep(800);
            WebElement searchButton = driver.findElement(By.className("button__search"));
            searchButton.click();
            Thread.sleep(3500);
            System.out.println("🔍 검색 완료");

            List<WebElement> products = driver.findElements(By.cssSelector("a.link__item"));
            List<String> productUrls = new ArrayList<>();
            for (WebElement product : products) {
                try {
                    String url = product.getAttribute("href");
                    if (url != null && !url.isEmpty()) {
                        productUrls.add(url);
                    }
                } catch (StaleElementReferenceException e) {
                    System.out.println("⚠️ 상품 요소가 무효화됨");
                }
            }

            int count = 0;
            for (String productUrl : productUrls) {
                if (count >= 25) break;

                try {
                    driver.navigate().to(productUrl);
                    Thread.sleep(2500);

                    // goodscode 추출
                    String goodscode = "";
                    String url = driver.getCurrentUrl();
                    if (url.contains("goodscode=")) {
                        goodscode = url.split("goodscode=")[1].split("&")[0];
                    }

                    // 미니샵 링크 클릭
                    String miniShopUrl = "";
                    try {
                        WebElement miniShopLink = driver.findElement(By.cssSelector("span.text__seller > a"));
                         miniShopUrl = miniShopLink.getAttribute("href");
                        if (miniShopUrl != null && !miniShopUrl.isEmpty()) {
                            driver.navigate().to(miniShopUrl);
                            Thread.sleep(2000);
                        } else {
                            System.out.println("❌ 미니샵 링크 없음 - 건너뜀");
                            continue;
                        }
                    } catch (NoSuchElementException e) {
                        System.out.println("❌ 미니샵 링크 요소 없음 - 건너뜀");
                        continue;
                    }

                    WebElement infoButton = driver.findElement(By.id("seller_info"));
                    infoButton.click();
                    Thread.sleep(1200);

                    String shopName = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='상호']/following-sibling::dd[1]"));
                    String phone = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='전화번호']/following-sibling::dd[1]"));
                    String uniqueKey = shopName + "|" + phone;

                    if (visitedSellers.contains(uniqueKey)) {
                        System.out.println("⚠️ 중복된 판매자 - 건너뜀: " + shopName);
                        continue;
                    }
                    if(!phone.startsWith("010")&& !phone.startsWith("010-")){
                        System.out.println("️️010 번호가 아님 제외"+ phone);
                        continue;
                    }
                    visitedSellers.add(uniqueKey);

                    String ceo = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='대표자']/following-sibling::dd[1]"));
                    String email = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='이메일']/following-sibling::dd[1]/a"));
                    String bizNo = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='사업자번호']/following-sibling::dd[1]"));
                    String address = getTextSafely(driver, By.xpath("//div[@class='seller_info_box']//dt[text()='영업소재지']/following-sibling::dd[1]"));

                    System.out.println("📦 판매자 정보 (" + (count + 1) + ")");
                    System.out.println("goodscode: " + goodscode);
                    System.out.println("상호: " + shopName);
                    System.out.println("대표자: " + ceo);
                    System.out.println("전화: " + phone);
                    System.out.println("이메일: " + email);
                    System.out.println("사업자번호: " + bizNo);
                    System.out.println("주소: " + address);
                    System.out.println("미니샵 URL: " + miniShopUrl);
                    System.out.println("--------------------");


                    count++;

                } catch (Exception e) {
                    System.out.println("⚠️ 판매자 정보 처리 중 오류: " + e.getMessage());
                }
            }

            saveCookies(driver, COOKIE_PATH);

        } catch (Exception e) {
            System.out.println("❌ 오류 발생: " + e.getMessage());
        } finally {
            System.out.println("🔚 종료하려면 Enter");
            new Scanner(System.in).nextLine();
            driver.quit();
        }
    }

    private static String getTextSafely(WebDriver driver, By by) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            String text = element.getText().trim();
            return text.isEmpty() ? "정보 없음" : text;
        } catch (Exception e) {
            return "정보 없음";
        }
    }

    private static void saveCookies(WebDriver driver, String path) {
        try (Writer writer = new FileWriter(path)) {
            Set<Cookie> cookies = driver.manage().getCookies();
            List<Map<String, Object>> cookieList = new ArrayList<>();

            for (Cookie cookie : cookies) {
                if (cookie.getExpiry() != null && cookie.getExpiry().getTime() < System.currentTimeMillis()) {
                    continue;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("name", cookie.getName());
                map.put("value", cookie.getValue());
                map.put("domain", cookie.getDomain());
                map.put("path", cookie.getPath());
                map.put("secure", cookie.isSecure());
                map.put("httpOnly", cookie.isHttpOnly());
                if (cookie.getExpiry() != null) {
                    map.put("expiry", cookie.getExpiry().getTime() / 1000);
                }
                cookieList.add(map);
            }

            new Gson().toJson(cookieList, writer);
            System.out.println("✅ 쿠키 저장 완료 (" + cookieList.size() + "개)");
        } catch (IOException e) {
            System.out.println("❌ 쿠키 저장 실패: " + e.getMessage());
        }
    }

    private static void loadCookies(WebDriver driver, String path) {
        try (Reader reader = new FileReader(path)) {
            Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> cookieList = new Gson().fromJson(reader, type);

            if (cookieList == null || cookieList.isEmpty()) {
                System.out.println("⚠️ 저장된 쿠키가 없습니다.");
                return;
            }

            long now = System.currentTimeMillis() / 1000;
            int loadedCount = 0;

            for (Map<String, Object> map : cookieList) {
                try {
                    if (map.containsKey("expiry")) {
                        double expiry = (Double) map.get("expiry");
                        if (expiry < now) continue;
                    }

                    Cookie.Builder builder = new Cookie.Builder((String) map.get("name"), (String) map.get("value"))
                            .domain((String) map.get("domain"))
                            .path((String) map.get("path"))
                            .isSecure((Boolean) map.getOrDefault("secure", false))
                            .isHttpOnly((Boolean) map.getOrDefault("httpOnly", false));

                    if (map.containsKey("expiry")) {
                        builder.expiresOn(new Date((long) ((Double) map.get("expiry") * 1000)));
                    }

                    Cookie cookie = builder.build();
                    driver.manage().addCookie(cookie);
                    loadedCount++;
                } catch (Exception e) {
                    System.out.println("⚠️ 쿠키 로드 실패: " + map.get("name") + " - " + e.getMessage());
                }
            }

            System.out.println("✅ 쿠키 로드 완료 (" + loadedCount + "개)");
        } catch (FileNotFoundException e) {
            System.out.println("⚠️ 쿠키 파일 없음 - 첫 실행입니다.");
        } catch (IOException e) {
            System.out.println("❌ 쿠키 파일 읽기 실패: " + e.getMessage());
        }
    }
}
