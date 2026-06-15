package com.example.budgetapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetapplication.data.CategoryBudget
import com.example.budgetapplication.data.Transaction
import com.example.budgetapplication.databinding.ActivityHistoryBinding
import com.example.budgetapplication.databinding.ItemCategoryBudgetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: DatabaseReference
    private val unifiedHistoryList = mutableListOf<HistoryItem>()
    private lateinit var adapter: HistoryAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.btnMenu.setOnClickListener { finish() }
        binding.toolbar.tvToolbarTitle.text = "Legacy Vault"

        database = FirebaseDatabase.getInstance().reference
        setupRecyclerView()
        loadUnifiedHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(unifiedHistoryList)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun loadUnifiedHistory() {
        val userId = auth.currentUser?.uid ?: return
        
        // Use a single listener to fetch both and merge
        val transactionsRef = database.child("transactions").orderByChild("userId").equalTo(userId)
        val budgetsRef = database.child("category_budgets").orderByChild("userId").equalTo(userId)

        transactionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(transSnapshot: DataSnapshot) {
                budgetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(budgetSnapshot: DataSnapshot) {
                        unifiedHistoryList.clear()
                        
                        // Parse Transactions
                        for (child in transSnapshot.children) {
                            val t = child.getValue(Transaction::class.java) ?: continue
                            unifiedHistoryList.add(HistoryItem.TransactionItem(t))
                        }
                        
                        // Parse Budgets
                        for (child in budgetSnapshot.children) {
                            val b = child.getValue(CategoryBudget::class.java) ?: continue
                            unifiedHistoryList.add(HistoryItem.BudgetItem(b))
                        }

                        // Sort by date descending
                        unifiedHistoryList.sortByDescending { it.timestamp }
                        adapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    sealed class HistoryItem {
        abstract val timestamp: Long
        data class TransactionItem(val transaction: Transaction) : HistoryItem() {
            override val timestamp: Long = transaction.date
        }
        data class BudgetItem(val budget: CategoryBudget) : HistoryItem() {
            override val timestamp: Long = budget.timelineDate
        }
    }

    class HistoryAdapter(private val list: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemCategoryBudgetBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCategoryBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
            
            when (item) {
                is HistoryItem.TransactionItem -> {
                    val t = item.transaction
                    holder.binding.tvCategoryName.text = "SYSTEM ENTRY: ${t.category}"
                    holder.binding.tvObjective.text = "Description: ${t.description}\nDate: ${sdf.format(Date(t.date))}"
                    holder.binding.tvLimitAmount.text = "Type: ${t.type}"
                    holder.binding.tvCurrentSaved.text = "Amount: R${String.format(Locale.US, "%.2f", t.amount)}"
                    holder.binding.tvCurrentSaved.setTextColor(if (t.type == "INCOME") Color.parseColor("#43E90B") else Color.RED)
                    
                    holder.binding.btnUpdateSavings.visibility = View.GONE
                    holder.binding.progressBar.visibility = View.GONE
                    holder.binding.budgetCard.strokeColor = Color.BLACK
                    holder.binding.budgetCard.strokeWidth = 2
                }
                is HistoryItem.BudgetItem -> {
                    val b = item.budget
                    holder.binding.tvCategoryName.text = "MISSION OBJECTIVE: ${b.category}"
                    holder.binding.tvObjective.text = "Goal: ${b.objective}\nDeadline: ${sdf.format(Date(b.timelineDate))}"
                    holder.binding.tvLimitAmount.text = "Target: R${String.format(Locale.US, "%.2f", b.limitAmount)}"
                    holder.binding.tvCurrentSaved.text = "Status: ${if (b.isCompleted) "MISSION SUCCESS" else "OPERATIONAL"}"
                    holder.binding.tvCurrentSaved.setTextColor(if (b.isCompleted) Color.parseColor("#43E90B") else Color.BLACK)
                    
                    holder.binding.btnUpdateSavings.visibility = View.GONE
                    holder.binding.progressBar.visibility = View.VISIBLE
                    
                    val progress = if (b.limitAmount > 0) (b.currentSaved / b.limitAmount).toFloat() else 0f
                    holder.binding.progressBar.post {
                        val lp = holder.binding.progressBar.layoutParams
                        lp.width = ((holder.binding.progressBar.parent as View).width * progress.coerceAtMost(1f)).toInt()
                        holder.binding.progressBar.layoutParams = lp
                    }
                    
                    holder.binding.budgetCard.strokeColor = if (b.isCompleted) Color.parseColor("#43E90B") else Color.LTGRAY
                    holder.binding.budgetCard.strokeWidth = if (b.isCompleted) 4 else 2
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
