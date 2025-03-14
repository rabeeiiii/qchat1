package com.example.qchat.webrtc.sessions

import SignalingClient
import com.example.qchat.webrtc.peer.StreamPeerConnectionFactory
import kotlinx.coroutines.flow.SharedFlow
import org.webrtc.*

interface WebRtcSessionManager {

    val signalingClient: SignalingClient

    val peerConnectionFactory: StreamPeerConnectionFactory

    val localVideoTrackFlow: SharedFlow<VideoTrack>

    val remoteVideoTrackFlow: SharedFlow<VideoTrack>

    fun onSessionScreenReady()

    fun flipCamera()

    fun enableMicrophone(enabled: Boolean)

    fun enableCamera(enabled: Boolean)

    fun disconnect()

    fun sendOffer(calleeId: String, offer: SessionDescription)

    fun sendAnswer(callerId: String, answer: SessionDescription)

    fun sendIceCandidate(sessionId: String, iceCandidate: IceCandidate)

    fun listenForIceCandidates(sessionId: String, onIceCandidateReceived: (IceCandidate) -> Unit)

    fun createOffer(onOfferCreated: (SessionDescription) -> Unit)

    fun handleOffer(offerSdp: String, onAnswerCreated: (SessionDescription) -> Unit)

    fun handleAnswer(answerSdp: String)

    fun handleIceCandidate(candidateSdp: String)

    fun createAnswer(onAnswerCreated: (SessionDescription) -> Unit)


}
