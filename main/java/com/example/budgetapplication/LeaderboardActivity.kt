package com.example.budgetapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetapplication.data.User
import com.example.budgetapplication.databinding.ActivityLeaderboardBinding
import com.example.budgetapplication.databinding.ItemLeaderboardBinding
import com.google.firebase.database.*
import java.util.*

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var database: DatabaseReference
    private val userList = mutableListOf<User>()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // KEY: DATABASE_REFERENCE
        database = FirebaseDatabase.getInstance().getReference("users")

        setupRecyclerView()
        loadLeaderboardData()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter(userList)
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter
    }

    private fun loadLeaderboardData() {
        // KEY: REALTIME_LEADERBOARD_QUERY
        database.orderByChild("totalSavings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java) ?: continue
                    userList.add(user)
                }
                // Sort descending by savings
                userList.sortByDescending { it.totalSavings }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LeaderboardActivity, "Sync Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // KEY: LEADERBOARD_ADAPTER_LOGIC
    class LeaderboardAdapter(private val users: List<User>) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            val rank = position + 1
            holder.binding.tvRank.text = rank.toString()
            holder.binding.tvUserEmail.text = user.email
            
            val savingsFormatted = String.format(Locale.US, "%.2f", user.totalSavings)
            holder.binding.tvUserSavings.text = "R" + savingsFormatted
            
            val primaryLime = Color.parseColor("#43E90B")
            
            if (position == 0) {
                // Top saver highlight
                holder.binding.root.setCardBackgroundColor(primaryLime)
                holder.binding.tvRank.setTextColor(Color.BLACK)
                holder.binding.tvUserEmail.setTextColor(Color.BLACK)
                holder.binding.tvUserSavings.setTextColor(Color.BLACK)
            } else {
                holder.binding.root.setCardBackgroundColor(Color.WHITE)
                holder.binding.tvRank.setTextColor(Color.BLACK)
                holder.binding.tvUserEmail.setTextColor(Color.DKGRAY)
                holder.binding.tvUserSavings.setTextColor(primaryLime)
            }
        }

        override fun getItemCount(): Int = users.size
    }
}
