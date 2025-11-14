package com.proshot.camera.presentation.camera

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.proshot.camera.presentation.camera.components.CameraControls
import com.proshot.camera.presentation.camera.components.CameraPreview
import com.proshot.camera.presentation.theme.PureBlack
import timber.log.Timber

/**
 * Main camera screen with professional controls
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request camera permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PureBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PureBlack)
                .systemBarsPadding()
        ) {
            when {
                !permissionsState.allPermissionsGranted -> {
                    // Show permission required message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera permission is required",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                uiState.isLoading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Opening camera...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                else -> {
                    // Main camera interface
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        gridType = uiState.currentGrid
                    )

                    // Professional manual controls overlay
                    CameraControls(
                        modifier = Modifier.fillMaxSize(),
                        cameraState = cameraState,
                        uiState = uiState,
                        onISOChange = viewModel::adjustISO,
                        onShutterSpeedChange = viewModel::adjustShutterSpeed,
                        onWhiteBalanceChange = viewModel::adjustWhiteBalance,
                        onExposureCompensationChange = viewModel::adjustExposureCompensation,
                        onFocusModeChange = viewModel::setFocusMode,
                        onCapture = { viewModel.captureImage() },
                        onToggleGrid = viewModel::toggleGrid,
                        onToggleHistogram = viewModel::toggleHistogram,
                        onToggleLevel = viewModel::toggleLevel
                    )
                }
            }
        }
    }
}
