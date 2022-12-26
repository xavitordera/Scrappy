data class Category(
    val id: String,
    val name: String,
    val supermarket: Int = 1
)

enum class Supermarket {
    mercadona, bonPreu
}