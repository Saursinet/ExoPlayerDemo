package com.example.exoplayerdemo

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.Serializable

class MainActivity : AppCompatActivity(), Player.Listener {

    private var player: SimpleExoPlayer? = null
    private var mediaSource: MediaSource? = null
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player = SimpleExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder().build()
            )
            .build()

        playerView = findViewById(R.id.player_view)
        playerView?.player = player
        player?.playWhenReady = true
        player?.addListener(this)
        mediaMetadataRetriever = MediaMetadataRetriever()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, 42)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            42 -> {
                print(data)
                data?.data?.let {
                    loadVideo(it)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun loadVideo(videoUrl: Uri) {
        val trimPosition = TrimmingRange()

        val userAgent = Util.getUserAgent(this, "Vormats")
        val mediaDataSourceFactory =
            DefaultDataSourceFactory(this, userAgent)

        buildMediaSource(videoUrl, mediaDataSourceFactory).let { mSource ->
            val clippingSource = ClippingMediaSource(
                mSource,
                C.msToUs(trimPosition.start),
                if (trimPosition.end < 0) C.TIME_END_OF_SOURCE
                else C.msToUs(trimPosition.end)
            )
            mediaSource = mSource
            player!!.let {
                it.setMediaSource(clippingSource, false)
                it.prepare()
            }
        }
    }

    private fun buildMediaSource(
        uri: Uri,
        mediaDataSourceFactory: DataSource.Factory
    ): MediaSource {
        val mediaItem = MediaItem.fromUri(uri)
        return when (Util.inferContentType(uri)) {
            C.TYPE_SS -> SsMediaSource.Factory(
                DefaultSsChunkSource.Factory(mediaDataSourceFactory), mediaDataSourceFactory
            ).createMediaSource(mediaItem)
            C.TYPE_DASH -> DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory), mediaDataSourceFactory
            ).createMediaSource(mediaItem)
            C.TYPE_HLS -> HlsMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(mediaItem)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(mediaItem)
            else -> {
                error("Unsupported type")
            }
        }
    }

}

data class TrimmingRange(var start: Long = 0, var end: Long = -1) : Serializable