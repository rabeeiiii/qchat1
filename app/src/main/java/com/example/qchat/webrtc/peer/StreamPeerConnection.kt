package com.example.qchat.webrtc.peer

import android.util.Log
import com.example.qchat.webrtc.utils.addRtcIceCandidate
import com.example.qchat.webrtc.utils.createValue
import com.example.qchat.webrtc.utils.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.*

class StreamPeerConnection(
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?
) : PeerConnection.Observer {

    private val TAG = "StreamPeerConnection"
    private val pendingIceMutex = Mutex()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    lateinit var connection: PeerConnection
        private set

    suspend fun createOffer(): Result<SessionDescription> = createValue {
        connection.createOffer(it, mediaConstraints)
    }

    suspend fun createAnswer(): Result<SessionDescription> = createValue {
        connection.createAnswer(it, mediaConstraints)
    }

    suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> = setValue {
        connection.setRemoteDescription(it, sessionDescription)
    }.also {
        pendingIceMutex.withLock {
            pendingIceCandidates.forEach { iceCandidate ->
                connection.addRtcIceCandidate(iceCandidate)
            }
            pendingIceCandidates.clear()
        }
    }

    suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> = setValue {
        connection.setLocalDescription(it, sessionDescription)
    }

    suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        if (connection.remoteDescription == null) {
            pendingIceMutex.withLock { pendingIceCandidates.add(iceCandidate) }
            return Result.failure(Exception("RemoteDescription is not set"))
        }
        return connection.addRtcIceCandidate(iceCandidate)
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate != null) onIceCandidate?.invoke(candidate, type)
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream != null) onStreamAdded?.invoke(stream)
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        mediaStreams?.forEach { stream ->
            onStreamAdded?.invoke(stream)
        }
    }

    override fun onRenegotiationNeeded() {
        onNegotiationNeeded?.invoke(this, type)
    }

    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
    override fun onDataChannel(channel: DataChannel?) {}
    override fun onTrack(transceiver: RtpTransceiver?) {
        onVideoTrack?.invoke(transceiver)
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {}
}
