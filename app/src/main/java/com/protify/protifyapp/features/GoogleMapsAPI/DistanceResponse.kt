data class DistanceResponse(
    val destination_addresses: List<String>,
    val origin_addresses: List<String>,
    val rows: List<Row>,
) {
    data class Row(
        val elements: List<Element>
    ) {
        data class Element(
            val distance: Distance,
            val duration: Duration,
            val durationInTraffic: Duration,
            val status: String,
        ) {
            data class Distance(
                val text: String,
                val value: Int
            )

            data class Duration(
                val text: String,
                val value: Int
            )
        }
    }
}
