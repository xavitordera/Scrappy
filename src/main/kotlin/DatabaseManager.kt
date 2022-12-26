import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseManager {
    private var dbConnection: Connection? = null

    fun fillDB(result: List<Result>) {
        connectToDB()
        result.let { results ->
            val categories = results.map { it.category }
            val products = results.map { it.products }.flatten()
            insertCategories(categories)
            insertProducts(products)
        }
        dbConnection?.close()
    }
    private fun connectToDB() {
        val jdbcUrl = "jdbc:mysql://localhost:3306/scrappy"
        // get the connection
        dbConnection = DriverManager
            .getConnection(jdbcUrl, "root", "")
    }

    private fun insertCategories(categories: List<Category>) {
        val values: String = categories.joinToString(",") {
            "(" + "${it.id},\n" +
                    "'${it.name}',\n" +
                    "${it.supermarket}\n" +
                    ")"
        } + ";"

        val categoriesStatement = "INSERT INTO `scrappy`.`CATEGORIES`\n" +
                "(`id`,\n" +
                "`name`,\n" +
                "`supermarket`)\n" +
                "VALUES\n" +
                values
        println("Inserting categories ...")
        try {
            val statement = dbConnection?.prepareStatement(categoriesStatement)
            statement?.executeUpdate()
            println("Categories inserted successfully ...")
        } catch (ex: SQLException) {
            System.err.println(ex.message)
        }
    }

    private fun insertProducts(products: List<Product>) {
        val values: String = products.joinToString(",") {
            "(" +
                    "'${it.name}'," +
                    "'${it.image}'," +
                    "'${it.price}'," +
                    "'${it.extraPrice}'," +
                    "${it.supermarket}," +
                    "${it.categoryId}," +
                    "'${it.size}'" +
                    ")"
        } + ";"

        val productsStatement = (
                "INSERT INTO `scrappy`.`PRODUCTS`" +
                        "(`name`," +
                        "`image`," +
                        "`price`," +
                        "`extra_price`," +
                        "`supermarket`," +
                        "`category`," +
                        "`size`)" +
                        "VALUES" +
                        values
                )
        println("Inserting products ...")
        try {
            val statement = dbConnection?.prepareStatement(productsStatement)
            statement?.executeUpdate()
            println("products inserted successfully ...")
        } catch (ex: SQLException) {
            System.err.println(ex.message)
        }
    }


}