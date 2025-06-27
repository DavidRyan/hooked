package com.hooked.catches.data.database

import com.hooked.core.logging.Logger

class DatabaseModule(
    private val driverFactory: DatabaseDriverFactory
) {
    private var database: CatchDatabase? = null
    
    fun provideDatabase(): CatchDatabase {
        if (database == null) {
            database = CatchDatabase(driverFactory.createDriver())
            Logger.info("DatabaseModule", "Database initialized")
        }
        return database!!
    }
    
    fun provideCatchLocalDataSource(): CatchLocalDataSource {
        return CatchLocalDataSource(provideDatabase())
    }
}