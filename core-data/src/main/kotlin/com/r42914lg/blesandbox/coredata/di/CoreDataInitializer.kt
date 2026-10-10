package com.r42914lg.blesandbox.coredata.di

import androidx.room.Room
import com.r42914lg.blesandbox.coredata.AirNodeRepository
import com.r42914lg.blesandbox.coredata.AirNodeRepositoryImpl
import com.r42914lg.blesandbox.coredata.db.AirNodeDatabase
import org.koin.dsl.module

class CoreDataInitializer {
    val module = module {
        single {
            Room.databaseBuilder(
                get(),
                AirNodeDatabase::class.java,
                "airnode_database.db"
            ).build()
        }

        single { get<AirNodeDatabase>().temperatureDao() }
        single { get<AirNodeDatabase>().ledStateDao() }

        single<AirNodeRepository> {
            AirNodeRepositoryImpl(
                temperatureDao = get(),
                ledStateDao = get()
            )
        }
    }
}
