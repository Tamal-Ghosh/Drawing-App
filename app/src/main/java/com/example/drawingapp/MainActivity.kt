package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import android.content.res.Configuration


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawingView: DrawingView
    private lateinit var brushButton: ImageButton
    private lateinit var purpleButton: ImageButton
    private lateinit var redButton: ImageButton
    private lateinit var greenButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var orangeButton: ImageButton
    private lateinit var colorPickerButton: ImageButton
    private lateinit var gallaryButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var bgColorChangeButton: ImageButton
    private lateinit var redoButton: ImageButton
    private var lastSavedImageUri: android.net.Uri? = null

    private lateinit var rootLayout: ConstraintLayout // Added this

    private var currentBrushSize = 15f
    private val recentColors = mutableListOf("#F006F0", "#FF1000", "#0AFB14", "#048EFA", "#FF4D00")
    private lateinit var colorButtons: List<ImageButton>

    private enum class PermissionRequestType { GALLERY, SAVE }
    private var currentPermissionRequestType: PermissionRequestType? = null

    private val openGallaryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            imageUri?.let {
                val imageView = findViewById<ImageView>(R.id.gallary_image)
                imageView.setImageURI(it)
                imageView.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val readGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true

        when (currentPermissionRequestType) {
            PermissionRequestType.GALLERY -> if (readGranted) openGallery() else toast("Gallery permission denied")
            PermissionRequestType.SAVE -> {
                if (writeGranted) {
                    val bitmap = getBitmapFromView(rootLayout)
                    CoroutineScope(Dispatchers.IO).launch { saveImage(bitmap) }
                } else toast("Save permission denied")
            }
            else -> {}
        }
        currentPermissionRequestType = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawer = findViewById(R.id.drawer_layout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.dark_mode -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    true
                }
                R.id.light_mode -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    true
                }
                R.id.share -> {
                    lastSavedImageUri?.let { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share drawing via"))
                    } ?: run {
                        toast("No image saved yet to share.")
                    }
                    true
                }

                else -> false
            }
        }

        purpleButton = findViewById(R.id.purple_button)
        redButton = findViewById(R.id.red_button)
        greenButton = findViewById(R.id.green_button)
        blueButton = findViewById(R.id.blue_button)
        orangeButton = findViewById(R.id.orange_button)
        colorButtons = listOf(purpleButton, redButton, greenButton, blueButton, orangeButton)

        updateColorButtons()

        undoButton = findViewById(R.id.undo_button)
        colorPickerButton = findViewById(R.id.colorPicker_button)
        gallaryButton = findViewById(R.id.gallery_button)
        brushButton = findViewById(R.id.brush_button)
        saveButton = findViewById(R.id.save_button)
        bgColorChangeButton = findViewById(R.id.bg_colorPicker_button)
        redoButton = findViewById(R.id.redo_button)
        drawingView = findViewById(R.id.drawingView)

        drawingView.changeBrushSize(currentBrushSize)

        // *** Fix here: initialize rootLayout and set its background based on dark mode ***
        rootLayout = findViewById(R.id.Constraint_layout3)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isDarkMode) ContextCompat.getColor(this, R.color.dark_background) else ContextCompat.getColor(this, R.color.white)
        rootLayout.setBackgroundColor(bgColor)
        // *** end fix ***

        listOf(
            purpleButton, redButton, greenButton, blueButton, orangeButton,
            undoButton, colorPickerButton, gallaryButton, saveButton, bgColorChangeButton, redoButton
        ).forEach { it.setOnClickListener(this) }

        brushButton.setOnClickListener { showBrushChooserDialog() }
        drawer.closeDrawer(GravityCompat.START, false)

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.purple_button -> drawingView.setColor(recentColors[0])
            R.id.red_button -> drawingView.setColor(recentColors[1])
            R.id.green_button -> drawingView.setColor(recentColors[2])
            R.id.blue_button -> drawingView.setColor(recentColors[3])
            R.id.orange_button -> drawingView.setColor(recentColors[4])
            R.id.undo_button -> drawingView.undoPath()
            R.id.redo_button -> drawingView.redoPath()
            R.id.colorPicker_button -> showColorPickerDialog()
            R.id.gallery_button -> requestGalleryPermission()
            R.id.save_button -> {
                val bitmap = getBitmapFromView(rootLayout)
                CoroutineScope(Dispatchers.IO).launch {
                    saveImageToMediaStore(bitmap, this@MainActivity)
                }
            }
            R.id.bg_colorPicker_button -> {
                // *** Fix here: change rootLayout background instead of findViewById ***
                showBgColorPickerDialog()
            }
        }
    }

    private fun requestGalleryPermission() {
        currentPermissionRequestType = PermissionRequestType.GALLERY
        requestStoragePermission()
    }

    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush)
        val seekBar = brushDialog.findViewById<SeekBar>(R.id.dialog_seek_bar)
        val progressText = brushDialog.findViewById<TextView>(R.id.dialog_text_view_progress)

        seekBar.progress = currentBrushSize.toInt()
        progressText.text = currentBrushSize.toInt().toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrushSize = progress.toFloat()
                drawingView.changeBrushSize(currentBrushSize)
                progressText.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brushDialog.show()
    }

    private fun showColorPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.BLACK, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                val lastColor = drawingView.getCurrentColor()
                recentColors.add(0, String.format("#%06X", 0xFFFFFF and lastColor))
                if (recentColors.size > 5) recentColors.removeAt(recentColors.size - 1)
                updateColorButtons()
                drawingView.setColor(color)
            }
        })
        dialog.show()
    }

    private fun showBgColorPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.BLACK, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                rootLayout.setBackgroundColor(color) // Changed to rootLayout here
            }
        })
        dialog.show()
    }

    private fun openGallery() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGallaryLauncher.launch(pickIntent)
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        requestPermission.launch(permissions)
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "saved_image")
        if (!dir.exists()) dir.mkdir()
        val file = File(dir, "Image-${Random.nextInt(1000)}.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        withContext(Dispatchers.Main) {
            toast("${file.absolutePath} saved!")
        }
    }

    suspend fun saveImageToMediaStore(bitmap: Bitmap, context: Context) {
        withContext(Dispatchers.IO) {
            val filename = "Image-${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/saved_image")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                // Save the URI globally on the main thread
                withContext(Dispatchers.Main) {
                    lastSavedImageUri = it
                    toast("Image saved!")
                }
            }
        }
    }


    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun updateColorButtons() {
        colorButtons.forEachIndexed { idx, btn ->
            val parsedColor = Color.parseColor(recentColors[idx])
            btn.setColorFilter(parsedColor)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(parsedColor)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }
}
