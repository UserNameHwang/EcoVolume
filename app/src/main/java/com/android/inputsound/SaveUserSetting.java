package com.android.inputsound;

/**
 * Created by Sungjung on 2015-06-23.
 */
public class SaveUserSetting {

    public static double GetLimitDcb() {
        return LimitDcb;
    }

    public static void SetLimitDcb(double dcb) {
        LimitDcb = dcb;
        return;
    }
    private static double LimitDcb;

    public static boolean isNoiseAlertStarted() {
        return NoiseAlertStarted;
    }

    public static void setNoiseAlertStarted(boolean volumeAlertStarted) {
        NoiseAlertStarted = volumeAlertStarted;
    }
    private static boolean NoiseAlertStarted;

    public static boolean isTimeAlertStarted() {
        return TimeAlertStarted;
    }

    public static void setTimeAlertStarted(boolean timeAlertStarted) {
        TimeAlertStarted = timeAlertStarted;
    }
    private static boolean TimeAlertStarted;

    public static boolean isEcoVolumeStarted() {
        return EcoVolumeStarted;
    }

    public static void setEcoVolumeStarted(boolean ecoVolumeStarted) {
        EcoVolumeStarted = ecoVolumeStarted;
    }
    private static boolean EcoVolumeStarted;
}
