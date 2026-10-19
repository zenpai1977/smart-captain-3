package com.smartcaptain.app

enum class ServiceType { BIKE, AUTO, CAB, PARCEL }

data class DriverRules(
    val serviceSwitchEnabled: Boolean = true,
    val minTripDistanceKm: Double = 5.0,
    val maxPickupDistanceKm: Double = 2.0,
    val minFareInr: Double = 60.0,
    val preferredDestination: String = "",
    val allowedServices: Set<ServiceType> = setOf(ServiceType.BIKE, ServiceType.AUTO, ServiceType.CAB, ServiceType.PARCEL)
)

data class RideOffer(
    val platform: String,
    val serviceType: ServiceType,
    val fareInr: Double,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val dropArea: String
)

data class FilterDecision(
    val matchesRules: Boolean, 
    val reasons: List<String>
)

data class DriverStatusResponse(
    val phone: String, 
    val passActive: Boolean, 
    val statusText: String, 
    val rules: DriverRules
)
