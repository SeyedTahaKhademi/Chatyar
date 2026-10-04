package ir.hooshamoozan.chatyar.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProviderEntity::class, ChatEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
