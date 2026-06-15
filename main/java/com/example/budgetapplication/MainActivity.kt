package com.example.budgetapplication

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.budgetapplication.data.User
import com.example.budgetapplication.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().reference
        
        // KEY: UI_SYSTEM_INIT
        setupEliteBranding()
        setupSidebar()
        setupDashboard()
        loadRealtimeData()
    }

    private fun setupEliteBranding() {
        // KEY: TITLE_GRADIENT
        binding.toolbar.tvToolbarTitle.post {
            val paint = binding.toolbar.tvToolbarTitle.paint
            val width = paint.measureText("Wallet23")
            val shader = LinearGradient(0f, 0f, width, 0f,
                intArrayOf(Color.parseColor("#43E90B"), Color.parseColor("#00D4FF")),
                null, Shader.TileMode.CLAMP)
            binding.toolbar.tvToolbarTitle.paint.shader = shader
            binding.toolbar.tvToolbarTitle.invalidate()
        }
    }

    private fun setupSidebar() {
        // KEY: HALFWAY_GLIDE_MENU
        binding.toolbar.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        val params = binding.navView.layoutParams
        params.width = resources.displayMetrics.widthPixels / 2
        binding.navView.layoutParams = params

        val menu = binding.navView
        menu.findViewById<View>(R.id.menuDashboard).setOnClickListener { binding.drawerLayout.closeDrawers() }
        menu.findViewById<View>(R.id.menuTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
            binding.drawerLayout.closeDrawers()
        }
        menu.findViewById<View>(R.id.menuCategoryGoals).setOnClickListener {
            startActivity(Intent(this, CategoryBudgetActivity::class.java))
            binding.drawerLayout.closeDrawers()
        }
        menu.findViewById<View>(R.id.menuLeaderboard).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
            binding.drawerLayout.closeDrawers()
        }
        menu.findViewById<View>(R.id.menuHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            binding.drawerLayout.closeDrawers()
        }
        menu.findViewById<View>(R.id.menuLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupDashboard() {
        binding.addTransactionBtn.setOnClickListener { startActivity(Intent(this, TransactionActivity::class.java)) }
        binding.calculateBudgetBtn.setOnClickListener { startActivity(Intent(this, BudgetActivity::class.java)) }
        binding.categoryBudgetBtn.setOnClickListener { startActivity(Intent(this, CategoryBudgetActivity::class.java)) }
        binding.leaderboardBtn.setOnClickListener { startActivity(Intent(this, LeaderboardActivity::class.java)) }
    }

    private fun loadRealtimeData() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return
                syncStats(user)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun syncStats(user: User) {
        database.child("transactions").orderByChild("userId").equalTo(user.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var inc = 0.0
                    var exp = 0.0
                    for (child in snapshot.children) {
                        val amt = child.child("amount").getValue(Double::class.java) ?: 0.0
                        if (child.child("type").getValue(String::class.java) == "INCOME") inc += amt
                        else exp += amt
                    }
                    val savings = inc - exp
                    database.child("users").child(user.uid).child("totalSavings").setValue(savings)
                    updateBadge(savings, if (user.initialIncomeBaseline > 0) user.initialIncomeBaseline else inc)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateBadge(savings: Double, income: Double) {
        // KEY: GAMIFICATION_ENGINE_35_PERCENT
        if (income <= 0) return
        val ratio = (savings / income) * 100
        val level = when {
            ratio >= 125 -> 10; ratio >= 115 -> 9; ratio >= 105 -> 8; ratio >= 95 -> 7; ratio >= 85 -> 6;
            ratio >= 75 -> 5; ratio >= 65 -> 4; ratio >= 55 -> 3; ratio >= 45 -> 2; ratio >= 35 -> 1; else -> 0
        }
        val titles = arrayOf("Recruit", "Novice Saver", "Budget Beginner", "Frugal Finder", "Penny Pincher", "Savings Soldier", "Cash Commander", "Wealth Warrior", "Financial Fighter", "Money Master", "Savings Guru")
        binding.tvBadgeTitle.text = if (level > 0) titles[level] else "Secure Level 1 (35%)"
        binding.tvSavingsLevel.text = "Savings Level $level Member (" + String.format(Locale.US, "%.1f", ratio) + "%)"
        
        val color = if (level > 0) Color.parseColor("#43E90B") else Color.RED
        binding.badgeCard.strokeColor = color
        binding.ivBadgeIcon.setColorFilter(color)

        // KEY: SPIRAL_BORDER_ANIMATION
        val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotate.duration = 2000
        rotate.repeatCount = Animation.INFINITE
        binding.ivBadgeIcon.startAnimation(rotate)
    }
}
