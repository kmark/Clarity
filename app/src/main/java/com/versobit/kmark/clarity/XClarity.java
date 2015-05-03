/*
 * Copyright (C) 2015 Kevin Mark
 *
 * This file is part of Clarity.
 *
 * Clarity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Clarity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Clarity.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.clarity;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XResources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XClarity implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    // Difference between ICS and JB lies in this commit:
    // https://android.googlesource.com/platform/packages/providers/ContactsProvider/+/eae25ef81bfe12946f50c72be9647447bb2a16b5

    private static final String TAG = XClarity.class.getSimpleName();

    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String ACTIVITY_THREAD_CURRENTACTHREAD = "currentActivityThread";
    private static final String ACTIVITY_THREAD_GETSYSCTX = "getSystemContext";

    private static final String CONTACTS_PROVIDER_PKG = "com.android.providers.contacts";
    private static final String MEDIA_PROVIDER_PKG = "com.android.providers.media";

    private static final String PHOTO_PROCESSOR_CLASS = "com.android.providers.contacts.PhotoProcessor";
    private static final String PHOTO_PROCESSOR_THUMBNAIL = "sMaxThumbnailDim";

    private static final String RES_THUMBNAIL = "config_max_thumbnail_photo_dim";
    private static final String RES_ALBUM_THUMBNAIL = "maximum_thumb_size";

    private static final class Config {

        private static final Uri ALL_PREFS_URI = Uri.parse("content://" + SettingsProvider.AUTHORITY + "/all");

        private static int thumbnailDim = 256; // px
        private static int albumThumbDim = 650; // dp
        private static boolean debug = false;

        private static void reload(Context ctx) {
            Cursor prefs = ctx.getContentResolver().query(ALL_PREFS_URI, null, null, null, null);
            if(prefs == null) {
                return;
            }
            while(prefs.moveToNext()) {
                switch (Setting.fromString(prefs.getString(SettingsProvider.QUERY_ALL_KEY))) {
                    case THUMBNAIL_DIM:
                        try {
                            thumbnailDim = Integer.parseInt(prefs.getString(SettingsProvider.QUERY_ALL_VALUE).trim());
                        } catch(NumberFormatException ex) {
                            log("Could not convert '%s' to an integer for %s",
                                    prefs.getString(SettingsProvider.QUERY_ALL_VALUE),
                                    Setting.THUMBNAIL_DIM.toString());
                        }
                        break;
                    case DEBUG:
                        debug = prefs.getInt(SettingsProvider.QUERY_ALL_VALUE) == SettingsProvider.TRUE;
                }
            }
            prefs.close();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if(!CONTACTS_PROVIDER_PKG.equals(lpp.packageName)) {
            return;
        }

        Object activityThread = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass(ACTIVITY_THREAD_CLASS, null), ACTIVITY_THREAD_CURRENTACTHREAD);
        Context systemCtx = (Context)XposedHelpers.callMethod(activityThread, ACTIVITY_THREAD_GETSYSCTX);

        Config.reload(systemCtx);

        // Exit here if on ICS. We've already loaded the config for use in initPkgRes
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        Class PhotoProcessor = XposedHelpers.findClass(PHOTO_PROCESSOR_CLASS, lpp.classLoader);
        XposedHelpers.setStaticIntField(PhotoProcessor, PHOTO_PROCESSOR_THUMBNAIL, Config.thumbnailDim);

        debug("%s set to %d for %s", PHOTO_PROCESSOR_THUMBNAIL, Config.thumbnailDim, lpp.processName);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam pkgRes) throws Throwable {
        if(MEDIA_PROVIDER_PKG.equals(pkgRes.packageName)) {
            try {
                pkgRes.res.setReplacement(MEDIA_PROVIDER_PKG, "dimen", RES_ALBUM_THUMBNAIL,
                        new XResources.DimensionReplacement(Config.albumThumbDim, TypedValue.COMPLEX_UNIT_DIP));
                debug("%s set to %d", RES_ALBUM_THUMBNAIL, Config.albumThumbDim);
            } catch (Resources.NotFoundException ex) {
                // Oh well
            }
            return;
        }

        if(!CONTACTS_PROVIDER_PKG.equals(pkgRes.packageName) ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        // This is completely untested on ICS but /should/ work. Config loading may be hit-or-miss.

        pkgRes.res.setReplacement(CONTACTS_PROVIDER_PKG, "integer", RES_THUMBNAIL, Config.thumbnailDim);
        debug("%s set to %d", RES_THUMBNAIL, Config.thumbnailDim);
    }

    private static void debug(String format, Object... args) {
        if(Config.debug) {
            log(format, args);
        }
    }

    private static void log(String format, Object... args) {
        XposedBridge.log(TAG + ": " + String.format(format, args));
    }
}
