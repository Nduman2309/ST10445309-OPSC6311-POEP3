package com.example.budgetapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetapplication.data.CategoryBudget
import com.example.budgetapplication.databinding.ActivityCategoryBudgetBinding
import com.example.budgetapplication.databinding.ItemCategoryBudgetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CategoryBudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBudgetBinding
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private var selectedTimeline: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        setupToolbar()
        setupCategorySpinner()

        binding.btnSelectTimeline.setOnClickListener { showDatePicker() }
        binding.btnSaveBudget.setOnClickListener { saveCategoryBudget() }
        
        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }
        binding.btnOk.setOnClickListener { 
            binding.cardCongratulations.visibility = View.GONE
            binding.viewBlurDim.visibility = View.GONE
            binding.ivSpiralLight.visibility = View.GONE
            binding.ivSpiralLight.clearAnimation()
        }

        loadActiveBudgets()
    }

    private fun setupToolbar() {
        binding.toolbar.btnMenu.setOnClickListener { finish() }
        binding.toolbar.tvToolbarTitle.text = "Operational Missions"
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Food", "Rent", "Transport", "Entertainment", "Healthcare", "Shopping", "Other")
        binding.spinnerBudgetCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, day)
            selectedTimeline = selectedDate.timeInMillis
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
            binding.tvTimeline.text = sdf.format(selectedDate.time)
        }
        DatePickerDialog(this, dateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveCategoryBudget() {
        val amount = binding.etBudgetAmount.text.toString().toDoubleOrNull()
        val category = binding.spinnerBudgetCategory.selectedItem.toString()
        val objective = binding.etObjective.text.toString()
        val userId = auth.currentUser?.uid ?: return

        if (amount == null || amount <= 0 || selectedTimeline == 0L) {
            Toast.makeText(this, "Mission data incomplete", Toast.LENGTH_SHORT).show()
            return
        }

        val budgetId = database.child("category_budgets").push().key ?: return
        val budget = CategoryBudget(budgetId, userId, category, amount, 0.0, objective, selectedTimeline, false)

        database.child("category_budgets").child(budgetId).setValue(budget).addOnSuccessListener {
            Toast.makeText(this, "Mission Deployed!", Toast.LENGTH_SHORT).show()
            binding.etBudgetAmount.text?.clear()
            binding.etObjective.text?.clear()
        }
    }

    private fun loadActiveBudgets() {
        val userId = auth.currentUser?.uid ?: return
        database.child("category_budgets").orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.llActiveBudgets.removeAllViews()
                    for (child in snapshot.children) {
                        val b = child.getValue(CategoryBudget::class.java) ?: continue
                        if (!b.isCompleted) addBudgetView(b)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addBudgetView(b: CategoryBudget) {
        val itemBinding = ItemCategoryBudgetBinding.inflate(layoutInflater)
        itemBinding.tvCategoryName.text = b.category
        itemBinding.tvObjective.text = b.objective
        
        val locale = Locale.US
        itemBinding.tvLimitAmount.text = "Goal: R" + String.format(locale, "%.2f", b.limitAmount)
        itemBinding.tvCurrentSaved.text = "Secured: R" + String.format(locale, "%.2f", b.currentSaved)

        val progress = if (b.limitAmount > 0) (b.currentSaved / b.limitAmount).toFloat() else 0f
        itemBinding.progressBar.post {
            val width = (itemBinding.progressBar.parent as View).width
            val lp = itemBinding.progressBar.layoutParams
            lp.width = (width * progress.coerceAtMost(1f)).toInt()
            itemBinding.progressBar.layoutParams = lp
        }

        itemBinding.btnUpdateSavings.setOnClickListener { showUpdateDialog(b, itemBinding.root) }
        binding.llActiveBudgets.addView(itemBinding.root)
    }

    private fun showUpdateDialog(b: CategoryBudget, cardView: View) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount to secure (R)"
        input.setPadding(40, 40, 40, 40)
        
        AlertDialog.Builder(this)
            .setTitle("Capital Acquisition")
            .setView(input)
            .setPositiveButton("Process") { _, _ ->
                val added = input.text.toString().toDoubleOrNull() ?: 0.0
                updateBudgetSavings(b, added, cardView)
            }
            .setNegativeButton("Abort", null)
            .show()
    }

    private fun updateBudgetSavings(b: CategoryBudget, added: Double, cardView: View) {
        val newSaved = b.currentSaved + added
        val isNowCompleted = newSaved >= b.limitAmount
        val updates = mapOf("currentSaved" to newSaved, "isCompleted" to isNowCompleted)
        database.child("category_budgets").child(b.id).updateChildren(updates).addOnSuccessListener {
            if (isNowCompleted) triggerCompletionAnimation()
        }
    }

    private fun triggerCompletionAnimation() {
        // 1. Show blur and spiral icon initially
        binding.viewBlurDim.visibility = View.VISIBLE
        binding.viewBlurDim.alpha = 1f
        
        binding.ivSpiralLight.visibility = View.VISIBLE
        binding.ivSpiralLight.scaleX = 0f
        binding.ivSpiralLight.scaleY = 0f
        binding.ivSpiralLight.alpha = 0f


        binding.ivSpiralLight.animate()
            .scaleX(15f)
            .scaleY(15f)
            .alpha(1f)
            .rotation(1440f)
            .setDuration(1200)
            .withEndAction {

                binding.ivSpiralLight.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction {
                        binding.ivSpiralLight.visibility = View.GONE
                        

                        binding.viewBlurDim.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                binding.viewBlurDim.visibility = View.GONE
                            }
                        
                        // 5. Show the Congratulations Card clearly
                        binding.cardCongratulations.visibility = View.VISIBLE
                        binding.cardCongratulations.alpha = 0f
                        binding.cardCongratulations.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .start()
                    }
                    .start()
            }
            .start()
    }
}
