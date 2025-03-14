import android.util.Log
import com.example.qchat.webrtc.WebRtcManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.webrtc.*

class SignalingClient(
    private val userId: String,
    private val remoteUserId: String
) {
    private val db = FirebaseFirestore.getInstance()
    private var callListener: ListenerRegistration? = null
    private var webRtcManager: WebRtcManager? = null

    init {
        listenForRemoteSignaling()
    }

    fun setWebRtcManager(manager: WebRtcManager) {
        webRtcManager = manager
    }

    private fun listenForRemoteSignaling() {
        callListener = db.collection("calls")
            .document(userId)
            .collection("signals")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("SignalingClient", "Error listening for signals: ${e.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { doc ->
                    when (doc.document.getString("type")) {
                        "offer" -> {
                            val sdp = doc.document.getString("sdp")
                            sdp?.let {
                                val description = SessionDescription(SessionDescription.Type.OFFER, it)
                                webRtcManager?.handleRemoteOffer(description)
                                Log.d("SignalingClient", "Received SDP offer: $it")
                            }
                        }
                        "answer" -> {
                            val sdp = doc.document.getString("sdp")
                            sdp?.let {
                                val description = SessionDescription(SessionDescription.Type.ANSWER, it)
                                webRtcManager?.handleAnswer(description)
                                Log.d("SignalingClient", "Received SDP answer: $it")
                            }
                        }
                        "candidate" -> {
                            val sdpMid = doc.document.getString("sdpMid")
                            val sdpMLineIndex = doc.document.getLong("sdpMLineIndex")?.toInt()
                            val candidate = doc.document.getString("candidate")
                            if (sdpMid != null && sdpMLineIndex != null && candidate != null) {
                                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                                webRtcManager?.handleIceCandidate(iceCandidate)
                                Log.d("SignalingClient", "Received ICE candidate: $iceCandidate")
                            }
                        }
                        else -> Log.e("SignalingClient", "Unknown signal type")
                    }
                }
            }
    }

    fun sendOffer(sdp: SessionDescription) {
        val offer = hashMapOf(
            "type" to "offer",
            "sdp" to sdp.description
        )
        db.collection("calls").document(remoteUserId).collection("signals").add(offer)
    }

    fun sendAnswer(sdp: SessionDescription) {
        val answer = hashMapOf(
            "type" to "answer",
            "sdp" to sdp.description
        )
        db.collection("calls").document(remoteUserId).collection("signals").add(answer)
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val ice = hashMapOf(
            "type" to "candidate",
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )
        db.collection("calls").document(remoteUserId).collection("signals").add(ice)
    }
    fun sendCallState(state: String) {
        val callState = hashMapOf(
            "type" to "callState",
            "state" to state
        )
        db.collection("calls").document(remoteUserId).collection("signals").add(callState)
    }

    fun listenForCallState(
        onRinging: () -> Unit,
        onCallAccepted: () -> Unit,
        onCallRejected: () -> Unit
    ) {
        callListener = db.collection("calls")
            .document(userId)
            .collection("signals")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("SignalingClient", "Error listening for call state: ${e.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { doc ->
                    val type = doc.document.getString("type")
                    val state = doc.document.getString("state")
                    when (type) {
                        "callState" -> {
                            when (state) {
                                "ringing" -> onRinging()
                                "accepted" -> onCallAccepted()
                                "rejected" -> onCallRejected()
                            }
                        }
                    }
                }
            }
    }


    fun cleanup() {
        callListener?.remove()
    }
}
