package com.hooked.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): HookedDatabase {
    val driver = driverFactory.createDriver()
    val database = HookedDatabase(driver)
    return database
}
