package com.example.diakontmobilescanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.diakontmobilescanner.BarcodeAnalyzer
import com.example.diakontmobilescanner.R
import com.example.diakontmobilescanner.viewmodel.ScanViewModel

class ScanFragment : Fragment() {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var previewView: PreviewView
    private val viewModel: ScanViewModel by activityViewModels()
    private var lastScanTime = 0L
    private val scanInterval = 2000L // 2 секунды пауза между сканами

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_scan, container, false).also {
        previewView = it.findViewById(R.id.previewView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkCameraPermission()
        startCamera()

        view.findViewById<Button>(R.id.btn_history).setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

    }

    @SuppressLint("ServiceCast")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(requireContext()),
                    BarcodeAnalyzer { barcode ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastScanTime > scanInterval) {
                            lastScanTime = currentTime
                            viewModel.addBarcode(barcode)

                            val vibrator =
                                context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        100,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                vibrator.vibrate(100)
                            }
                        }
                    })
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)

// Включить автофокус
            camera.cameraControl.enableTorch(false)

            val focusPoint = previewView.meteringPointFactory.createPoint(
                previewView.width / 2f,
                previewView.height / 2f
            )
            val action =
                FocusMeteringAction.Builder(focusPoint, FocusMeteringAction.FLAG_AF).build()

            camera.cameraControl.startFocusAndMetering(action)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private val CAMERA_PERMISSION_CODE = 1001

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}