package edu.stevens.cs597.budgetgarden

import android.app.Application
import edu.stevens.cs597.budgetgarden.data.AppDatabase
import edu.stevens.cs597.budgetgarden.data.BudgetDao
import edu.stevens.cs597.budgetgarden.data.BudgetRepository
import kotlinx.coroutines.*

class GardenBudgetApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var dao: BudgetDao
        private set

    lateinit var repository: BudgetRepository
        private set

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private lateinit var setupJob: Deferred<Unit>

    override fun onCreate() {
        super.onCreate()

        // Initialize the shared database
        database = AppDatabase.get(this)

        // Initialize DAO
        dao = database.budgetDao()

        // Initialize repository
        repository = BudgetRepository(database)

        // Create the first Wallet if one doesn't exist
        setupJob = appScope.async {
            repository.ensureFirstWallet()
        }
    }

    // Other screens can wait for setup to finish
    suspend fun awaitDatabaseReady() {
        setupJob.await()
    }
}