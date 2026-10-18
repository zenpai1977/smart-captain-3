package com.smartcaptain.app

data class RideRules(
    val minFare: Double = 60.0,
    val maxPickupKm: Double = 2.0,
    val minTripKm: Double = 5.0,
    val enabledPlatforms: Set<String> = setOf("Rapido", "Uber", "Porter"),
    val enabledVehicleTypes: Set<String> = setOf("Bike", "Auto", "Cab", "Parcel"),
    val destinationKeyword: String = "",
    val filtersEnabled: Boolean = true
)

data class RideOffer(
    val platform: String,
    val fare: Double,
    val pickupKm: Double,
    val tripKm: Double,
    val destination: String = "",
    val vehicleType: String? = null
)

data class RuleDecision(val matches: Boolean, val reasons: List<String>)

object RideRuleEngine {
    fun evaluate(rules: RideRules, offer: RideOffer): RuleDecision {
        val reasons = mutableListOf<String>()
        if (!rules.filtersEnabled) reasons += "Filters are switched off"
        if (offer.platform !in rules.enabledPlatforms) reasons += "Platform disabled"
        if (offer.fare < rules.minFare) reasons += "Fare below minimum"
        if (offer.pickupKm > rules.maxPickupKm) reasons += "Pickup too far"
        if (offer.tripKm < rules.minTripKm) reasons += "Trip too short"

        if (rules.destinationKeyword.isNotBlank() &&
            (offer.destination.isBlank() ||
             !offer.destination.contains(rules.destinationKeyword.trim(), ignoreCase = true))
        ) reasons += "Destination missing or doesn't match"

        if (rules.enabledVehicleTypes.size < 4) {
            val type = offer.vehicleType
            if (type == null) reasons += "Vehicle type could not be identified safely"
            else if (type !in rules.enabledVehicleTypes) reasons += "Vehicle type disabled"
        }
        return RuleDecision(reasons.isEmpty(), reasons)
    }
}
