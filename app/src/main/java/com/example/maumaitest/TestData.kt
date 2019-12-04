package com.example.maumaitest

data class TestData(
    val result: String,
    val time: String
)

val dataList = listOf(
    TestData("IN", "11:11:31"),
    TestData("IN", "11:13:47"),
    TestData("OUT", "11:14:02"),
    TestData("IN", "11:14:51")
)