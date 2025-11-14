package com.proshot.camera.data.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gallery manager for browsing and organizing photos/videos
 * Integrates with Android MediaStore
 */
@Singleton
class GalleryManager @Inject constructor(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Reload media when MediaStore changes
            Timber.d("MediaStore changed, reloading gallery")
        }
    }

    init {
        // Register content observer for MediaStore changes
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )

        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    /**
     * Load all media items (photos and videos)
     */
    suspend fun loadMedia(
        sortOrder: MediaSortOrder = MediaSortOrder.DATE_DESC,
        filterType: MediaType? = null
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val items = mutableListOf<MediaItem>()

            // Load images if not filtering for videos only
            if (filterType == null || filterType == MediaType.IMAGE) {
                items.addAll(loadImages())
            }

            // Load videos if not filtering for images only
            if (filterType == null || filterType == MediaType.VIDEO) {
                items.addAll(loadVideos())
            }

            // Sort items
            val sortedItems = when (sortOrder) {
                MediaSortOrder.DATE_DESC -> items.sortedByDescending { it.dateAdded }
                MediaSortOrder.DATE_ASC -> items.sortedBy { it.dateAdded }
                MediaSortOrder.NAME_ASC -> items.sortedBy { it.displayName }
                MediaSortOrder.NAME_DESC -> items.sortedByDescending { it.displayName }
                MediaSortOrder.SIZE_DESC -> items.sortedByDescending { it.size }
                MediaSortOrder.SIZE_ASC -> items.sortedBy { it.size }
            }

            _mediaItems.value = sortedItems

            Timber.d("Loaded ${sortedItems.size} media items")
            Result.success(sortedItems)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load media")
            Result.failure(e)
        }
    }

    /**
     * Load images from MediaStore
     */
    private fun loadImages(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val mimeType = cursor.getString(mimeColumn)
                val path = cursor.getString(dataColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                items.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        dateAdded = dateAdded,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        type = MediaType.IMAGE,
                        duration = null
                    )
                )
            }
        }

        return items
    }

    /**
     * Load videos from MediaStore
     */
    private fun loadVideos(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val mimeType = cursor.getString(mimeColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                items.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        dateAdded = dateAdded,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        type = MediaType.VIDEO,
                        duration = duration
                    )
                )
            }
        }

        return items
    }

    /**
     * Load albums (grouped by folder)
     */
    suspend fun loadAlbums(): Result<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val albumMap = mutableMapOf<String, MutableList<MediaItem>>()

            // Group all media items by their parent folder
            _mediaItems.value.forEach { item ->
                val folder = File(item.path).parent ?: "Unknown"
                albumMap.getOrPut(folder) { mutableListOf() }.add(item)
            }

            // Convert to Album objects
            val albums = albumMap.map { (folder, items) ->
                Album(
                    name = File(folder).name,
                    path = folder,
                    coverItem = items.firstOrNull(),
                    itemCount = items.size,
                    items = items
                )
            }.sortedByDescending { it.itemCount }

            _albums.value = albums

            Timber.d("Loaded ${albums.size} albums")
            Result.success(albums)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load albums")
            Result.failure(e)
        }
    }

    /**
     * Delete media item
     */
    suspend fun deleteMediaItem(item: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deleted = contentResolver.delete(item.uri, null, null)

            if (deleted > 0) {
                // Remove from local list
                _mediaItems.value = _mediaItems.value.filter { it.id != item.id }
                Timber.d("Media item deleted: ${item.displayName}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete media item"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete media item")
            Result.failure(e)
        }
    }

    /**
     * Get media item by ID
     */
    fun getMediaItemById(id: Long): MediaItem? {
        return _mediaItems.value.find { it.id == id }
    }

    /**
     * Filter media items by date range
     */
    fun filterByDateRange(startDate: Long, endDate: Long): List<MediaItem> {
        return _mediaItems.value.filter { it.dateAdded in startDate..endDate }
    }

    /**
     * Search media items by name
     */
    fun searchByName(query: String): List<MediaItem> {
        return _mediaItems.value.filter {
            it.displayName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get media statistics
     */
    fun getStatistics(): MediaStatistics {
        val images = _mediaItems.value.filter { it.type == MediaType.IMAGE }
        val videos = _mediaItems.value.filter { it.type == MediaType.VIDEO }

        return MediaStatistics(
            totalItems = _mediaItems.value.size,
            totalImages = images.size,
            totalVideos = videos.size,
            totalSize = _mediaItems.value.sumOf { it.size },
            rawImages = images.count { it.mimeType.contains("dng", ignoreCase = true) },
            jpegImages = images.count { it.mimeType.contains("jpeg", ignoreCase = true) }
        )
    }

    /**
     * Release resources
     */
    fun release() {
        contentResolver.unregisterContentObserver(contentObserver)
    }
}

/**
 * Media item (photo or video)
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val path: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val type: MediaType,
    val duration: Long? = null  // For videos
) {
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024f)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024f * 1024f))
            else -> String.format("%.2f GB", size / (1024f * 1024f * 1024f))
        }

    val resolution: String
        get() = "${width}x${height}"

    val isRaw: Boolean
        get() = mimeType.contains("dng", ignoreCase = true) ||
                mimeType.contains("raw", ignoreCase = true)
}

/**
 * Media type
 */
enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * Album (folder grouping)
 */
data class Album(
    val name: String,
    val path: String,
    val coverItem: MediaItem?,
    val itemCount: Int,
    val items: List<MediaItem>
)

/**
 * Media sort order
 */
enum class MediaSortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC
}

/**
 * Media statistics
 */
data class MediaStatistics(
    val totalItems: Int,
    val totalImages: Int,
    val totalVideos: Int,
    val totalSize: Long,
    val rawImages: Int,
    val jpegImages: Int
) {
    val formattedTotalSize: String
        get() = when {
            totalSize < 1024 * 1024 -> String.format("%.1f KB", totalSize / 1024f)
            totalSize < 1024 * 1024 * 1024 -> String.format("%.1f MB", totalSize / (1024f * 1024f))
            else -> String.format("%.2f GB", totalSize / (1024f * 1024f * 1024f))
        }
}
