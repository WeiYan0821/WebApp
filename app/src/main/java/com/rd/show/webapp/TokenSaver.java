package com.rd.show.webapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TokenSaver {

    private final static String SHARED_PREF_NAME = "net.rouk1.SHARED_PREF_NAME";
    private final static String TOKEN_KEY = "net.rouk1.TOKEN_KEY";

    public static String getToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        Log.i("getToken", "getToken: " + prefs.getString(TOKEN_KEY, ""));
        return prefs.getString(TOKEN_KEY, "");
    }

    public static void setToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

}
