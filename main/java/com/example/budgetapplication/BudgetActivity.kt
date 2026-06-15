package com.example.budgetapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetapplication.data.Transaction
import com.example.budgetapplication.databinding.ActivityBudgetBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private val allTransactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // KEY: DB_CONNECTION
        database = FirebaseDatabase.getInstance().getReference("transactions")

        binding.toolbar.btnMenu.setOnClickListener { finish() }
        binding.toolbar.tvToolbarTitle.text = "Visual Intelligence"

        binding.backBtn.setOnClickListener { finish() }

        loadBudgetData()
    }

    private fun loadBudgetData() {
        val userId = auth.currentUser?.uid ?: return
        
        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var income = 0.0
                    var expense = 0.0
                    allTransactions.clear()
                    
                    for (child in snapshot.children) {
                        val t = child.getValue(Transaction::class.java) ?: continue
                        allTransactions.add(t)
                        if (t.type == "INCOME") income += t.amount
                        else expense += t.amount
                    }
                    
                    updateUI(income, expense)
                    setup3DPieChart(income, expense)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetActivity, "Sync Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(income: Double, expense: Double) {
        val locale = Locale.US
        binding.totalIncomeText.text = "Income: R" + String.format(locale, "%.2f", income)
        binding.totalExpenseText.text = "Expenses: R" + String.format(locale, "%.2f", expense)
        val balance = income - expense
        binding.balanceText.text = "Net Reservoir: R" + String.format(locale, "%.2f", balance)
        
        binding.explanationText.text = if (balance >= 0) {
            "Savings performance detected. Capital revenue at R" + String.format(locale, "%.2f", balance)
        } else {
            "System Warning: Capital depletion of R" + String.format(locale, "%.2f", -balance)
        }
    }

    private fun setup3DPieChart(income: Double, expense: Double) {
        // KEY: INTERACTIVE_3D_PIE_LOGIC
        val entries = ArrayList<PieEntry>()
        if (income > 0) entries.add(PieEntry(income.toFloat(), "Inflow"))
        if (expense > 0) entries.add(PieEntry(expense.toFloat(), "Outflow"))

        val dataSet = PieDataSet(entries, "System Balance")
        dataSet.colors = listOf(Color.parseColor("#43E90B"), Color.parseColor("#FF3B30"))
        dataSet.sliceSpace = 5f
        dataSet.selectionShift = 20f // The hover effect
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 16f

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            animateY(1500)
            
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val label = (e as? PieEntry)?.label ?: return
                    showInteractivePopup(label)
                }
                override fun onNothingSelected() {
                    binding.transactionPopup.visibility = View.GONE
                }
            })
            invalidate()
        }
    }

    private fun showInteractivePopup(label: String) {
        // KEY: RHYTHMIC_POPUP_FEEDBACK
        binding.transactionPopup.visibility = View.VISIBLE
        val type = if (label == "Inflow") "INCOME" else "EXPENSE"
        val filtered = allTransactions.filter { it.type == type }.takeLast(4)
        
        var popupText = "Savings " + label + " History:\n"
        for (t in filtered) {
            popupText += "• R" + String.format(Locale.US, "%.2f", t.amount) + " | " + t.category + "\n"
        }
        if (filtered.isEmpty()) popupText += "No recent records found."
        
        binding.tvPopupContent.text = popupText
    }
}
