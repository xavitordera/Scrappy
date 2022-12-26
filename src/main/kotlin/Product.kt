data class Product(
    val name: String,
    val image: String?,
    val price: String,
    val extraPrice: String?,
    val size: String?,
    val categoryId: String,
    val supermarket: Int = 1
)