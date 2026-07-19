package com.luisete.queda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.luisete.queda.core.database.QuedaDatabase
import com.luisete.queda.core.testing.E2ECommand
import com.luisete.queda.core.testing.E2ECommandParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class E2ETestControlActivity : ComponentActivity() {
    @Inject
    lateinit var database: QuedaDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.dataString
        val command = E2ECommandParser.parse(uri)

        handleCommand(command)
    }

    private fun handleCommand(command: E2ECommand) {
        lifecycleScope.launch {
            when (command) {
                E2ECommand.Reset, E2ECommand.SeedEmpty -> {
                    Log.d("E2E", "Performing command: ${command::class.simpleName}")
                    withContext(Dispatchers.IO) {
                        clearE2EData()
                    }
                }

                is E2ECommand.Invalid -> {
                    Log.e("E2E", "Invalid E2E command received")
                    setResult(RESULT_CANCELED)
                    finish()
                    return@launch
                }
            }

            startActivity(
                Intent(this@E2ETestControlActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
        }
    }

    private fun clearE2EData() {
        database.clearAllTables()
        getSharedPreferences("queda_e2e_control", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
