package com.smartcaptain.app

class RuleEngine {
    fun evaluate(
        offer: RideOffer, 
        rules: DriverRules, 
        masterEnabled: Boolean, 
        emergencyStopped: Boolean, 
        passActive: Boolean
    ): FilterDecision {
        val reasons = mutableListOf<String>()
        if (emergencyStopped) return FilterDecision(false, listOf("EMERGENCY STOP is active."))
        if (!masterEnabled) return FilterDecision(false, listOf("Master switch is turned OFF."))
        if (!passActive) return FilterDecision(false, listOf("Driver pass is not active."))
        if (offer.fareInr < rules.minFareInr) reasons.add("Fare ₹${offer.fareInr} below min.")
        return if (reasons.isEmpty()) FilterDecision(true, listOf("MATCH")) else FilterDecision(false, reasons)
    }
}
