package com.mccartykim.yumcodingchallenge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StockListing(val id: String, val name: String, val price: Double, val companyType: List<String>, var priceDiff: Double?)

sealed class Detail

// NOTE: This is based off the JSON schema I observed in curl, which is a bit different from the example in the challenge
@JsonClass(generateAdapter = true)
data class StockDetail(
    val id: String, val name: String, val price: Double, val allTimeHigh: Double,
    val companyType: List<String>, val address: String, val imageUrl: String, val website: String
): Detail()

data class NetworkError(val message: String): Detail()
