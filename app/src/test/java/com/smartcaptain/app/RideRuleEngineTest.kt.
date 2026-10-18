package com.smartcaptain.app

import org.junit.Assert.*
import org.junit.Test

class RideRuleEngineTest {
    private val rules = RideRules()
    private val matchingOffer = RideOffer("Uber", 85.0, 1.2, 7.5, "Connaught Place", "Cab")

    @Test fun qualifyingOfferMatches() {
        assertTrue(RideRuleEngine.evaluate(rules, matchingOffer).matches)
    }
    @Test fun lowFareDoesNotMatch() {
        val result = RideRuleEngine.evaluate(rules, matchingOffer.copy(fare = 40.0))
        assertFalse(result.matches)
        assertTrue(result.reasons.any { it.contains("Fare") })
    }
    @Test fun distantPickupDoesNotMatch() {
        assertFalse(RideRuleEngine.evaluate(rules, matchingOffer.copy(pickupKm = 3.0)).matches)
    }
    @Test fun destinationFailsClosedWhenUnknown() {
        val restricted = rules.copy(destinationKeyword = "CP")
        assertFalse(RideRuleEngine.evaluate(restricted, matchingOffer.copy(destination = "")).matches)
    }
    @Test fun restrictedVehicleRequiresKnownAllowedType() {
        val restricted = rules.copy(enabledVehicleTypes = setOf("Auto"))
        assertFalse(RideRuleEngine.evaluate(restricted, matchingOffer.copy(vehicleType = null)).matches)
        assertFalse(RideRuleEngine.evaluate(restricted, matchingOffer.copy(vehicleType = "Cab")).matches)
        assertTrue(RideRuleEngine.evaluate(restricted, matchingOffer.copy(vehicleType = "Auto")).matches)
    }
}
