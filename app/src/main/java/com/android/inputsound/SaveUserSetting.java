package com.android.inputsound;

/**
 * Created by Sungjung on 2015-06-23.
 */
public class SaveUserSetting {

    private static double LimitDcb;

    public static double GetLimitDcb() {
        return LimitDcb;
    }

    public static void SetLimitDcb(double dcb)
    {
        LimitDcb = dcb;
        return;
    }

}
