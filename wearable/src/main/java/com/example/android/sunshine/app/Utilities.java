package com.example.android.sunshine.app;

/**
 * Created by nik on 11/26/2016.
 */

public final class Utilities {
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    public static String getstringforweatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return "Thunderstorm";
        } else if (weatherId >= 300 && weatherId <= 321) {
            return "  Drizzle   ";
        } else if (weatherId >= 500 && weatherId <= 504) {
            return "   Rain     ";
        } else if (weatherId == 511) {
            return "    Snow    ";
        } else if (weatherId >= 520 && weatherId <= 531) {
            return "    Rain    ";
        } else if (weatherId >= 600 && weatherId <= 622) {
            return "    Snow    ";
        } else if (weatherId >= 701 && weatherId <= 761) {
            return "     Foggy  ";
        } else if (weatherId == 761 || weatherId == 781) {
            return "   Storm    ";
        } else if (weatherId == 800) {
            return "   Clear Sky";
        } else if (weatherId == 801) {
            return "Light Clouds";
        } else if (weatherId >= 802 && weatherId <= 804) {
            return "   Cloudy   ";
        }
        return null;
    }

}
