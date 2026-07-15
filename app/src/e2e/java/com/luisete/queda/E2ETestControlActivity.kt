package com.luisete.queda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.luisete.queda.core.testing.E2ECommand
import com.luisete.queda.core.testing.E2ECommandParser

class E2ETestControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.dataString
        val command = E2ECommandParser.parse(uri)

        handleCommand(command)
    }

    private fun handleCommand(command: E2ECommand) {
        when (command) {
            E2ECommand.Reset, E2ECommand.SeedEmpty -> {
                Log.d("E2E", "Performing command: ${command::class.simpleName}")
                clearE2EData()
                setResult(RESULT_OK)
            }

            is E2ECommand.Invalid -> {
                Log.e("E2E", "Invalid E2E command received")
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }

    private fun clearE2EData() {
        getSharedPreferences("queda_e2e_control", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
