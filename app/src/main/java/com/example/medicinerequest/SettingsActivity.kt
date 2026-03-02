package com.example.medicinerequest

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editFirstName = findViewById<EditText>(R.id.editFirstName)
        val editLastName = findViewById<EditText>(R.id.editLastName)
        val editCF = findViewById<EditText>(R.id.editCF)
        val editPhone = findViewById<EditText>(R.id.editPhone)
        val radioWhatsApp = findViewById<RadioButton>(R.id.radioWhatsApp)
        val radioSms = findViewById<RadioButton>(R.id.radioSms)
        val medicineContainer = findViewById<LinearLayout>(R.id.medicineListContainer)
        val editNewMedicine = findViewById<EditText>(R.id.editNewMedicine)
        val btnAddMedicine = findViewById<Button>(R.id.btnAddMedicine)
        val editImportText = findViewById<EditText>(R.id.editImportText)
        val btnImport = findViewById<Button>(R.id.btnImport)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        var firstNameStr = prefs.getString("patient_firstname", "") ?: ""
        var lastNameStr = prefs.getString("patient_lastname", "") ?: ""
        // Migration: if new fields are empty, pre-populate from old single patient_name field
        if (firstNameStr.isEmpty() && lastNameStr.isEmpty()) {
            val oldName = (prefs.getString("patient_name", "") ?: "").trim()
            if (oldName.isNotEmpty()) {
                val spaceIdx = oldName.indexOf(' ')
                if (spaceIdx > 0) {
                    firstNameStr = oldName.substring(0, spaceIdx).trim()
                    lastNameStr = oldName.substring(spaceIdx).trim()
                } else {
                    firstNameStr = oldName
                }
            }
        }
        editFirstName.setText(firstNameStr)
        editLastName.setText(lastNameStr)
        editCF.setText(prefs.getString("fiscal_code", ""))
        editPhone.setText(prefs.getString("doctor_phone", ""))

        if (prefs.getString("send_method", "whatsapp") == "sms")
            radioSms.isChecked = true
        else
            radioWhatsApp.isChecked = true

        parseMedicines().forEach { (entry, isDefault) ->
            addMedicineRow(medicineContainer, entry, isDefault)
        }

        btnAddMedicine.setOnClickListener {
            val name = editNewMedicine.text.toString().trim()
            if (name.isNotEmpty()) {
                addMedicineRow(medicineContainer, "1 x $name", false)
                editNewMedicine.text.clear()
            }
        }

        btnImport.setOnClickListener {
            val text = editImportText.text.toString()
            if (text.isBlank()) return@setOnClickListener
            val entries = parseImportText(text)
            if (entries.isEmpty()) return@setOnClickListener
            medicineContainer.removeAllViews()
            entries.forEach { (entry, isDefault) -> addMedicineRow(medicineContainer, entry, isDefault) }
            editImportText.text.clear()
            Toast.makeText(this, "Lista importata (${entries.size} farmaci)", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val sendMethod = if (radioSms.isChecked) "sms" else "whatsapp"
            saveMedicines(medicineContainer)
            prefs.edit()
                .putString("patient_firstname", editFirstName.text.toString().trim())
                .putString("patient_lastname", editLastName.text.toString().trim())
                .putString("fiscal_code", editCF.text.toString())
                .putString("doctor_phone", editPhone.text.toString())
                .putString("send_method", sendMethod)
                .apply()
            Toast.makeText(this, "Salvato", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // entry è nel formato "N x Nome farmaco" oppure solo "Nome farmaco"
    private fun addMedicineRow(container: LinearLayout, entry: String, isDefault: Boolean) {
        val (qty, name) = splitQtyName(entry)
        val qtyPx = (56 * resources.displayMetrics.density).toInt()

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(0, 4, 0, 4)

        // Riga 1: [checkbox] [nome farmaco — spazio pieno]
        val line1 = LinearLayout(this)
        line1.orientation = LinearLayout.HORIZONTAL
        line1.gravity = Gravity.CENTER_VERTICAL

        val cb = CheckBox(this)
        cb.isChecked = isDefault
        line1.addView(cb)

        val etName = EditText(this)
        etName.setText(name)
        etName.textSize = 20f
        etName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        line1.addView(etName)

        card.addView(line1)

        // Riga 2: [quantità] [" x (conf.)"] [spazio] [× Elimina]
        val line2 = LinearLayout(this)
        line2.orientation = LinearLayout.HORIZONTAL
        line2.gravity = Gravity.CENTER_VERTICAL

        val etQty = EditText(this)
        etQty.setText(qty.toString())
        etQty.textSize = 18f
        etQty.inputType = InputType.TYPE_CLASS_NUMBER
        etQty.gravity = Gravity.CENTER
        etQty.layoutParams = LinearLayout.LayoutParams(qtyPx, LinearLayout.LayoutParams.WRAP_CONTENT)
        line2.addView(etQty)

        val tvX = TextView(this)
        tvX.text = " x (confezioni)"
        tvX.textSize = 16f
        line2.addView(tvX)

        val spacer = android.view.View(this)
        spacer.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        line2.addView(spacer)

        val btnDelete = Button(this)
        btnDelete.text = "Elimina"
        btnDelete.textSize = 14f
        btnDelete.setOnClickListener { container.removeView(card) }
        line2.addView(btnDelete)

        card.addView(line2)
        container.addView(card)
    }

    // Separa "2 x Rosumibe 20/10 mg" → Pair(2, "Rosumibe 20/10 mg")
    // Se non c'è prefisso numerico → Pair(1, entry)
    private fun splitQtyName(entry: String): Pair<Int, String> {
        val m = Regex("""^(\d+)\s*x\s+(.+)""", RegexOption.IGNORE_CASE).matchEntire(entry.trim())
        return if (m != null) Pair(m.groupValues[1].toInt(), m.groupValues[2].trim())
        else Pair(1, entry.trim())
    }

    // Accetta tre formati:
    // 1. Formato per il medico:  "Farmaci: COGNOME Nome, 2 x Farmaco, 1 x Farmaco, ..."
    // 2. Formato con default: "[x] 2 x Farmaco" / "[ ] Farmaco" (uno per riga)
    // 3. Lista semplice: un nome per riga (qty=1 implicita)
    private fun parseImportText(text: String): List<Pair<String, Boolean>> {
        val trimmed = text.trim()
        if (trimmed.startsWith("Farmaci:", ignoreCase = true)) {
            return trimmed.substringAfter(":").split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
                .drop(1) // primo elemento è il nome paziente
                .filter { it.isNotEmpty() }
                .map { Pair(it, false) }
        }
        return trimmed.lines().mapNotNull { line ->
            val t = line.trim()
            when {
                t.isEmpty() -> null
                t.startsWith("[x] ", ignoreCase = true) -> Pair(t.drop(4).trim(), true)
                t.startsWith("[ ] ") -> Pair(t.drop(4).trim(), false)
                else -> Pair(t, false)
            }
        }.filter { it.first.isNotEmpty() }
    }

    private fun parseMedicines(): List<Pair<String, Boolean>> {
        val file = File(filesDir, "medicines.txt")
        if (!file.exists()) file.writeText("[x] 1 x Paracetamolo 1000 mg\n")
        return parseImportText(file.readText())
    }

    private fun saveMedicines(container: LinearLayout) {
        val lines = buildString {
            for (i in 0 until container.childCount) {
                val card  = container.getChildAt(i) as? LinearLayout ?: continue
                val line1 = card.getChildAt(0) as? LinearLayout ?: continue
                val line2 = card.getChildAt(1) as? LinearLayout ?: continue
                val cb     = line1.getChildAt(0) as? CheckBox ?: continue
                val etName = line1.getChildAt(1) as? EditText ?: continue
                val etQty  = line2.getChildAt(0) as? EditText ?: continue
                val name = etName.text.toString().trim()
                if (name.isEmpty()) continue
                val qty = etQty.text.toString().toIntOrNull()?.takeIf { it > 0 } ?: 1
                val prefix = if (cb.isChecked) "[x] " else "[ ] "
                appendLine("$prefix$qty x $name")
            }
        }
        File(filesDir, "medicines.txt").writeText(lines.trim())
    }
}
