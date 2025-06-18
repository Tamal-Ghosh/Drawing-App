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
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var drawingView: DrawingView
    private lateinit var brushButton: ImageButton
    private var currentBrushSize = 15f
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
    // Corrected color order: purple, red, green, blue, orange
    private val recentColors = mutableListOf(
        "#F006F0", // purple
        "#FF1000", // red
        "#0AFB14", // green
        "#048EFA", // blue
        "#FF4D00"  // orange
    )
    private lateinit var colorButtons: List<ImageButton>

    private enum class PermissionRequestType {
        GALLERY, SAVE
    }

    private var currentPermissionRequestType: PermissionRequestType? = null

    private val openGallaryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val readGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }

            val writeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
            } else {
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
            }

            when (currentPermissionRequestType) {
                PermissionRequestType.GALLERY -> {
                    if (readGranted) {
                        openGallery()
                    } else {
                        Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
                PermissionRequestType.SAVE -> {
                    if (writeGranted) {
                        val layout = findViewById<ConstraintLayout>(R.id.Constraint_layout3)
                        val bitmap = getBitmapFromView(layout)
                        CoroutineScope(Dispatchers.IO).launch {
                            saveImage(bitmap)
                        }
                    } else {
                        Toast.makeText(this, "Save permission denied", Toast.LENGTH_SHORT).show()
                    }
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

        // Color buttons
        purpleButton = findViewById(R.id.purple_button)
        redButton = findViewById(R.id.red_button)
        greenButton = findViewById(R.id.green_button)
        blueButton = findViewById(R.id.blue_button)
        orangeButton = findViewById(R.id.orange_button)
        colorButtons = listOf(purpleButton, redButton, greenButton, blueButton, orangeButton)

        updateColorButtons()

        // Tool buttons
        undoButton = findViewById(R.id.undo_button)
        colorPickerButton = findViewById(R.id.colorPicker_button)
        gallaryButton = findViewById(R.id.gallery_button)
        brushButton = findViewById(R.id.brush_button)
        saveButton = findViewById(R.id.save_button)
        bgColorChangeButton = findViewById(R.id.bg_colorPicker_button)
        redoButton = findViewById(R.id.redo_button)

        drawingView = findViewById(R.id.drawingView)
        drawingView.changeBrushSize(currentBrushSize)

        listOf(
            purpleButton, redButton, greenButton, blueButton, orangeButton,
            undoButton, colorPickerButton, gallaryButton, saveButton, bgColorChangeButton, redoButton
        ).forEach {
            it.setOnClickListener(this)
        }

        brushButton.setOnClickListener {
            showBrushChooserDialog()
        }
    }

    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush)

        val seekBarProgress = brushDialog.findViewById<SeekBar>(R.id.dialog_seek_bar)
        val showProgressTextView = brushDialog.findViewById<TextView>(R.id.dialog_text_view_progress)

        seekBarProgress.progress = currentBrushSize.toInt()
        showProgressTextView.text = currentBrushSize.toInt().toString()

        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newSize = progress.toFloat()
                currentBrushSize = newSize
                drawingView.changeBrushSize(newSize)
                showProgressTextView.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brushDialog.show()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.purple_button -> drawingView.setColor(recentColors[0])
            R.id.red_button    -> drawingView.setColor(recentColors[1])
            R.id.green_button  -> drawingView.setColor(recentColors[2])
            R.id.blue_button   -> drawingView.setColor(recentColors[3])
            R.id.orange_button -> drawingView.setColor(recentColors[4])
            R.id.undo_button -> drawingView.undoPath()
            R.id.redo_button -> drawingView.redoPath()
            R.id.colorPicker_button -> showColorPickerDialog()
            R.id.gallery_button -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestStoragePermission()
                    } else {
                        openGallery()
                    }
                } else {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestStoragePermission()
                    } else {
                        openGallery()
                    }
                }
            }
            R.id.save_button -> {
                val layout = findViewById<ConstraintLayout>(R.id.Constraint_layout3)
                val bitmap = getBitmapFromView(layout)
                CoroutineScope(Dispatchers.IO).launch {
                    saveImageToMediaStore(bitmap, this@MainActivity)
                }
            }
            brushButton.id -> {
                showBrushChooserDialog()
            }
            bgColorChangeButton.id -> {
                showBgColorPickerDialog()
            }
        }
    }

    private fun showBgColorPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.BLACK, object : OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                findViewById<ConstraintLayout>(R.id.Constraint_layout3).setBackgroundColor(color)
            }
        })
        dialog.show()
    }

    private fun showColorPickerDialog() {
        val dialog = AmbilWarnaDialog(this, Color.BLACK, object : OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                val lastColor=drawingView.getCurrentColor()
                recentColors.add(0, String.format("#%06X", 0xFFFFFF and lastColor))
                if (recentColors.size > 5) {
                    recentColors.removeAt(recentColors.size - 1)
                }
                updateColorButtons()
                drawingView.setColor(color)
            }
        })
        dialog.show()
    }

    private fun openGallery() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGallaryLauncher.launch(pickIntent)
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showRationaleDialog()
            } else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission")
            .setMessage("We need this permission to access internal storage.")
            .setPositiveButton("Yes") { dialog, _ ->
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val mydir = File("$root/saved_image")
        if (!mydir.exists()) {
            mydir.mkdir()
        }
        val generator = Random(System.currentTimeMillis())
        val n = generator.nextInt(1000)
        val outputFile = File(mydir, "Image-$n.jpg")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        try {
            val out = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${outputFile.absolutePath} saved!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateColorButtons() {
        colorButtons.forEachIndexed { idx, btn ->
            val parsedColor = Color.parseColor(recentColors[idx])
            btn.setColorFilter(parsedColor)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(parsedColor)
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

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}