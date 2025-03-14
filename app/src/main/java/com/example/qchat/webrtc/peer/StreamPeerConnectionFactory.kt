package com.example.qchat.webrtc.peer

import android.content.Context
import org.webrtc.*

class StreamPeerConnectionFactory(private val context: Context) {

    private val eglBase: EglBase by lazy {
        EglBase.create()
    }


    val eglBaseContext: EglBase.Context
        get() = eglBase.eglBaseContext

    private val videoDecoderFactory: VideoDecoderFactory by lazy {
        DefaultVideoDecoderFactory(eglBaseContext)
    }

    private val videoEncoderFactory: VideoEncoderFactory by lazy {
        HardwareVideoEncoderFactory(eglBaseContext, true, true)
    }

    val rtcConfig: PeerConnection.RTCConfiguration by lazy {
        PeerConnection.RTCConfiguration(
            arrayListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
    }

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .createPeerConnectionFactory()
    }

    fun makePeerConnection(
        configuration: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(configuration, observer)
    }

    fun makeAudioSource(mediaConstraints: MediaConstraints): AudioSource {
        return peerConnectionFactory.createAudioSource(mediaConstraints)
    }

    fun makeAudioTrack(audioSource: AudioSource, trackId: String): AudioTrack {
        return peerConnectionFactory.createAudioTrack(trackId, audioSource)
    }

    fun makeVideoSource(isScreencast: Boolean): VideoSource {
        return peerConnectionFactory.createVideoSource(isScreencast)
    }

    fun makeVideoTrack(videoSource: VideoSource, trackId: String): VideoTrack {
        return peerConnectionFactory.createVideoTrack(trackId, videoSource)
    }
}
