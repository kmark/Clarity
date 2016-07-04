/*
 * Copyright (C) 2015-2016 Kevin Mark
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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Debug;
import eu.chainfire.libsuperuser.Shell;

public final class DbProcessorFragment extends Fragment {

    public static final String FRAGMENT_TAG = "fragment_dbworker";

    private static final String ARG_REPORTS_TO = "reports_to";
    private static final String ARG_DRY_RUN = "dry_run";
    private static final String ARG_RETAIN_BACKUPS = "retain_backups";
    private static final String ARG_SOFT_REBOOT = "soft_reboot";

    private static final String EXT_DIRECTORY = "Clarity";
    private static final String CONTACTS_PROVIDER_PKG = "com.android.providers.contacts";
    private static final Pattern CHARACTER_BLACKLIST = Pattern.compile(".*[^A-Za-z0-9\\/\\.].*");
    private static final int PROGRESS_MAX = 10000;

    public interface Callbacks {
        void onProgressUpdate(Integer progress, String msg);
        void onPostExecute();
    }

    private Callbacks callback;
    private ProcessorTask task;
    private StringBuilder log = new StringBuilder();
    private int progress = 0;

    public static DbProcessorFragment newInstance(String reportsTo, boolean dryRun, boolean backups, boolean reboot) {
        DbProcessorFragment frag = new DbProcessorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REPORTS_TO, reportsTo);
        args.putBoolean(ARG_DRY_RUN, dryRun);
        args.putBoolean(ARG_RETAIN_BACKUPS, backups);
        args.putBoolean(ARG_SOFT_REBOOT, reboot);
        frag.setArguments(args);
        return frag;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        bind();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        bind();
    }

    public void bind() {
        FragmentManager fm = getFragmentManager();
        if(fm == null) {
            return;
        }
        callback = (Callbacks)fm.findFragmentByTag(getArguments().getString(ARG_REPORTS_TO));
    }

    public void unbind() {
        callback = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        boolean dryRun = getArguments().getBoolean(ARG_DRY_RUN);
        boolean backups = getArguments().getBoolean(ARG_RETAIN_BACKUPS);
        boolean reboot = getArguments().getBoolean(ARG_SOFT_REBOOT);
        task = new ProcessorTask();
        task.execute(dryRun, backups, reboot);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unbind();
    }

    public AsyncTask.Status getStatus() {
        if(task == null) {
            return null;
        }
        return task.getStatus();
    }

    public int getProgress() {
        return progress;
    }

    public String getLog() {
        return log.toString();
    }

    private final class ProcessorTask extends AsyncTask<Object, Object, Object> {

        private final String TAG = ProcessorTask.class.getSimpleName();

        private static final int CMD_RM_CACHE_DB = 1;
        private static final int CMD_RM_CACHE_JOURNAL = 2;
        private static final int CMD_RM_CACHE_IMAGES = 7;
        private static final int CMD_CP_DB_TO_CACHE = 3;
        private static final int CMD_CP_JOURNAL_TO_CACHE = 4;
        private static final int CMD_CP_IMGS_TO_CACHE = 8;
        private static final int CMD_CP_CACHE_TO_DB = 11;
        private static final int CMD_CP_CACHE_TO_JOURNAL = 12;
        private static final int CMD_CHOWN_CACHE_DB = 5;
        private static final int CMD_CHOWN_CACHE_JOURNAL = 6;
        private static final int CMD_CHOWN_CACHE_IMGS_DIR = 9;
        private static final int CMD_CHOWN_CACHE_IMGS_FILES = 10;
        private static final int CMD_CHOWN_DB = 13;
        private static final int CMD_CHOWN_JOURNAL = 14;
        private static final int CMD_GETENFORCE = 15;
        private static final int CMD_SETENFORCE_OFF = 16;
        private static final int CMD_SETENFORCE_ON = 17;

        private Date start = null;
        private File logFile = null;
        private final Activity act = getActivity();
        private final Hashtable<Integer, CommandResult> cmdResults = new Hashtable<>();

        @Override
        protected void onPreExecute() {
            log.setLength(0);
            start = new Date();
            onProgressUpdate(null, act.getString(R.string.frag_dbproc_starting, start));
        }

        @Override
        protected Object doInBackground(Object... params) {
            boolean dryRun = (boolean)params[0];
            boolean backups = (boolean)params[1];
            boolean reboot = (boolean)params[2];
            int internalProg = 0;
            int thumbnailDim = 256;
            String contactsPath = Environment.getDataDirectory().getAbsolutePath() + "/data/" + CONTACTS_PROVIDER_PKG;
            String cachePath = getActivity().getCacheDir().getAbsolutePath();
            File extDir = new File(Environment.getExternalStorageDirectory(), EXT_DIRECTORY);
            File cachedDb = new File(cachePath, "contacts2.db");
            File cachedJournal = new File(cachePath, "contacts2.db-journal");
            File cachedPhotos = new File(cachePath, "photos");
            File backupDir = new File(extDir, act.getString(R.string.frag_dbproc_backupdir, start));
            int appUid;
            int providerUid;
            boolean withJournal = true;
            boolean enforcing = false;
            Debug.setDebug(BuildConfig.DEBUG);
            cmdResults.clear();

            if(backups) {
                publishProgress(backupDir);
            }

            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                thumbnailDim = Integer.parseInt(prefs.getString(Setting.THUMBNAIL_DIM.toString(), "256"));
            } catch (NumberFormatException ex) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_thumbpreferror, thumbnailDim));
            }

            internalProg = (int)(0.02 * PROGRESS_MAX);
            publishProgress(internalProg, act.getString(
                    R.string.frag_dbproc_params, thumbnailDim, dryRun, backups, reboot,
                    cachePath, contactsPath
            ));

            if(!Shell.SU.available()) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_noroot));
                return null;
            }
            internalProg = (int)(0.04 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            Shell.Interactive su = new Shell.Builder().useSU().setWantSTDERR(true).open();
            addCommand(su, "getenforce", CMD_GETENFORCE);
            su.waitForIdle();
            CommandResult getEnforceResult = cmdResults.get(CMD_GETENFORCE);
            if (getEnforceResult.exitCode == 0 && getEnforceResult.output.length > 0) {
                String out = getEnforceResult.output[0].trim().toLowerCase();
                enforcing = out.contains("enforcing") || out.contains("0");
            }

            if (enforcing) {
                publishProgress(internalProg, act.getString(R.string.frag_dbrpoc_selinux));
            }
            internalProg = (int)(0.06 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            if(CHARACTER_BLACKLIST.matcher(cachePath).matches() ||
                    CHARACTER_BLACKLIST.matcher(contactsPath).matches()) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_invalidchars));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            }

            internalProg = (int)(0.08 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            try {
                PackageManager pm = getActivity().getPackageManager();
                appUid = pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0).uid;
                providerUid = pm.getApplicationInfo(CONTACTS_PROVIDER_PKG, 0).uid;
            } catch (PackageManager.NameNotFoundException ex) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_pkg404, ex.getMessage()));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            }
            internalProg = (int)(0.10 * PROGRESS_MAX);
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_founduids, appUid, providerUid));

            if (enforcing) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_selinux_setpermissive));
                addCommand(su, "setenforce 0", CMD_SETENFORCE_OFF);
                su.waitForIdle();
            }

            publishProgress(internalProg, act.getString(R.string.frag_dbproc_cpdbcache));
            addCommand(su, "rm " + cachedDb.getAbsolutePath(), CMD_RM_CACHE_DB);
            addCommand(su, "rm " + cachedJournal.getAbsolutePath(), CMD_RM_CACHE_JOURNAL);
            addCommand(su, "cp " + contactsPath + "/databases/contacts2.db " + cachePath, CMD_CP_DB_TO_CACHE);
            addCommand(su, "cp " + contactsPath + "/databases/contacts2.db-journal " + cachePath, CMD_CP_JOURNAL_TO_CACHE);
            addCommand(su, "chown +" + appUid + ":+" + appUid + " " + cachedDb.getAbsolutePath(), CMD_CHOWN_CACHE_DB);
            addCommand(su, "chown +" + appUid + ":+" + appUid + " " + cachedJournal.getAbsolutePath(), CMD_CHOWN_CACHE_JOURNAL);
            su.waitForIdle();

            if (cmdResults.get(CMD_CP_JOURNAL_TO_CACHE).exitCode != 0) {
                withJournal = false;
                publishProgress(null, act.getString(R.string.frag_dbproc_nojournal));
            }

            internalProg = (int)(0.15 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            if(!cachedDb.exists() || !cachedDb.canRead() || !cachedDb.canWrite()) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_initcpfail,
                        cachedDb.exists(), cachedDb.canRead(), cachedDb.canWrite(),
                        cachedJournal.exists(), cachedJournal.canRead(), cachedJournal.canWrite()));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            }

            internalProg = (int)(0.18 * PROGRESS_MAX);
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_cppicscache));
            addCommand(su, "rm -r " + cachedPhotos.getAbsolutePath(), CMD_RM_CACHE_IMAGES);
            addCommand(su, "cp -R " + contactsPath + "/files/photos " + cachePath, CMD_CP_IMGS_TO_CACHE);
            addCommand(su, "chown  +" + appUid + ":+" + appUid + " " + cachedPhotos.getAbsolutePath(), CMD_CHOWN_CACHE_IMGS_DIR);
            addCommand(su, "chown  +" + appUid + ":+" + appUid + " " + cachedPhotos.getAbsolutePath() + "/*", CMD_CHOWN_CACHE_IMGS_FILES);
            su.waitForIdle();

            internalProg = (int)(0.20 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            if(!cachedPhotos.exists() && !cachedPhotos.canRead() || !cachedPhotos.canExecute()) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_photocpfail,
                        cachedPhotos.exists(), cachedPhotos.canRead(), cachedPhotos.canExecute()));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            }

            internalProg = (int)(0.22 * PROGRESS_MAX);
            publishProgress(internalProg, null);

            if(backups) {
                if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    publishProgress(internalProg,
                            act.getString(R.string.frag_dbproc_backupstateerror, Environment.getExternalStorageState()));
                    setEnforcing(su, enforcing);
                    su.close();
                    return null;
                }
                internalProg = (int)(0.25 * PROGRESS_MAX);
                publishProgress(internalProg,
                        act.getString(R.string.frag_dbproc_dobackup, backupDir.getAbsolutePath()));

                if(!backupDir.exists() && !backupDir.mkdirs()) {
                    publishProgress(internalProg,
                            act.getString(R.string.frag_dbproc_backupmkdirerror, backupDir.getAbsolutePath()));
                    setEnforcing(su, enforcing);
                    su.close();
                    return null;
                } else {
                    try {
                        FileUtils.copyFile(cachedDb, new File(backupDir, "contacts2.db"));
                        if(withJournal) {
                            FileUtils.copyFile(cachedJournal, new File(backupDir, "contacts2.db-journal"));
                        }
                    } catch (IOException ex) {
                        publishProgress(internalProg,
                                act.getString(R.string.frag_dbproc_backupcopyerror, ex.getMessage()));
                        setEnforcing(su, enforcing);
                        su.close();
                        return null;
                    }
                }
            }

            internalProg = (int)(0.30 * PROGRESS_MAX);
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_opendb));
            SQLiteDatabase contactsDb;
            try {
                contactsDb = SQLiteDatabase.openDatabase(cachedDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            } catch (SQLiteException ex) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_opendberror, ex.getMessage()));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            }

            internalProg = PROGRESS_MAX / 3;
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_dbqd15));
            Cursor photoContacts = contactsDb.rawQuery("SELECT d._id, d.data14 FROM data AS d JOIN mimetypes AS m on d.mimetype_id = m._id WHERE m.mimetype = 'vnd.android.cursor.item/photo' AND d.data14 IS NOT NULL AND d.data15 IS NOT NULL", null);
            int totalContacts = photoContacts.getCount();
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_dbqd15total, totalContacts));

            List<Pair<Integer, byte[]>> newThumbnails = new ArrayList<>();

            int i = 0;
            for(photoContacts.moveToFirst(); !photoContacts.isAfterLast(); photoContacts.moveToNext(), i++) {
                int id = photoContacts.getInt(0);
                int photoId = photoContacts.getInt(1);
                File photoFile = new File(cachedPhotos, String.valueOf(photoId));
                internalProg += PROGRESS_MAX / totalContacts / 3;
                publishProgress(internalProg, act.getString(
                        R.string.frag_dbproc_workphoto, i + 1, totalContacts, id, photoId));

                if(!photoFile.exists() || !photoFile.canRead() || !photoFile.isFile()) {
                    publishProgress(internalProg, act.getString(
                            R.string.frag_dbproc_photochkfail, photoFile.exists(),
                            photoFile.canRead(), photoFile.isFile()));
                    continue;
                }

                BitmapFactory.Options inOpts = new BitmapFactory.Options();
                inOpts.inPreferQualityOverSpeed = true;
                inOpts.inScaled = false;
                inOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap original = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), inOpts);
                int srcW = original.getWidth(), srcH = original.getHeight();
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_photosize, srcW, srcH));

                if(srcW != srcH) {
                    publishProgress(internalProg, act.getString(R.string.frag_dbproc_notsquare));
                    original.recycle();
                    continue;
                }

                float scale = (float)thumbnailDim / (float)srcW;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int destW = srcW, destH = srcH;
                if(scale < 1f) {
                    Matrix m = new Matrix();
                    m.postScale(scale, scale);
                    Bitmap thumbnail = Bitmap.createBitmap(original, 0, 0, srcW, srcH, m, true);
                    destW = thumbnail.getWidth();
                    destH = thumbnail.getHeight();
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, output);
                    thumbnail.recycle();
                } else {
                    original.compress(Bitmap.CompressFormat.JPEG, 90, output);
                }
                original.recycle();

                publishProgress(internalProg, act.getString(R.string.frag_dbproc_newthumb, destW,
                        destH, output.size()));

                newThumbnails.add(new Pair<>(id, output.toByteArray()));
                try {
                    output.close();
                } catch (IOException ex) {
                    //
                }
            }

            photoContacts.close();

            internalProg = (int)(2.0 / 3.0 * PROGRESS_MAX);
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_insertphotos,
                    newThumbnails.size(),
                    newThumbnails.size() != 1 ? act.getString(R.string.frag_dbproc_insertphotos_plural) : ""));

            SQLiteStatement update = contactsDb.compileStatement("UPDATE data SET data15 = ? WHERE _id = ?");
            try {
                for (Pair<Integer, byte[]> thumbPair : newThumbnails) {
                    update.bindBlob(1, thumbPair.second);
                    update.bindLong(2, thumbPair.first);
                    update.executeUpdateDelete();
                    update.clearBindings();
                    internalProg += PROGRESS_MAX / newThumbnails.size() / 4;
                    publishProgress(internalProg, null);
                }
            } catch (SQLException ex) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_insertfail, ex.getMessage()));
                setEnforcing(su, enforcing);
                su.close();
                return null;
            } finally {
                update.close();
                contactsDb.close();
            }

            internalProg = (int)(0.95 * PROGRESS_MAX);
            if(dryRun) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_cpbackdryrun));
            } else {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_cpback));
                addCommand(su, "cp " + cachedDb.getAbsolutePath() + " " + contactsPath + "/databases/contacts2.db", CMD_CP_CACHE_TO_DB);
                addCommand(su, "chown +" + providerUid + ":+" + providerUid + " " + contactsPath + "/databases/contacts2.db", CMD_CHOWN_DB);
                if(withJournal) {
                    addCommand(su, "cp " + cachedJournal.getAbsolutePath() + " " + contactsPath + "/databases/contacts2.db-journal", CMD_CP_CACHE_TO_JOURNAL);
                    addCommand(su, "chown +" + providerUid + ":+" + providerUid + " " + contactsPath + "/databases/contacts2.db-journal", CMD_CHOWN_JOURNAL);
                }
                su.waitForIdle();
            }

            internalProg = PROGRESS_MAX;
            publishProgress(internalProg, act.getString(R.string.frag_dbproc_success));

            if(reboot) {
                publishProgress(internalProg, act.getString(R.string.frag_dbproc_softreboot_wait));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                publishProgress(internalProg, act.getString(dryRun ? R.string.frag_dbproc_dryreboot :
                        R.string.frag_dbproc_softreboot_bye));
                setEnforcing(su, enforcing);
                su.close();
                return dryRun ? null : true;
            }

            setEnforcing(su, enforcing);
            su.close();
            return null;
        }

        private void setEnforcing(Shell.Interactive su, boolean enforcing) {
            if (enforcing) {
                publishProgress(null, act.getString(R.string.frag_dbproc_selinux_setenforcing));
                addCommand(su, "setenforce 1", CMD_SETENFORCE_ON);
            }
        }

        private void addCommand(Shell.Interactive su, String command, int code) {
            publishProgress(null, act.getString(R.string.frag_dbproc_shell, code, command));
            su.addCommand(command, code, commandResultListener);
        }

        private final Shell.OnCommandResultListener commandResultListener = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                cmdResults.put(commandCode, new CommandResult(exitCode, output));

                StringBuilder buildLog = new StringBuilder();
                if(output != null) {
                    for(String s : output) {
                        if(s != null && !s.trim().isEmpty()) {
                            buildLog.append(s);
                            buildLog.append('\n');
                        }
                    }
                }
                if(buildLog.length() > 0) {
                    buildLog.deleteCharAt(buildLog.length() - 1);
                    buildLog.insert(0, '\n');
                }
                publishProgress(null, act.getString(R.string.frag_dbproc_shellout, commandCode,
                        exitCode, buildLog.toString()));
            }
        };

        @Override
        protected void onProgressUpdate(Object... objects) {
            if(objects[0] instanceof File) {
                logFile = new File((File)objects[0], "dbprocessor.log");
                return;
            }

            Integer prg = (Integer)objects[0];
            String msg = (String)objects[1];

            if(prg != null) {
                progress = prg;
            }
            if(msg != null) {
                log.append(msg);
                Log.d(TAG, msg);
            }

            if(callback != null) {
                callback.onProgressUpdate(prg, msg);
            }
        }

        @Override
        protected void onPostExecute(Object object) {
            onProgressUpdate(PROGRESS_MAX, act.getString(R.string.frag_dbproc_end, new Date()));

            if(logFile != null && logFile.getParentFile().exists()) {
                try {
                    // I/O on the main thread?! What?!
                    FileUtils.write(logFile, getLog());
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }

            if(object == Boolean.TRUE) {
                new Shell.Builder().useSU()
                        .addCommand("sync; setprop ctl.restart surfaceflinger; setprop ctl.restart zygote")
                        .open();
            }

            if(callback != null) {
                callback.onPostExecute();
            }
        }

        private final class CommandResult {
            private final int exitCode;
            private final String[] output;

            private CommandResult(int exitCode, List<String> output) {
                this.exitCode = exitCode;
                this.output = output.toArray(new String[output.size()]);
            }
        }
    }
}
