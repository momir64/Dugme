package rs.moma.dugme

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thread {
            val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
            if (!sharedPref.getBoolean("OnOff", false)) {
                sharedPref.edit().putBoolean("OnOff", true).apply()
                sharedPref.edit().putLong("LastPlay", System.currentTimeMillis()).apply()
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
                mediaPlayer.release()
            }
        }
        finish()
    }
}