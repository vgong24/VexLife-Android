package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.util.Locale

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
 * The navigation core already decided what the cue means and when it became newly eligible. This
 * layer only realizes that meaning through local TTS and short-lived navigation audio focus.
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
                // Turn cues are short. Android owns the actual focus transition/ducking.
            }

    /*
     * Keep the API-26 AudioFocusRequest out of the loaded field signature on API 23-25. The helper
     * is entered only behind the runtime SDK guard.
     */
    private val platformFocusRequest:
        Any? =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            AudioFocusApi26.create(
                attributes =
                    audioAttributes,
                listener =
                    focusListener,
            )
        } else {
            null
        }

    private var tts:
        TextToSpeech? = null
    private var earlyInitStatus:
        Int? = null
    private var ttsReady = false
    private var closed = false
    private var pendingCue:
        VexNavigationGuidanceCue? = null

    init {
        val created =
            TextToSpeech(
                appContext,
            ) { status ->
                val engine =
                    tts

                if (engine == null) {
                    earlyInitStatus =
                        status
                } else {
                    onTtsInitialized(
                        status = status,
                        engine = engine,
                    )
                }
            }

        created.setOnUtteranceProgressListener(
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

        tts =
            created

        earlyInitStatus
            ?.also {
                earlyInitStatus = null
            }
            ?.let { status ->
                onTtsInitialized(
                    status = status,
                    engine = created,
                )
            }
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

        val engine =
            tts ?: return

        if (!requestAudioFocus()) {
            return
        }

        val result =
            engine.speak(
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
        ttsReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
    }

    private fun onTtsInitialized(
        status: Int,
        engine: TextToSpeech,
    ) {
        if (
            closed ||
            status !=
            TextToSpeech.SUCCESS
        ) {
            return
        }

        ttsReady = true

        runCatching {
            engine.language =
                Locale.getDefault()
            engine.setAudioAttributes(
                audioAttributes,
            )
        }

        pendingCue
            ?.also {
                pendingCue = null
            }
            ?.let(::speak)
    }

    private fun requestAudioFocus(): Boolean {
        val result =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {
                AudioFocusApi26.request(
                    manager = audioManager,
                    request =
                        requireNotNull(
                            platformFocusRequest,
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
            platformFocusRequest?.let {
                AudioFocusApi26.abandon(
                    manager = audioManager,
                    request = it,
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

@RequiresApi(Build.VERSION_CODES.O)
private object AudioFocusApi26 {
    fun create(
        attributes: AudioAttributes,
        listener:
            AudioManager.OnAudioFocusChangeListener,
    ): Any =
        AudioFocusRequest.Builder(
            AudioManager
                .AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        )
            .setAudioAttributes(
                attributes,
            )
            .setOnAudioFocusChangeListener(
                listener,
            )
            .build()

    fun request(
        manager: AudioManager,
        request: Any,
    ): Int =
        manager.requestAudioFocus(
            request as
                AudioFocusRequest,
        )

    fun abandon(
        manager: AudioManager,
        request: Any,
    ) {
        manager.abandonAudioFocusRequest(
            request as
                AudioFocusRequest,
        )
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
