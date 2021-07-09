package rs.moma.dugme

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.N)
class DugmeTileService : TileService() {
    override fun onClick() {
        thread {
            val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
            if (!sharedPref.getBoolean("OnOff", false)) {
                var duration = 1700L
                val mediaPlayer: MediaPlayer
                sharedPref.getString("Sound", null).let { uri ->
                    if (uri == null || !DocumentFile.fromSingleUri(applicationContext, uri.toUri())!!.exists()) {
                        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.buzz)
                        if (uri != null)
                            sharedPref.edit().remove("Sound").apply()
                    } else {
                        mediaPlayer = MediaPlayer()
                        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri.toUri(), "r")
                        mediaPlayer.setDataSource(parcelFileDescriptor?.fileDescriptor)
                        mediaPlayer.prepare()
                        parcelFileDescriptor?.close()
                        MediaMetadataRetriever().let {
                            it.setDataSource(applicationContext, uri.toUri())
                            duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                        }
                    }
                }
                mediaPlayer.start()
                sharedPref.edit().putBoolean("OnOff", true).apply()
                sharedPref.edit().putLong("LastPlay", System.currentTimeMillis()).apply()
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.updateTile()
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_PLAY_SOUND)
                sharedPref.edit().putLong("Duration", duration).apply()
                Thread.sleep((duration - 100).coerceAtLeast(0))
                while (mediaPlayer.isPlaying)
                    Thread.sleep(5)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND)
                sharedPref.edit().putBoolean("OnOff", false).apply()
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
                mediaPlayer.release()
            } else
                onStartListening()
        }
    }

    override fun onStartListening() {
        val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
        val lastPlay = sharedPref.getLong("LastPlay", -1)
        val duration = sharedPref.getLong("Duration", 2500)
        if (lastPlay != -1L && System.currentTimeMillis() - lastPlay > duration)
            sharedPref.edit().putBoolean("OnOff", false).apply()
        qsTile.state = if (sharedPref.getBoolean("OnOff", false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}