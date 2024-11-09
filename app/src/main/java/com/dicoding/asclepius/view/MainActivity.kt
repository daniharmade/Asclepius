package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.model.CancerHistory
import com.dicoding.asclepius.utils.FIRST_LABEL_RESULT_EXTRA
import com.dicoding.asclepius.utils.FIRST_SCORE_RESULT_EXTRA
import com.dicoding.asclepius.utils.IMAGE_URI_EXTRA
import com.dicoding.asclepius.utils.SECOND_LABEL_RESULT_EXTRA
import com.dicoding.asclepius.utils.SECOND_SCORE_RESULT_EXTRA
import com.dicoding.asclepius.viewmodel.HistoryViewModel
import com.dicoding.asclepius.viewmodel.HistoryViewModelFactory
import com.dicoding.asclepius.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.text.NumberFormat
import java.util.Date
import com.yalantis.ucrop.UCrop

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe URI gambar yang disimpan di ViewModel
        viewModel.currentImageUri.observe(this) { uri ->
            if (uri != null) {
                binding.previewImageView.setImageURI(uri)
            }
        }

        val cancerHistoryViewModelFactory = HistoryViewModelFactory.getInstance(this)
        val cancerHistoryViewModel: HistoryViewModel by viewModels {
            cancerHistoryViewModelFactory
        }

        with(binding) {
            galleryButton.setOnClickListener { startGallery() }
            analyzeButton.setOnClickListener {
                viewModel.currentImageUri.value?.let {
                    analyzeImage(it, cancerHistoryViewModel)
                } ?: showToast("Image Uri is null")
            }
            topAppBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.history_button -> {
                        Intent(this@MainActivity, HistoryActivity::class.java).apply {
                            startActivity(this)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun startGallery() =
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            launchUCrop(uri)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun launchUCrop(uri: Uri) {
        val timestamp = Date().time
        val cachedImage = File(cacheDir, "cropped_image_${timestamp}.jpg")

        val destinationUri = Uri.fromFile(cachedImage)

        val uCrop = UCrop.of(uri, destinationUri).withAspectRatio(1f, 1f)

        uCrop.getIntent(this@MainActivity).apply {
            launcherUCrop.launch(this) // "this" keyword is reference to intent, not activity
        }
    }

    private val launcherUCrop =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    // Simpan URI gambar ke ViewModel
                    viewModel.setCurrentImageUri(resultUri)
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val error = UCrop.getError(result.data!!)
                showToast("Error: ${error?.localizedMessage}")
            }
        }

    private fun analyzeImage(imageUri: Uri, cancerHistoryViewModel: HistoryViewModel) {
        CoroutineScope(Dispatchers.Main).launch {
            showProgressAndDisableButtons(true)
            var cancerHistory = CancerHistory(label = "", confidenceScore = "", image = "")

            try {
                withContext(Dispatchers.IO) {
                    val imageClassifierHelper = ImageClassifierHelper(context = this@MainActivity,
                        classifierListener = object : ImageClassifierHelper.ClassifierListener {
                            override fun onError(error: String) {
                                showToast("Error: $error")
                            }

                            override fun onResults(
                                results: List<Classifications>?,
                                inferenceTime: Long
                            ) {
                                results?.let { listClassification ->
                                    if (listClassification.isNotEmpty() && listClassification[0].categories.isNotEmpty()) {
                                        val sortedCategories =
                                            listClassification[0].categories.sortedByDescending { it?.score }
                                        cancerHistory = CancerHistory(
                                            label = sortedCategories[0].label,
                                            confidenceScore = formatNumberToPercent(sortedCategories[0].score),
                                            image = imageUri.toString()
                                        )
                                        moveToResult(sortedCategories)
                                    }
                                }
                            }
                        })

                    imageClassifierHelper.classifyStaticImage(imageUri)
                    cancerHistoryViewModel.insertCancerHistory(cancerHistory)
                    withContext(Dispatchers.Main) {
                        showProgressAndDisableButtons(false)
                    }
                }
            } catch (e: Exception) {
                Log.d("MainActivity", e.message.toString())
            }
        }
    }

    private fun moveToResult(analyzeResult: List<Category>) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(IMAGE_URI_EXTRA, viewModel.currentImageUri.value.toString())
        intent.putExtra(FIRST_LABEL_RESULT_EXTRA, analyzeResult[0].label)
        intent.putExtra(FIRST_SCORE_RESULT_EXTRA, formatNumberToPercent(analyzeResult[0].score))
        intent.putExtra(SECOND_LABEL_RESULT_EXTRA, analyzeResult[1].label)
        intent.putExtra(SECOND_SCORE_RESULT_EXTRA, formatNumberToPercent(analyzeResult[1].score))
        startActivity(intent)
    }

    private fun showProgressAndDisableButtons(isActive: Boolean) {
        with(binding) {
            progressIndicator.visibility = if (isActive) View.VISIBLE else View.INVISIBLE
            galleryButton.isEnabled = !isActive
            analyzeButton.isEnabled = !isActive
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatNumberToPercent(score: Float): String =
        NumberFormat.getPercentInstance().format(score)
}
