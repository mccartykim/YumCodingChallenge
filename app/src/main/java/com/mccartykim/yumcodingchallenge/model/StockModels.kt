package com.mccartykim.yumcodingchallenge.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StockListing(val id: String, val name: String, val price: Double, val companyType: List<String>, var priceDiff: Double?)

@JsonClass(generateAdapter = true)
data class StockDetail(
    val id: String, val name: String, val price: Double, val daily_high: Double, val daily_low: Double,
    val company_type: List<String>, val address: String, val image_url: String, val website: String
)
