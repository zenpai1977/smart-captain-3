package com.smartcaptain.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendClient(private val baseUrl: String = "https://smart-captain.onrender.com") {
    suspend fun fetchDriverStatus(phone: String): Result<DriverStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            DriverStatusResponse(phone, true, "Active", DriverRules())
        }
    }

    suspend fun saveRulesAndActivate(phone: String, plan: String, rules: DriverRules): Result<String> = withContext(Dispatchers.IO) {
        runCatching { "Saved" }
    }
}
