package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.Closeable
import java.util.Locale

/**
 * Pure gate between navigation state emissions and one spoken utterance per newly issued cue key.
 */
internal class VexNavigationCueSpeechGate {
    private var lastSpoken:
        VexNavigationGuidanceCueKey? = null

    fun nextCue(
        state: VexNavigationSessionState,
    ): VexNavigationGuidanceCue? {
        if (
            state.phase !=
            VexNavigationSessionPhase.ACTIVE
        ) {
            return null
        }

        val cue =
            state
                .convergence
                ?.latestIssuedCue
                ?: return null

        if (cue.key == lastSpoken) {
            return null
        }

        lastSpoken =
            cue.key

        return cue.deepCopy()
    }
}

/**
 * Android-native speech realization for deterministic navigation cues.
 *
 * Navigation meaning and timing are already decided before this class is called. TTS failure never
 * changes route progress. Audio focus is short-lived and marked as navigation guidance so Android
 * can duck compatible media without VexMaps reaching into another feature's player.
 */
internal class VexNavigationSpeech(
    context: Context,
) : Closeable {
    private val appContext =
        context.applicationContext

    private val audioManager =
        requireNotNull(
            appContext.getSystemService(
                AudioManager::class.java,
            ),
        )

    private val audioAttributes =
        AudioAttributes.Builder()
            .setUsage(
                AudioAttributes
                    .USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            )
            .setContentType(
                AudioAttributes
                    .CONTENT_TYPE_SPEECH,
            )
            .build()

    private val focusListener =
        AudioManager
            .OnAudioFocusChangeListener {
                // Each utterance is short and independently eligible. A focus loss simply means
                // the current TTS output should not fight another higher-priority audio owner.
            }

    private val focusRequest:
        AudioFocusRequest? =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            AudioFocusRequest.Builder(
                AudioManager
                    .AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
                .setAudioAttributes(
                    audioAttributes,
                )
                .setOnAudioFocusChangeListener(
                    focusListener,
                )
                .build()
        } else {
            null
        }

    private var ttsReady = false
    private var closed = false
    private var pendingCue:
        VexNavigationGuidanceCue? = null

    private val tts =
        TextToSpeech(
            appContext,
        ) { status ->
            if (
                closed ||
                status !=
                TextToSpeech.SUCCESS
            ) {
                return@TextToSpeech
            }

            ttsReady = true

            runCatching {
                tts.language =
                    Locale.getDefault()
                tts.setAudioAttributes(
                    audioAttributes,
                )
            }

            pendingCue
                ?.also {
                    pendingCue = null
                }
                ?.let(::speak)
        }.apply {
            setOnUtteranceProgressListener(
                object :
                    UtteranceProgressListener() {
                    override fun onStart(
                        utteranceId: String?,
                    ) = Unit

                    override fun onDone(
                        utteranceId: String?,
                    ) {
                        abandonAudioFocus()
                    }

                    @Deprecated(
                        "Legacy TextToSpeech callback",
                    )
                    override fun onError(
                        utteranceId: String?,
                    ) {
                        abandonAudioFocus()
                    }

                    override fun onError(
                        utteranceId: String?,
                        errorCode: Int,
                    ) {
                        abandonAudioFocus()
                    }

                    override fun onStop(
                        utteranceId: String?,
                        interrupted: Boolean,
                    ) {
                        abandonAudioFocus()
                    }
                },
            )
        }

    fun speak(
        cue: VexNavigationGuidanceCue,
    ) {
        if (closed) {
            return
        }

        if (!ttsReady) {
            pendingCue =
                cue.deepCopy()
            return
        }

        if (!requestAudioFocus()) {
            return
        }

        val result =
            tts.speak(
                cue.instructionProjection,
                TextToSpeech.QUEUE_FLUSH,
                null,
                cue.key.utteranceId,
            )

        if (
            result ==
            TextToSpeech.ERROR
        ) {
            abandonAudioFocus()
        }
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        pendingCue = null
        tts.stop()
        tts.shutdown()
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        val result =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                audioManager.requestAudioFocus(
                    requireNotNull(
                        focusRequest,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager
                        .AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                )
            }

        return result ==
            AudioManager
                .AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            focusRequest?.let {
                audioManager
                    .abandonAudioFocusRequest(
                        it,
                    )
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(
                focusListener,
            )
        }
    }
}

private val VexNavigationGuidanceCueKey
    .utteranceId: String
    get() =
        buildString {
            append(routeRef)
            append(':')
            append(routeRevision)
            append(':')
            append(maneuverRef)
            append(':')
            append(cueBand.name)
        }

// [VXG RealForever]
