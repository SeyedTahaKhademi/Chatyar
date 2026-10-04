package ir.hooshamoozan.chatyar

import android.content.Context
import androidx.room.Room
import ir.hooshamoozan.chatyar.data.AppDatabase
import ir.hooshamoozan.chatyar.data.ChatRepository
import ir.hooshamoozan.chatyar.data.ProviderRepository
import ir.hooshamoozan.chatyar.data.SecretStore
import ir.hooshamoozan.chatyar.data.SettingsRepository
import ir.hooshamoozan.chatyar.network.AiGatewayFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "chatyar.db").build()
    val secretStore = SecretStore(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val providerRepository = ProviderRepository(database.providerDao(), secretStore)
    val chatRepository = ChatRepository(database.chatDao(), database.messageDao())
    val gatewayFactory = AiGatewayFactory()
}
