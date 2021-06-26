package rs.moma.dugme

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.N)
class DugmeTileService : TileService() {
    override fun onClick() {
        thread {
            val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
            if (!sharedPref.getBoolean("OnOff", false)) {
                sharedPref.edit().putBoolean("OnOff", true).apply()
                sharedPref.edit().putLong("LastPlay", System.currentTimeMillis()).apply()
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.updateTile()
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_PLAY_SOUND)
                val mediaPlayer = MediaPlayer.create(this, R.raw.buzz)
                mediaPlayer.start()
                Thread.sleep(1600)
                while (mediaPlayer.isPlaying)
                    Thread.sleep(5)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND)
                sharedPref.edit().putBoolean("OnOff", false).apply()
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.updateTile()
                mediaPlayer.release()
            }
        }
    }

    override fun onStartListening() {
        val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
        val lastPlay = sharedPref.getLong("LastPlay", -1)
        if (lastPlay != -1L && System.currentTimeMillis() - lastPlay > 2500)
            sharedPref.edit().putBoolean("OnOff", false).apply()
        qsTile.state = if (sharedPref.getBoolean("OnOff", false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}