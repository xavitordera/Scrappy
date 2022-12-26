import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.net.URL
import java.time.Duration

class BonPreuScrapper(override val onScrappedResult: (Result) -> Unit) : ScrapperInterface {
    private var driver: ChromeDriver = createDriver()
    override var result: List<Result>? = null

    private fun createDriver(): ChromeDriver {
        val options = ChromeOptions();
        options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
        return ChromeDriver(options)
    }

    override fun scrapSafely() {
        try {
            scrap()
        } catch (error: Exception) {
            error.printStackTrace()
            if (error is StaleElementReferenceException) {
                setupProductsPage()
                scrapSafely()
            }
        }
        driver.quit()
    }

    override fun load() {
        loadBonPreuPage()
    }


    private fun extractCategoryAndProductsInfo(driver: ChromeDriver): List<Result> {
        val subCategoriesWeb = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[1]/div[2]/div[2]/ul/li/a"))
        val results: ArrayList<List<Result>> = arrayListOf()
        subCategoriesWeb.forEach { parentCategory ->
            safeClick(parentCategory)
            safeClick(parentCategory)
            val url = URL(driver.currentUrl)
            val idString = url.toString().split("=").last()
            val category = Category(id = idString, name = parentCategory.text, 2)
//            safeClick(parentCategory)
            println("Scrapping category named ${category.name} ...")
            val products = extractProductsInfo(driver, category)
            println("${category.name} successfully scrapped")
//            val subCategories = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[1]/div[2]/div[2]/ul/li"))
//            results.add(
//                return Result(category, products)
//            )
            driver.navigate().back()
        }
        return emptyList()
    }

    private fun safeClick(element: WebElement) {
        val wait = WebDriverWait(driver, Duration.ofMillis(6000))
        wait.until(ExpectedConditions.elementToBeClickable(element))
        element.click()
    }


    private fun loadBonPreuPage() {
        val urlBonPreu = "https://www.compraonline.bonpreuesclat.cat"

        driver.get(urlBonPreu)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        // Cookies
        val cookies = driver.findElement(By.id("onetrust-accept-btn-handler"))
        safeClick(cookies)

        // Language to spanish
        driver.findElement(By.id("select-language-menu-button")).click()
        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/header/div[1]/div[4]/div/div/div/ul/li[2]/button")).click()

        setupProductsPage()
    }

    private fun setupProductsPage() {
        val urlBonPreuProducts = "https://www.compraonline.bonpreuesclat.cat/products"
        driver.get(urlBonPreuProducts)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        println("$urlBonPreuProducts loaded successfully")
    }

    private fun parseCategoriesAndProducts(driver: ChromeDriver): List<Result> {
        val parentCategories = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[1]/div[2]/div[2]/ul/li/a"))

        return parentCategories.map {
            safeClick(it)
            return@map extractCategoryAndProductsInfo(driver)
        }.flatten()
    }

    private fun extractProductsInfo(driver: ChromeDriver, category: Category): List<Product> {
        val productsElements = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[2]/div/div/div")).drop(1)
        val productes = ArrayList<Product>()
        var i = 0
        for (productElement in productsElements) {
            if (i%25 == 0) {
                Actions(driver)
                    .scrollToElement(productElement)
                    .perform()
            }
//            val elements = section.findElements(By.xpath("./div/div"))
            //*[@id="main"]/div[2]/div/div/div[2]/div/div/div[2]/div[2]/div[2]/div[1]/div[2]/div/span[2]
//*[@id="main"]/div[2]/div/div/div[2]/div/div/div[2]/div[2]/div[2]/div[1]/div[3]/strong
//            val products: List<Product> = elements.map {//*[@id="main"]/div[2]/div/div/div[2]/div/div/div[2]/div[2]/div[2]/div[1]/div[2]/div/span[1]
                val image = findElementSafely("./div[2]/div[1]/div/a/img", productElement)?.getAttribute("src")
                val name = findElementSafely("./div[2]/div[2]/div[1]/h3/a", productElement)?.text ?: ""
                val price = findElementSafely("./div[2]/div[2]/div[1]/div[3]/strong", productElement)?.text ?: ""
                val extraPrice = findElementSafely("./div[2]/div[2]/div[1]/div[2]/div/span[2]", productElement)?.text
                val size = findElementSafely("./div[2]/div[2]/div[1]/div[2]/div/span[1]", productElement)?.text/*+ " " +
                    findElementSafely("./button/div[2]/div[1]/span[2]", it)?.text*/
//            extractProductId(driver, it)
                val product = Product(name, image, price, extraPrice, size, category.id)
//            }
            productes.add(product)
            i++
        }

        return productes
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