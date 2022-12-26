import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.net.URL
import java.time.Duration

interface ScrapperInterface {
    var result: List<Result>?
    val onScrappedResult: (Result) -> Unit
    fun load()
    fun scrapSafely()
}

class Scrapper(override val onScrappedResult: (Result) -> Unit): ScrapperInterface {
    private var driver: ChromeDriver = createDriver()
    override var result: List<Result>? = null

    private fun createDriver(): ChromeDriver {
        val options = ChromeOptions();
        options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
        return ChromeDriver(options)
    }

    override fun load() {
        loadMercadonaPage()
    }

    override fun scrapSafely() {
        try {
            scrap()
        } catch (error: Exception) {
            error.printStackTrace()
            if (error is StaleElementReferenceException) {
                scrapSafely()
            }
        }
        driver.quit()
    }


    private fun extractCategoryAndProductsInfo(driver: ChromeDriver): List<Result> {
        val subCategoriesWeb = driver.findElements(By.className("category-item"))
        return subCategoriesWeb.map {
            safeClick(driver, it)
            val url = URL(driver.currentUrl)
            val idString = url.path.split("/").last()
            val category = Category(id = idString, name = it.text)
            println("Scrapping category named ${category.name} ...")
            val products = extractProductsInfo(driver, category)
            println("${category.name} successfully scrapped")
            val resultCategory = Result(category, products)
            onScrappedResult(resultCategory)
            return@map resultCategory
        }
    }

    private fun safeClick(driver: ChromeDriver, element: WebElement) {
        val wait = WebDriverWait(driver, Duration.ofMillis(6000))
        wait.until(ExpectedConditions.elementToBeClickable(element)).click()
    }


    private fun loadMercadonaPage() {
        // Set the URL of the web page you want to scrape
        val number = 112
        val url = "https://tienda.mercadona.es/categories/$number"

        driver.get(url)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        val elementText = driver.findElement(By.name("postalCode"))

        driver.findElement(By.xpath("/html/body/div[1]/div[1]/div/div/button[2]")).click()

        elementText.sendKeys("08007")
        elementText.sendKeys(Keys.RETURN)
        println("$url loaded successfully")
    }

    private fun parseCategoriesAndProducts(driver: ChromeDriver): List<Result> {
        val elements = driver.findElements(By.xpath("/html/body/div[1]/div[2]/div[1]/ul/li"))

        return elements.map {
            safeClick(driver, it)
            return@map extractCategoryAndProductsInfo(driver)
        }.flatten()
    }

    private fun extractProductsInfo(driver: ChromeDriver, category: Category): List<Product> {
        val sections = driver.findElements(By.xpath("/html/body/div[1]/div[2]/div[2]/div[1]/div/div/section"))
        val productes = ArrayList<List<Product>>()
        for (section in sections) {
            val elements = section.findElements(By.xpath("./div/div"))

            val products: List<Product> = elements.map {
                val image = findElementSafely("./button/div[1]/img", it)?.getAttribute("src")
                val name = findElementSafely("./button/div[2]/h4", it)?.text ?: ""
                val price = findElementSafely("./button/div[2]/div[2]/p[1]", it)?.text ?: ""
                val extraPrice = findElementSafely("./button/div[2]/div[2]/p[2]", it)?.text
                val size = findElementSafely("./button/div[2]/div[1]/span[1]", it)?.text/*+ " " +
                    findElementSafely("./button/div[2]/div[1]/span[2]", it)?.text*/
//            extractProductId(driver, it)
                return@map Product(name, image, price, extraPrice, size, category.id)
            }
            productes.add(products)
        }

        return productes.flatten()
    }

    private fun findElementSafely(xpath: String, element: WebElement): WebElement? {
        return try {
            element.findElement(By.xpath(xpath))
        } catch (e: NoSuchElementException) {
            null
        }
    }
    private fun scrap() {
        result = parseCategoriesAndProducts(driver)
    }


//fun extractProductId(driver: ChromeDriver, element: WebElement): Int {
//    val buttonElement = element.findElement(By.cssSelector("button.product-cell__content-link"))
//    val previousURL = driver.currentUrl

//    safeClick(driver, buttonElement)
//    val productURL = driver.currentUrl
//    val idString = productURL.split("/").dropLast(1).last()
//    safeClick(driver, driver.findElement(By.className("modal-content__close")))
////    driver.get(previousURL)
//    return Integer.parseInt(idString)
//}

}