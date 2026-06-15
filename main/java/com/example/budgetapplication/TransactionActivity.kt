package com.example.budgetapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetapplication.data.Transaction
import com.example.budgetapplication.databinding.ActivityTransactionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private lateinit var database: DatabaseReference
    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // KEY: DB_REFERENCE
        database = FirebaseDatabase.getInstance().getReference("transactions")

        setupSpinners()

        binding.btnUploadReceipt.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            resultLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener { saveTransaction() }

        loadRecentTransactions()
    }

    private fun setupSpinners() {
        val types = arrayOf("EXPENSE", "INCOME")
        binding.spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        val categories = arrayOf("Salary", "Investment", "Food", "Rent", "Transport", "Entertainment", "Healthcare", "Shopping", "Other")
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.ivReceipt.visibility = View.VISIBLE
            binding.ivReceipt.setImageURI(selectedImageUri)
        }
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val description = binding.etDescription.text.toString()
        val type = binding.spinnerType.selectedItem.toString()
        val category = binding.spinnerCategory.selectedItem.toString()
        val userId = auth.currentUser?.uid ?: return

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val transactionId = database.push().key ?: return
        val base64Image = selectedImageUri?.let { encodeImageToBase64(it) }

        val transaction = Transaction(transactionId, userId, amount, description, category, System.currentTimeMillis(), type, base64Image)

        // KEY: SAVE_DATA
        database.child(transactionId).setValue(transaction).addOnSuccessListener {
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    private fun loadRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return
        database.orderByChild("userId").equalTo(userId).limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    while (binding.tlRecent.childCount > 1) binding.tlRecent.removeViewAt(1)
                    val list = mutableListOf<Transaction>()
                    for (child in snapshot.children) {
                        child.getValue(Transaction::class.java)?.let { list.add(it) }
                    }
                    list.asReversed().forEach { addTableRow(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addTableRow(t: Transaction) {
        val row = TableRow(this)
        val tvAmt = TextView(this).apply { 
            text = "R" + String.format(Locale.US, "%.2f", t.amount)
            setPadding(16, 16, 16, 16)
            setTextColor(if (t.type == "INCOME") Color.parseColor("#43E90B") else Color.RED)
            setTypeface(null, Typeface.BOLD)
        }
        val tvType = TextView(this).apply { 
            text = t.type
            setPadding(16, 16, 16, 16)
            setTextColor(Color.BLACK)
        }
        val tvCat = TextView(this).apply { 
            text = t.category
            setPadding(16, 16, 16, 16)
            setTextColor(Color.BLACK)
        }
        row.addView(tvAmt)
        row.addView(tvType)
        row.addView(tvCat)
        binding.tlRecent.addView(row)
    }
}
