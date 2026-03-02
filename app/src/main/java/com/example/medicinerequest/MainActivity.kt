package com.example.medicinerequest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private val selected = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val via = getSharedPreferences("app", MODE_PRIVATE)
                .getString("send_method", "whatsapp") ?: "whatsapp"
            sendMessage(via)
        }
    }

    override fun onResume() {
        super.onResume()
        rebuildMedicineList()
    }

    private fun rebuildMedicineList() {
        selected.clear()
        val container = findViewById<LinearLayout>(R.id.medicineContainer)
        container.removeAllViews()

        readMedicines().forEach { (name, isDefault) ->
            val checkBox = CheckBox(this)
            checkBox.text = name
            checkBox.textSize = 20f
            checkBox.setPadding(8, 20, 8, 20)
            checkBox.isChecked = isDefault
            if (isDefault) selected.add(name)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(name) else selected.remove(name)
            }
            container.addView(checkBox)
        }
    }

    private fun sendMessage(via: String) {
        if (selected.isEmpty()) {
            Toast.makeText(this, "Seleziona almeno un farmaco", Toast.LENGTH_LONG).show()
            return
        }

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        var firstName = prefs.getString("patient_firstname", "") ?: ""
        var lastName = prefs.getString("patient_lastname", "") ?: ""
        // Migration: if new fields are empty, fall back to old single patient_name field
        if (firstName.isEmpty() && lastName.isEmpty()) {
            val oldName = (prefs.getString("patient_name", "") ?: "").trim()
            if (oldName.isNotEmpty()) {
                val spaceIdx = oldName.indexOf(' ')
                if (spaceIdx > 0) {
                    firstName = oldName.substring(0, spaceIdx).trim()
                    lastName = oldName.substring(spaceIdx).trim()
                } else {
                    firstName = oldName
                }
            }
        }
        val phone = prefs.getString("doctor_phone", "") ?: ""

        if (firstName.isEmpty() && lastName.isEmpty()) {
            Toast.makeText(this, "Imposta nome e cognome nelle Impostazioni", Toast.LENGTH_LONG).show()
            return
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Imposta il numero del medico nelle Impostazioni", Toast.LENGTH_LONG).show()
            return
        }

        val message = buildMessage(firstName, lastName, selected)
        when (via) {
            "whatsapp" -> {
                // wa.me requires digits only (no +, spaces, dashes)
                val waPhone = phone.replace(Regex("[^0-9]"), "")
                val uri = Uri.parse("https://wa.me/$waPhone?text=" + URLEncoder.encode(message, "UTF-8"))
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            "sms" -> {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phone")
                    putExtra("sms_body", message)
                }
                startActivity(intent)
            }
        }
    }

    private fun readMedicines(): List<Pair<String, Boolean>> {
        val file = File(filesDir, "medicines.txt")
        if (!file.exists()) file.writeText("[x] 1 x Paracetamolo 1000 mg\n")
        return file.readLines().filter { it.isNotBlank() }.map { line ->
            when {
                line.startsWith("[x] ") -> Pair(line.removePrefix("[x] "), true)
                line.startsWith("[ ] ") -> Pair(line.removePrefix("[ ] "), false)
                else -> Pair(line, false)
            }
        }
    }

    private fun buildMessage(firstName: String, lastName: String, selectedMeds: List<String>): String {
        val nameFormatted = "${lastName.uppercase()} $firstName".trim()
        val medsFormatted = selectedMeds.joinToString(", ")
        return "Farmaci: $nameFormatted, $medsFormatted"
    }
}
