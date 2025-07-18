package net.kenstir.ui.view

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import net.kenstir.hemlock.R
import net.kenstir.ui.util.compatEnableEdgeToEdge

class TestActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var contentLayout: View
    private lateinit var toolbar: Toolbar

    /** Set up the window insets listener to adjust padding for system bars */
    private fun adjustPaddingForEdgeToEdge() {
        val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar_layout)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply top inset to AppBarLayout so Toolbar sits below status bar
            appBarLayout.updatePadding(top = sysBars.top)

            // Apply bottom inset to content layout
            contentLayout.updatePadding(bottom = sysBars.bottom)

            // Apply insets to navigation drawer
            navView.updatePadding(top = sysBars.top, bottom = sysBars.bottom)

            WindowInsetsCompat.CONSUMED
        }

        // Optional: Set system bar colors
//        window.statusBarColor = Color.TRANSPARENT
//        window.navigationBarColor = Color.TRANSPARENT
//        WindowCompat.getInsetsController(window, drawerLayout).apply {
//            isAppearanceLightStatusBars = true  // or false if you use dark background
//            isAppearanceLightNavigationBars = true
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_test)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)
        contentLayout = findViewById(R.id.content_main)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        adjustPaddingForEdgeToEdge()

        // Add hamburger icon
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.main_search_button -> {
                    Toast.makeText(this, "SEARCH", Toast.LENGTH_SHORT).show()
                }
                R.id.main_checkouts_button -> {
                    Toast.makeText(this, "Checkouts clicked", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Other item clicked", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
