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

    public static boolean isNoiseCancelStarted() {
        return NoiseCancelStarted;
    }

    public static void setNoiseCancelStarted(boolean noiseCancelStarted) {
        NoiseCancelStarted = noiseCancelStarted;
    }
    private static boolean NoiseCancelStarted;
}
