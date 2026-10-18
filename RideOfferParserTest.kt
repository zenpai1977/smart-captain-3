package com.smartcaptain.app

import org.junit.Assert.*
import org.junit.Test

class RideOfferParserTest {
    @Test fun parsesLabelledOffer() {
        val parsed = RideOfferParser.parse("Uber", listOf(
            "Fare ₹85", "Pickup distance: 1.2 km away",
            "Trip distance 7.5 km", "To: Connaught Place", "Cab"
        ))
        assertNotNull(parsed.offer)
        assertTrue(parsed.missingFields.isEmpty())
        assertEquals(85.0, parsed.offer!!.fare, 0.01)
        assertEquals(1.2, parsed.offer.pickupKm, 0.01)
        assertEquals(7.5, parsed.offer.tripKm, 0.01)
        assertEquals("Connaught Place", parsed.offer.destination)
    }

    @Test fun ambiguousUnlabelledDistancesFailClosed() {
        val parsed = RideOfferParser.parse("Rapido", listOf("₹90", "1.1 km", "6.0 km"))
        assertNull(parsed.offer)
        assertTrue(parsed.missingFields.contains("pickup distance") || parsed.missingFields.contains("trip distance"))
    }

    @Test fun metresConvertToKilometres() {
        val parsed = RideOfferParser.parse("Rapido", listOf(
            "Fare ₹70", "Pickup 800 m away", "Trip distance 6 km"
        ))
        assertNotNull(parsed.offer)
        assertEquals(0.8, parsed.offer!!.pickupKm, 0.01)
    }
}
