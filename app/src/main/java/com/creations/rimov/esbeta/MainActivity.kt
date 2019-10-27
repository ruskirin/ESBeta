package com.creations.rimov.esbeta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.creations.rimov.esbeta.extensions.infoLog
import com.creations.rimov.esbeta.view_models.GlobalViewModel
import com.creations.rimov.esbeta.views.MainToolbar
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    private val navHostFrag: NavHostFragment by lazy { mainNavHostFrag as NavHostFragment }
    private val navController: NavController by lazy { navHostFrag.navController }

    private val toolbar: MainToolbar by lazy { mainToolbar }

    private val globalVm by lazy {
        ViewModelProviders.of(this).get(GlobalViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        setActionBar(toolbar)

        globalVm.getPageNum().observe(this, Observer { num ->
            when {
                num == 0 -> toolbar.vanishPrev()
                num >= globalVm.getTotalPageNum() -> toolbar.vanishNext()
                else -> toolbar.visibleBookNav()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        TAG.infoLog("Current navigation fragment is ${navController.currentDestination?.id}")

        when(navController.currentDestination?.id) {
            R.id.videoFragment -> {
                TAG.infoLog("On Video Fragment")

                return true
            }
            R.id.bookFragment -> {
                TAG.infoLog("On Book Fragment")

                menuInflater.inflate(R.menu.toolbar_book, menu)

                return true
            }
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.bookPrev -> {
                globalVm.setPrevPage()

                return true
            }
            R.id.bookNext -> {
                globalVm.setNextPage()

                return true
            }
        }

        return false
    }
}
