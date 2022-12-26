class Server {
    private fun scrapper(supermarket: Supermarket, onScrappedResult: (Result) -> Unit): ScrapperInterface {
        return when(supermarket) {
            Supermarket.mercadona -> Scrapper(onScrappedResult)
            Supermarket.bonPreu -> BonPreuScrapper(onScrappedResult)
        }
    }

    init {
        start()
    }

    private fun start() {
        val supermarket: Supermarket = Supermarket.bonPreu
        val dbManager = DatabaseManager()


        val scrapper: ScrapperInterface by lazy {
            scrapper(supermarket) {
                dbManager.fillDB(listOf(it))
            }
        }

        scrapper.load()
        scrapper.scrapSafely()
        scrapper.result?.let {
            dbManager.fillDB(it)
        }
    }
}