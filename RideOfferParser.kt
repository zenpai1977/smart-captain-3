package com.smartcaptain.app

/**
 * Conservative parser for Accessibility text. It fails closed unless it can find a fare,
 * a labelled pickup distance and a labelled trip distance. Patterns require field testing
 * against the actual screen and app language before automatic actions are relied upon.
 */
data class OfferParseResult(val offer: RideOffer?, val missingFields: List<String>)

object RideOfferParser {
    private val distanceRegex = Regex(
        """(?i)(\d+(?:[.,]\d+)?)\s*(km|kms|kilometers?|kilometres?|m|meters?|metres?)\b"""
    )
    private val currencyRegex = Regex(
        """(?i)(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:₹|rs\.?|inr)\b"""
    )
    private val fareLabelRegex = Regex("""(?i)\b(fare|earnings?|you earn|payout|trip amount)\b""")
    private val pickupHint = Regex(
        """(?i)\b(pick\s*up|pickup|away|from you|to rider|to customer|reach(?:ing)?|arrive|collection distance|distance to rider|distance to pickup)\b"""
    )
    private val tripHint = Regex(
        """(?i)\b(trip distance|ride distance|total distance|drop distance|journey|trip|ride of|delivery distance|distance travelled|distance to drop)\b"""
    )
    private val destinationPrefix = Regex(
        """(?i)^(?:drop(?:[\s-]?off)?(?:\s+location)?|destination|going\s+to|to)\s*[:\-]?\s*(.+)$"""
    )

    fun parse(platform: String, linesIn: List<String>): OfferParseResult {
        val lines = linesIn.map { it.replace('\n', ' ').trim() }
            .filter { it.isNotBlank() }.take(250)
        if (lines.isEmpty()) return OfferParseResult(null, listOf("screen text"))
        val allText = lines.joinToString(" | ")
        val fare = extractFare(lines)
        val (pickup, trip) = extractDistances(allText)
        val destination = extractDestination(lines)
        val vehicleType = detectVehicleType(allText)

        val missing = buildList {
            if (fare == null) add("fare")
            if (pickup == null) add("pickup distance")
            if (trip == null) add("trip distance")
        }
        if (missing.isNotEmpty()) return OfferParseResult(null, missing)

        return OfferParseResult(
            RideOffer(platform, fare!!, pickup!!, trip!!, destination, vehicleType),
            emptyList()
        )
    }

    private fun extractFare(lines: List<String>): Double? {
        val candidates = mutableListOf<Double>()
        val irrelevant = Regex("""(?i)\b(wallet|balance|total earnings|previous trip|cash collected)\b""")
        lines.forEach { line ->
            if (irrelevant.containsMatchIn(line)) return@forEach
            currencyRegex.findAll(line).forEach currencyLoop@ { match ->
                val raw = match.groups[1]?.value ?: match.groups[2]?.value ?: return@currencyLoop
                raw.replace(",", "").toDoubleOrNull()
                    ?.takeIf { it in 1.0..100000.0 }?.let(candidates::add)
            }
            if (!currencyRegex.containsMatchIn(line) && fareLabelRegex.containsMatchIn(line)) {
                Regex("""(?<!\w)(\d{1,5}(?:[.,]\d{1,2})?)(?!\w)""").findAll(line).forEach { match ->
                    match.groupValues[1].replace(',', '.').toDoubleOrNull()
                        ?.takeIf { it in 1.0..100000.0 }?.let(candidates::add)
                }
            }
        }
        // When more than one amount is visible, use the smallest as a conservative fare.
        return candidates.minOrNull()
    }

    private fun extractDistances(text: String): Pair<Double?, Double?> {
        val pickupCandidates = mutableListOf<Pair<Int, Double>>()
        val tripCandidates = mutableListOf<Pair<Int, Double>>()
        distanceRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            val rawValue = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@forEach
            val unit = match.groupValues[2].lowercase()
            val km = if (unit.startsWith("m") && !unit.startsWith("km")) rawValue / 1000.0 else rawValue
            val pickupGap = nearestHintDistance(match.range, text, pickupHint)
            val tripGap = nearestHintDistance(match.range, text, tripHint)
            val before = text.substring(maxOf(0, start - 34), start)
            val after = text.substring(end, minOf(text.length, end + 34))
            when {
                pickupGap <= 34 && pickupGap < tripGap -> pickupCandidates += pickupGap to km
                tripGap <= 34 && tripGap < pickupGap -> tripCandidates += tripGap to km
                pickupHint.containsMatchIn("$before $after") && !tripHint.containsMatchIn("$before $after") ->
                    pickupCandidates += 0 to km
                tripHint.containsMatchIn("$before $after") && !pickupHint.containsMatchIn("$before $after") ->
                    tripCandidates += 0 to km
            }
        }
        return pickupCandidates.minByOrNull { it.first }?.second to
            tripCandidates.minByOrNull { it.first }?.second
    }

    private fun nearestHintDistance(numberRange: IntRange, text: String, hintRegex: Regex): Int {
        var best = Int.MAX_VALUE
        hintRegex.findAll(text).forEach { hint ->
            val gap = when {
                hint.range.last < numberRange.first -> numberRange.first - hint.range.last
                hint.range.first > numberRange.last -> hint.range.first - numberRange.last
                else -> 0
            }
            if (gap < best) best = gap
        }
        return best
    }

    private fun extractDestination(lines: List<String>): String {
        for (line in lines) {
            val candidate = destinationPrefix.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            if (candidate.isBlank() || pickupHint.containsMatchIn(candidate)) continue
            if (distanceRegex.containsMatchIn(candidate) && candidate.length < 22) continue
            val clean = candidate
                .replace(Regex("""(?i)\b\d+(?:[.,]\d+)?\s*(?:km|kms|m|meters?|metres?)\b"""), "")
                .trim(' ', ':', '-', '•')
            if (clean.any { it.isLetter() } && clean.length >= 3) return clean.take(140)
        }
        return ""
    }

    private fun detectVehicleType(text: String): String? = when {
        Regex("""(?i)\b(parcel|package|delivery|shipment)\b""").containsMatchIn(text) -> "Parcel"
        Regex("""(?i)\b(auto|autorickshaw|auto-rickshaw|three wheeler|3-wheeler)\b""").containsMatchIn(text) -> "Auto"
        Regex("""(?i)\b(bike|motorcycle|two wheeler|two-wheeler|scooter)\b""").containsMatchIn(text) -> "Bike"
        Regex("""(?i)\b(cab|car|sedan|suv|hatchback)\b""").containsMatchIn(text) -> "Cab"
        else -> null
    }
}
