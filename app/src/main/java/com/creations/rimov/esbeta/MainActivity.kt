package com.creations.rimov.esbeta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.creations.rimov.esbeta.extensions.infoLog
import com.creations.rimov.esbeta.view_models.GlobalViewModel
import com.creations.rimov.esbeta.views.MainToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private val TAG = this::class.java.simpleName

    private val navHostFrag: NavHostFragment by lazy { mainNavHostFrag as NavHostFragment }
    private val navController: NavController by lazy { navHostFrag.navController }

    private val toolbar: Toolbar by lazy { mainToolbar }

    private val globalVm by lazy {
        ViewModelProviders.of(this).get(GlobalViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        navController.addOnDestinationChangedListener(this)

        setSupportActionBar(toolbar)

        globalVm.getPageNum().observe(this, Observer { num ->

        })
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(navController.currentDestination?.id) {
            R.id.videoFragment -> {
                TAG.infoLog("On Video Fragment")

                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                return true
            }
            R.id.bookFragment -> {
                TAG.infoLog("On Book Fragment")

                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                menuInflater.inflate(R.menu.toolbar_book, menu)

                return true
            }
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        TAG.infoLog("Selecting item ${item.itemId}")

        when(item.itemId) {
            android.R.id.home -> {
                navController.navigate(R.id.mainMenuFragment)
            }
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

    /*Should be the way to toggle menu items, but issues appear*/
//    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        TAG.infoLog("Is menu null? ${menu == null}. Current destination is bookFrag? ${navController.currentDestination?.id == R.id.bookFragment}")
//
//        if(menu == null) return false
//
//        when(navController.currentDestination?.id) {
//            R.id.bookFragment -> {
//                val num = globalVm.getPageNum().value ?: return false
//
//                val btnPrev = menu.findItem(R.id.bookPrev)
//                val btnNext = menu.findItem(R.id.bookNext)
//
//                when {
//                    num == 0 -> {
//                        btnPrev.setEnabled(false)
//                        btnNext.setEnabled(num < globalVm.getTotalPageNum())
//                    }
//                    num >= globalVm.getTotalPageNum() -> {
//                        btnPrev.setEnabled(true)
//                        btnNext.setEnabled(false)
//                    }
//                    else -> {
//                        btnPrev.setEnabled(true)
//                        btnNext.setEnabled(true)
//                    }
//                }
//
//                return true
//            }
//        }
//
//        return false
//    }
}
