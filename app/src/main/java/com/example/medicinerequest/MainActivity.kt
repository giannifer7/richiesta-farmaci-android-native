package com.example.medicinerequest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_TEMPLATE = "Farmaci: {COGNOME} {nome}, {lista(\", \", {qta} x {farmaco})}"
    }

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
        updateLastRequestLabel()
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
        val email = prefs.getString("doctor_email", "") ?: ""

        if (firstName.isEmpty() && lastName.isEmpty()) {
            Toast.makeText(this, "Imposta nome e cognome nelle Impostazioni", Toast.LENGTH_LONG).show()
            return
        }
        if (via == "email" && email.isEmpty()) {
            Toast.makeText(this, "Imposta l'email del medico nelle Impostazioni", Toast.LENGTH_LONG).show()
            return
        }
        if (via != "email" && phone.isEmpty()) {
            Toast.makeText(this, "Imposta il numero del medico nelle Impostazioni", Toast.LENGTH_LONG).show()
            return
        }

        val fiscalCode = prefs.getString("fiscal_code", "") ?: ""
        val template = prefs.getString("message_template", null) ?: DEFAULT_TEMPLATE
        val message = applyTemplate(template, firstName, lastName, fiscalCode, selected)
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
            "email" -> {
                val uri = Uri.parse("mailto:$email?subject=${Uri.encode("Richiesta farmaci")}&body=${Uri.encode(message)}")
                startActivity(Intent(Intent.ACTION_SENDTO, uri))
            }
        }
        getSharedPreferences("app", MODE_PRIVATE).edit()
            .putLong("last_request_date", System.currentTimeMillis())
            .apply()
        updateLastRequestLabel()
    }

    private fun updateLastRequestLabel() {
        val tv = findViewById<TextView>(R.id.tvLastRequest)
        val lastMs = getSharedPreferences("app", MODE_PRIVATE).getLong("last_request_date", 0L)
        if (lastMs == 0L) {
            tv.text = ""
            return
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sentDay = Calendar.getInstance().apply {
            timeInMillis = lastMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val days = abs((today - sentDay) / (1000 * 60 * 60 * 24)).toInt()
        val dateLabel = SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN).format(Date(lastMs))
        val daysLabel = when (days) {
            0 -> "oggi"
            1 -> "1 giorno fa"
            else -> "$days giorni fa"
        }
        tv.text = "Ultima richiesta $daysLabel ($dateLabel)"
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

    private fun applyTemplate(
        template: String,
        firstName: String,
        lastName: String,
        fiscalCode: String,
        selectedMeds: List<String>
    ): String {
        val listaRegex = Regex("""\{lista\("([^"]*)",\s*([^)]*)\)\}""")
        var result = listaRegex.replace(template) { match ->
            val sep = match.groupValues[1]
                .replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")
            val itemTpl = match.groupValues[2]
            selectedMeds.joinToString(sep) { med ->
                val spaceX = Regex("""^(\d+)\s*x\s+(.+)""", RegexOption.IGNORE_CASE).matchEntire(med.trim())
                val qty = spaceX?.groupValues?.get(1) ?: "1"
                val name = spaceX?.groupValues?.get(2)?.trim() ?: med.trim()
                itemTpl.replace("{qta}", qty).replace("{farmaco}", name)
            }
        }
        result = result
            .replace("{COGNOME}", lastName.uppercase())
            .replace("{cognome}", lastName)
            .replace("{nome}", firstName)
            .replace("{cod_fisc}", fiscalCode)
        return result
    }
}
