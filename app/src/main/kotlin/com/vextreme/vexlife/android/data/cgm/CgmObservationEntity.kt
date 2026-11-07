package com.vextreme.vexlife.android.data.cgm

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vextreme.vexlife.android.cgm.CgmObservation
import com.vextreme.vexlife.android.cgm.CgmPartner

/**
 * Physical Room representation of one already-admitted historical CGM Partner Observation.
 *
 * The row preserves both statement identity (source/epoch/sequence/kind) and encounter provenance
 * (Connection id / observed-at). Restoring this row reconstructs historical values; it never invokes
 * Partner Decoder/Receiver and therefore is not a new Partner receipt.
 */
@Entity(
    tableName =
        "cgm_partner_observation",
    indices = [
        Index(
            value = [
                "source_device_ref",
                "source_acquisition_epoch",
                "signal_sequence",
            ],
        ),
        Index(
            value = [
                "observed_at_epoch_millis",
            ],
        ),
    ],
)
internal data class CgmObservationEntity(
    @PrimaryKey(
        autoGenerate =
            true,
    )
    @ColumnInfo(
        name =
            "database_id",
    )
    val databaseId: Long =
        0L,

    @ColumnInfo(
        name =
            "connection_ref",
    )
    val connectionRef: String,

    @ColumnInfo(
        name =
            "observed_at_epoch_millis",
    )
    val observedAtEpochMillis: Long,

    @ColumnInfo(
        name =
            "signal_kind",
    )
    val signalKind: String,

    @ColumnInfo(
        name =
            "source_device_ref",
    )
    val sourceDeviceRef: String,

    @ColumnInfo(
        name =
            "source_acquisition_epoch",
    )
    val sourceAcquisitionEpoch: String,

    @ColumnInfo(
        name =
            "signal_sequence",
    )
    val signalSequence: Long,

    @ColumnInfo(
        name =
            "captured_at_epoch_millis",
    )
    val capturedAtEpochMillis: Long,

    @ColumnInfo(
        name =
            "glucose_mg_dl",
    )
    val glucoseMgDl: Int?,

    @ColumnInfo(
        name =
            "glucose_reported_trend",
    )
    val glucoseReportedTrend: String?,

    @ColumnInfo(
        name =
            "temperature_celsius_hundredths",
    )
    val temperatureCelsiusHundredths: Int?,
)

internal fun CgmObservation.toEntity():
    CgmObservationEntity =
    when (
        val signal =
            signal
    ) {
        is CgmPartner.Signal.Glucose ->
            CgmObservationEntity(
                connectionRef =
                    connectionId.value,
                observedAtEpochMillis =
                    observedAtEpochMillis,
                signalKind =
                    "GLUCOSE",
                sourceDeviceRef =
                    signal.source.deviceRef,
                sourceAcquisitionEpoch =
                    signal.source.acquisitionEpoch,
                signalSequence =
                    signal.sequence,
                capturedAtEpochMillis =
                    signal.capturedAtEpochMillis,
                glucoseMgDl =
                    signal.mgDl,
                glucoseReportedTrend =
                    signal.reportedTrend.name,
                temperatureCelsiusHundredths =
                    null,
            )

        is CgmPartner.Signal.Temperature ->
            CgmObservationEntity(
                connectionRef =
                    connectionId.value,
                observedAtEpochMillis =
                    observedAtEpochMillis,
                signalKind =
                    "TEMPERATURE",
                sourceDeviceRef =
                    signal.source.deviceRef,
                sourceAcquisitionEpoch =
                    signal.source.acquisitionEpoch,
                signalSequence =
                    signal.sequence,
                capturedAtEpochMillis =
                    signal.capturedAtEpochMillis,
                glucoseMgDl =
                    null,
                glucoseReportedTrend =
                    null,
                temperatureCelsiusHundredths =
                    signal.celsiusHundredths,
            )
    }

internal fun CgmObservationEntity.toObservation():
    CgmPartner.Connection.Observation {
    val source =
        CgmPartner.Source(
            deviceRef =
                sourceDeviceRef,
            acquisitionEpoch =
                sourceAcquisitionEpoch,
        )

    val signal =
        when (
            signalKind
        ) {
            "GLUCOSE" ->
                CgmPartner.Signal.Glucose(
                    source =
                        source,
                    sequence =
                        signalSequence,
                    capturedAtEpochMillis =
                        capturedAtEpochMillis,
                    mgDl =
                        requireNotNull(
                            glucoseMgDl,
                        ) {
                            "GLUCOSE row requires glucose_mg_dl"
                        },
                    reportedTrend =
                        CgmPartner.Trend.valueOf(
                            requireNotNull(
                                glucoseReportedTrend,
                            ) {
                                "GLUCOSE row requires glucose_reported_trend"
                            },
                        ),
                )

            "TEMPERATURE" ->
                CgmPartner.Signal.Temperature(
                    source =
                        source,
                    sequence =
                        signalSequence,
                    capturedAtEpochMillis =
                        capturedAtEpochMillis,
                    celsiusHundredths =
                        requireNotNull(
                            temperatureCelsiusHundredths,
                        ) {
                            "TEMPERATURE row requires temperature_celsius_hundredths"
                        },
                )

            else ->
                error(
                    "unknown durable CGM signal kind: $signalKind",
                )
        }

    return CgmPartner.Connection.Observation(
        connectionId =
            CgmPartner.Connection.Id(
                value =
                    connectionRef,
            ),
        signal =
            signal,
        observedAtEpochMillis =
            observedAtEpochMillis,
    )
}

// [VXG RealForever][VEX-CGM-LABS][ROOM-OBSERVATION]
