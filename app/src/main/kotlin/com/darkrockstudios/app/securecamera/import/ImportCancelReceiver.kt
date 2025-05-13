package com.darkrockstudios.app.securecamera.import

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.darkrockstudios.app.securecamera.import.ImportWorker.Companion.NOTIFICATION_ID
import timber.log.Timber

/**
 * BroadcastReceiver to handle cancellation of the ImportWorker
 */
class ImportCancelReceiver : BroadcastReceiver() {
	companion object {
		const val ACTION_CANCEL_IMPORT = "com.darkrockstudios.app.securecamera.import.CANCEL_IMPORT"
		const val EXTRA_WORKER_ID = "EXTRA_WORKER_ID"
	}

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == ACTION_CANCEL_IMPORT) {
			val workerId = intent.getStringExtra(EXTRA_WORKER_ID)
			if (workerId != null) {
				Timber.d("Cancelling import worker: $workerId")
				WorkManager.getInstance(context).cancelWorkById(workerId.toUUID())

				// Dismiss notification when worker is cancelled
				val notificationManager =
					context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
				notificationManager.cancel(NOTIFICATION_ID)
			} else {
				Timber.e("No worker ID provided in cancel intent")
			}
		}
	}

	private fun String.toUUID() = java.util.UUID.fromString(this)
}
