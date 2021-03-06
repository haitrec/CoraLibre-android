package de.rki.coronawarnapp.risk

import de.rki.coronawarnapp.server.protocols.ApplicationConfigurationOuterClass
import org.coralibre.android.sdk.fakegms.nearby.exposurenotification.ExposureSummary
import timber.log.Timber
import kotlin.math.round

object RiskLevelCalculation {
    private var TAG = RiskLevelCalculation::class.simpleName

    private const val DECIMAL_MULTIPLIER = 100

    fun calculateRiskScore(
        attenuationParameters: ApplicationConfigurationOuterClass.AttenuationDuration,
        exposureSummary: ExposureSummary
    ): Double {

        /** all attenuation values are capped to [TimeVariables.MAX_ATTENUATION_DURATION] */
        val weightedAttenuationLow =
            attenuationParameters.weights.low
                .times(exposureSummary.attenuationDurations[0].capped())
        val weightedAttenuationMid =
            attenuationParameters.weights.mid
                .times(exposureSummary.attenuationDurations[1].capped())
        val weightedAttenuationHigh =
            attenuationParameters.weights.high
                .times(exposureSummary.attenuationDurations[2].capped())

        val maximumRiskScore = exposureSummary.maximumRiskScore.toDouble()

        val defaultBucketOffset = attenuationParameters.defaultBucketOffset.toDouble()
        val normalizationDivisor = attenuationParameters.riskScoreNormalizationDivisor.toDouble()

        val attenuationStrings =
            "Weighted Attenuation: ($weightedAttenuationLow + $weightedAttenuationMid + " +
                    "$weightedAttenuationHigh + $defaultBucketOffset)"
        Timber.v(attenuationStrings)

        val weightedAttenuationDuration =
            weightedAttenuationLow
                .plus(weightedAttenuationMid)
                .plus(weightedAttenuationHigh)
                .plus(defaultBucketOffset)

        Timber.v("Formula used: ($maximumRiskScore / $normalizationDivisor) * $weightedAttenuationDuration")

        val riskScore = (maximumRiskScore / normalizationDivisor) * weightedAttenuationDuration

        return round(riskScore.times(DECIMAL_MULTIPLIER)).div(DECIMAL_MULTIPLIER)
    }

    private fun Int.capped(): Int {
        return if (this > TimeVariables.getMaxAttenuationDuration()) {
            TimeVariables.getMaxAttenuationDuration()
        } else {
            this
        }
    }
}
