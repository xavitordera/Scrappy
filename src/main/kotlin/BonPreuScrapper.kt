import org.openqa.selenium.By
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
    override var result: List<Result>? = arrayListOf()

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

        Thread.sleep(300)

        val subCategoriesWeb = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[1]/div[2]/div[2]/ul/li/a"))
        val categoriesSize = subCategoriesWeb.size

        val results: ArrayList<Result> = arrayListOf()

        for(i in 1 until categoriesSize+1) {
            Thread.sleep(450)
            val categoryWebElement =
                findElementSafelyDriver("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[1]/div[2]/div[2]/ul/li[$i]/a")

            categoryWebElement?.let {
                val name = categoryWebElement.text
                safeClick(categoryWebElement)

                val url = URL(driver.currentUrl)
                val idString = url.toString().split("=").last()
                val category = Category(id = idString, name = name, 2)
                println("Scrapping category named ${category.name} ...")
                val products = extractProductsInfo(driver, category)
                println("${category.name} successfully scrapped")

                val categoryProduct = Result(category, products)
                onScrappedResult(categoryProduct)
                results.add(categoryProduct)

                driver.navigate().back()
            }
        }
        return results
    }

    private fun safeClick(element: WebElement) {
        try {
            val wait = WebDriverWait(driver, Duration.ofMillis(6000))
            wait.until(ExpectedConditions.elementToBeClickable(element))
            element.click()
        } catch (error: Exception) {
            System.err.println("Could not click ...retrying")
//            if (error is StaleElementReferenceException) {
//                safeClick(element)
//            }
        }
    }


    private fun loadBonPreuPage() {
        val urlBonPreu = "https://www.compraonline.bonpreuesclat.cat"

        driver.get(urlBonPreu)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        driver.manage().window().maximize()
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
            extractCategoryAndProductsInfo(driver)
        }.flatten()
    }

    private fun safeScroll(productElement: WebElement) {
        try {
            Actions(driver)
                .scrollToElement(productElement)
                .perform()
        } catch (e: Exception) {
            System.err.println("Could not scroll to element with name $productElement :/")
        }
    }

    private fun extractProductsInfo(driver: ChromeDriver, category: Category): List<Product> {
        Thread.sleep(200)
        val productsElements = driver.findElements(By.xpath("/html/body/div[1]/div/div[1]/div[2]/main/div[2]/div/div/div[2]/div/div/div")).drop(1)
        val productes = ArrayList<Product>()
        for ((i, productElement) in productsElements.withIndex()) {
            if (i%6 == 0 && i != 0) {
                safeScroll(productElement)
                Thread.sleep(100)
            }
            print("Inspecting element number $i .... \n")
            val name = findElementSafely("./div[2]/div[2]/div[1]/h3/a", productElement)?.text ?: break

            val image = findElementSafely("./div[2]/div[1]/div/a/img", productElement)?.getAttribute("src")

            val price = findElementSafely("./div[2]/div[2]/div[1]/div[3]/strong", productElement)?.text ?: ""
//            val extraPrice = findElementSafely("./div[2]/div[2]/div[1]/div[2]/div/span[2]", productElement)?.text
            val size = findElementSafely("./div[2]/div[2]/div[1]/div[2]/div", productElement)?.text
            val product = Product(name, image, price, "", size, category.id, 2)
            productes.add(product)
        }

        return productes
    }

    private fun findElementSafely(xpath: String, element: WebElement): WebElement? {
        return try {
            element.findElement(By.xpath(xpath))
        } catch (e: Exception) {
            System.err.println("could not locate element with xpath: $xpath")
            null
        }
    }
    private fun scrap() {
        result = parseCategoriesAndProducts(driver)
    }
    private fun findElementSafelyDriver(xpath: String): WebElement? {
        return try {
            driver.findElement(By.xpath(xpath))
        } catch (e: Exception) {
            System.err.println("could not locate element with xpath: $xpath")
            null
        }
    }
}