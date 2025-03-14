package com.example.qchat.webrtc.audio

import android.content.Context
import android.media.AudioManager
import android.util.Log

class AudioSwitch internal constructor(
    context: Context,
    audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener,
    preferredDeviceList: List<Class<out AudioDevice>>,
    private val audioManager: AudioManagerAdapter = AudioManagerAdapterImpl(
        context,
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        audioFocusChangeListener = audioFocusChangeListener
    )
) {

    private val loggerTag = "AudioSwitch"

    private var audioDeviceChangeListener: AudioDeviceChangeListener? = null
    private var selectedDevice: AudioDevice? = null
    private var userSelectedDevice: AudioDevice? = null
    private var wiredHeadsetAvailable = false
    private val mutableAudioDevices = ArrayList<AudioDevice>()
    private val preferredDeviceList: List<Class<out AudioDevice>>

    private var state: State = State.STOPPED

    internal enum class State {
        STARTED, ACTIVATED, STOPPED
    }

    init {
        this.preferredDeviceList = getPreferredDeviceList(preferredDeviceList)
    }

    private fun getPreferredDeviceList(preferredDeviceList: List<Class<out AudioDevice>>): List<Class<out AudioDevice>> {
        require(hasNoDuplicates(preferredDeviceList))
        return if (preferredDeviceList.isEmpty() || preferredDeviceList == defaultPreferredDeviceList) {
            defaultPreferredDeviceList
        } else {
            val result = defaultPreferredDeviceList.toMutableList()
            result.removeAll(preferredDeviceList)
            preferredDeviceList.forEachIndexed { index, device ->
                result.add(index, device)
            }
            result
        }
    }

    private fun <T> hasNoDuplicates(list: List<T>): Boolean {
        return list.groupingBy { it }.eachCount().all { it.value == 1 }
    }

    fun start(listener: AudioDeviceChangeListener) {
        Log.d(loggerTag, "[start] state: $state")
        audioDeviceChangeListener = listener
        when (state) {
            State.STOPPED -> {
                enumerateDevices()
                state = State.STARTED
            }
            else -> {}
        }
    }

    fun stop() {
        Log.d(loggerTag, "[stop] state: $state")
        when (state) {
            State.ACTIVATED -> {
                deactivate()
                closeListeners()
            }
            State.STARTED -> closeListeners()
            State.STOPPED -> {}
        }
    }

    fun activate() {
        Log.d(loggerTag, "[activate] state: $state")
        when (state) {
            State.STARTED -> {
                audioManager.cacheAudioState()
                audioManager.mute(false)
                audioManager.setAudioFocus()
                selectedDevice?.let { activate(it) }
                state = State.ACTIVATED
            }
            State.ACTIVATED -> selectedDevice?.let { activate(it) }
            State.STOPPED -> throw IllegalStateException()
        }
    }

    private fun deactivate() {
        Log.d(loggerTag, "[deactivate] state: $state")
        when (state) {
            State.ACTIVATED -> {
                audioManager.restoreAudioState()
                state = State.STARTED
            }
            State.STARTED, State.STOPPED -> {}
        }
    }

    private fun activate(audioDevice: AudioDevice) {
        Log.d(loggerTag, "[activate] audioDevice: $audioDevice")
        when (audioDevice) {
            is AudioDevice.BluetoothHeadset -> audioManager.enableSpeakerphone(false)
            is AudioDevice.Earpiece, is AudioDevice.WiredHeadset -> audioManager.enableSpeakerphone(false)
            is AudioDevice.Speakerphone -> audioManager.enableSpeakerphone(true)
        }
    }

    internal data class AudioDeviceState(
        val audioDeviceList: List<AudioDevice>,
        val selectedAudioDevice: AudioDevice?
    )

    private fun enumerateDevices(bluetoothHeadsetName: String? = null) {
        Log.d(loggerTag, "[enumerateDevices] bluetoothHeadsetName: $bluetoothHeadsetName")
        val oldAudioDeviceState = AudioDeviceState(mutableAudioDevices.map { it }, selectedDevice)
        addAvailableAudioDevices(bluetoothHeadsetName)

        if (!userSelectedDevicePresent(mutableAudioDevices)) {
            userSelectedDevice = null
        }

        selectedDevice = userSelectedDevice ?: mutableAudioDevices.firstOrNull()
        Log.v(loggerTag, "[enumerateDevices] selectedDevice: $selectedDevice")

        if (state == State.ACTIVATED) activate()
        val newAudioDeviceState = AudioDeviceState(mutableAudioDevices, selectedDevice)
        if (newAudioDeviceState != oldAudioDeviceState) {
            audioDeviceChangeListener?.invoke(mutableAudioDevices, selectedDevice)
        }
    }

    private fun addAvailableAudioDevices(bluetoothHeadsetName: String?) {
        Log.d(loggerTag, "[addAvailableAudioDevices]")
        mutableAudioDevices.clear()
        preferredDeviceList.forEach { audioDevice ->
            when (audioDevice) {
                AudioDevice.BluetoothHeadset::class.java -> {}
                AudioDevice.WiredHeadset::class.java -> {
                    if (wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.WiredHeadset())
                    }
                }
                AudioDevice.Earpiece::class.java -> {
                    if (audioManager.hasEarpiece() && !wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.Earpiece())
                    }
                }
                AudioDevice.Speakerphone::class.java -> {
                    if (audioManager.hasSpeakerphone()) {
                        mutableAudioDevices.add(AudioDevice.Speakerphone())
                    }
                }
            }
        }
    }

    private fun userSelectedDevicePresent(audioDevices: List<AudioDevice>) =
        userSelectedDevice?.let { selectedDevice ->
            if (selectedDevice is AudioDevice.BluetoothHeadset) {
                audioDevices.find { it is AudioDevice.BluetoothHeadset }?.let { newHeadset ->
                    userSelectedDevice = newHeadset
                    true
                } ?: false
            } else audioDevices.contains(selectedDevice)
        } ?: false

    private fun closeListeners() {
        audioDeviceChangeListener = null
        state = State.STOPPED
    }

    companion object {
        private val defaultPreferredDeviceList by lazy {
            listOf(
                AudioDevice.BluetoothHeadset::class.java,
                AudioDevice.WiredHeadset::class.java,
                AudioDevice.Earpiece::class.java,
                AudioDevice.Speakerphone::class.java
            )
        }
    }
}
