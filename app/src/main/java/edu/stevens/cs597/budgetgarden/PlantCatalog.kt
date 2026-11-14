package edu.stevens.cs597.budgetgarden

data class PlantAppearance(
    val key: String,
    val drawableRes: Int,
    val labelRes: Int
)

object PlantCatalog {
    val choices = listOf(
        PlantAppearance(
            "yellow_flower", R.drawable.yellow_flower,
            R.string.yellow_flower_description
        ),
        PlantAppearance(
            "pink_flower", R.drawable.pink_flower,
            R.string.pink_flower_description
        ),
        PlantAppearance(
            "orange_flower", R.drawable.orange_flower,
            R.string.orange_flower_description
        ),
        PlantAppearance(
            "red_flower", R.drawable.red_flower,
            R.string.red_flower_description
        )
    )

    // Older goals may use plant_default or another unrecognized icon key.
    fun fromKey(key: String): PlantAppearance =
        choices.firstOrNull { it.key == key } ?: choices.first()
}
