package com.example.qchat.webrtc

import SignalingClient
import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRtcManager(
    private val context: Context,
    private var signalingClient: SignalingClient? = null,
    private val localVideoRenderer: SurfaceViewRenderer? = null,
    private val remoteVideoRenderer: SurfaceViewRenderer? = null
) {
    private val TAG = "WebRtcManager"

    private var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    init {
        peerConnectionFactory = createPeerConnectionFactory()
        Log.d(TAG, "PeerConnectionFactory initialized successfully")
    }

    /**
     * Create the PeerConnectionFactory with WebRTC's necessary modules.
     */
    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val builder = PeerConnectionFactory.builder()
        return builder.createPeerConnectionFactory()
    }

    /**
     * Create a PeerConnection and configure SDP & ICE handling.
     */
    fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(createIceServers())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        signalingClient?.sendIceCandidate(it)
                        Log.d(TAG, "ICE Candidate sent: ${it.sdp}")
                    }
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d(TAG, "Media stream added: $stream")
                    stream?.audioTracks?.forEach { it.setEnabled(true) } // Enable remote audio
                    stream?.videoTracks?.forEach { track ->
                        track.setEnabled(true)
                        remoteVideoRenderer?.let { track.addSink(it) } // Render remote video
                    }
                }

                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                    Log.d(TAG, "Signaling state changed: $newState")
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE Connection state changed: $newState")
                }

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            }
        )
    }

    /**
     * Set up local audio and video tracks, depending on `isAudioOnly`.
     */
    fun setupLocalMediaTracks(isAudioOnly: Boolean = false) {
        try {
            // Setup audio track
            if (audioSource == null) {
                audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
            }
            if (audioTrack == null) {
                audioTrack = peerConnectionFactory.createAudioTrack("AUDIO_TRACK", audioSource)
            }

            if (!isAudioOnly) {
                // Setup video track
                val videoCapturer = createVideoCapturer()
                if (videoSource == null && videoCapturer != null) {
                    videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
                    videoCapturer.initialize(
                        SurfaceTextureHelper.create("CaptureThread", null),
                        context,
                        videoSource?.capturerObserver
                    )
                    videoCapturer.startCapture(1280, 720, 30)
                }
                if (videoTrack == null) {
                    videoTrack = peerConnectionFactory.createVideoTrack("VIDEO_TRACK", videoSource)
                    localVideoRenderer?.let { videoTrack?.addSink(it) } // Render local video
                }
            }

            Log.d(TAG, "Local media tracks setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up local media tracks: ${e.message}")
        }
    }

    /**
     * Create a video capturer.
     */
    private fun createVideoCapturer(): VideoCapturer? {
        val cameraEnumerator = Camera2Enumerator(context)
        val deviceNames = cameraEnumerator.deviceNames
        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                val videoCapturer = cameraEnumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) return videoCapturer
            }
        }
        for (deviceName in deviceNames) {
            val videoCapturer = cameraEnumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) return videoCapturer
        }
        return null
    }

    /**
     * Define ICE servers (STUN/TURN) for NAT traversal.
     */
    private fun createIceServers(): List<PeerConnection.IceServer> {
        return listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:your-turn-server-url")
                .setUsername("username")
                .setPassword("password")
                .createIceServer()
        )
    }

    /**
     * Handle remote SDP offer.
     */
    fun handleRemoteOffer(offer: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                createAnswer()
                applyPendingIceCandidates()
                Log.d(TAG, "Remote offer set successfully")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote offer: $error")
            }

            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, offer)
    }

    /**
     * Create SDP answer for a received offer.
     */
    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(this, it)
                    signalingClient?.sendAnswer(it)
                    Log.d(TAG, "Answer created and sent: ${it.description}")
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Answer creation failed: $error")
            }

            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    fun handleAnswer(answer: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote SDP answer set successfully")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote SDP answer: $error")
            }

            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, answer)
    }

    /**
     * Handle ICE candidates.
     */
    fun handleIceCandidate(candidate: IceCandidate) {
        if (peerConnection?.remoteDescription != null) {
            peerConnection?.addIceCandidate(candidate)
            Log.d(TAG, "ICE candidate added: $candidate")
        } else {
            pendingIceCandidates.add(candidate)
            Log.d(TAG, "Queued ICE candidate: $candidate")
        }
    }

    /**
     * Apply queued ICE candidates after the remote description is set.
     */
    private fun applyPendingIceCandidates() {
        for (candidate in pendingIceCandidates) {
            peerConnection?.addIceCandidate(candidate)
            Log.d(TAG, "Applied pending ICE candidate: $candidate")
        }
        pendingIceCandidates.clear()
    }

    /**
     * Start a new call with SDP offer.
     */
    fun startCall(isAudioOnly: Boolean = false) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", (!isAudioOnly).toString()))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(this, sdp)
                signalingClient?.sendOffer(sdp)
                Log.d(TAG, "Offer created and sent: ${sdp.description}")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "Local description set successfully for offer")
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Offer creation failed: $error")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Setting local description failed for offer: $error")
            }
        }, mediaConstraints)
    }

        /**
     * End the call and clean up resources.
     */
    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        audioSource?.dispose()
        videoSource?.dispose()
        Log.d(TAG, "Call ended and resources cleaned up")
    }
}
