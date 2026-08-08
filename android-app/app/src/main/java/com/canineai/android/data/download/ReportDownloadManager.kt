package com.canineai.android.data.download

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun save(reportId: String, bytes: ByteArray): File {
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "clinical-reports"
        ).apply { mkdirs() }
        return File(directory, "canineai-report-$reportId.pdf").apply { writeBytes(bytes) }
    }
}
