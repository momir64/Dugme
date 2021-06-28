package rs.moma.dugme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageView>(R.id.dugme).setOnClickListener {
            thread {
                val sharedPref = getSharedPreferences("QuickSettingsTale", MODE_PRIVATE)
                if (!sharedPref.getBoolean("OnOff", false)) {
                    var duration = 1700L
                    val mediaPlayer: MediaPlayer
                    sharedPref.getString("Sound", null).let { uri ->
                        if (uri == null)
                            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.buzz)
                        else {
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
                    mediaPlayer.release()
                } else {
                    val lastPlay = sharedPref.getLong("LastPlay", -1)
                    val duration = sharedPref.getLong("Duration", 2500)
                    if (lastPlay != -1L && System.currentTimeMillis() - lastPlay > duration)
                        sharedPref.edit().putBoolean("OnOff", false).apply()
                }
            }
        }

        findViewById<ImageView>(R.id.changeSound).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*")
            resultLauncher.launch(intent)
        }

        findViewById<ImageView>(R.id.restoreSound).setOnClickListener {
            val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
            sharedPref.edit().remove("Sound").apply()
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                applicationContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val sharedPref = getSharedPreferences("QuickSettingsTale", Context.MODE_PRIVATE)
                sharedPref.edit().putString("Sound", it.toString()).apply()
            }
        }
    }
}