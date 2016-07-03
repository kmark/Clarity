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

package com.versobit.kmark.clarity.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.versobit.kmark.clarity.DbProcessorFragment;
import com.versobit.kmark.clarity.R;

public final class DbProcessorDialog extends DialogFragment
        implements DbProcessorFragment.Callbacks {

    public static final String FRAGMENT_TAG = "fragment_diag_dbproc";
    private static final int REQUEST_WRITE_EXT = 2828;

    private ScrollView logVScroll;
    private CheckBox cbDryRun;
    private CheckBox cbBackups;
    private CheckBox cbReboot;
    private TextView logView;
    private ProgressBar progBar;
    private Button start;
    private DbProcessorFragment worker;

    @Override @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity(), getTheme());

        FragmentManager fm = getFragmentManager();
        worker = (DbProcessorFragment)fm.findFragmentByTag(DbProcessorFragment.FRAGMENT_TAG);

        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_dbprocessor, null);
        final ScrollView rootScroll = (ScrollView)v.findViewById(R.id.dialog_dbproc_rootscroll);
        View diagTitle = v.findViewById(R.id.dialog_dbproc_title);
        cbDryRun = (CheckBox)v.findViewById(R.id.dialog_dbproc_dryrun);
        cbBackups = (CheckBox)v.findViewById(R.id.dialog_dbproc_retain);
        cbReboot = (CheckBox)v.findViewById(R.id.dialog_dbproc_reboot);
        logVScroll = (ScrollView)v.findViewById(R.id.dialog_dbproc_logvscroll);
        logView = (TextView)v.findViewById(R.id.dialog_dbproc_log);
        progBar = (ProgressBar)v.findViewById(R.id.dialog_dbproc_progress);
        start = (Button)v.findViewById(R.id.dialog_dbproc_start);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Show the better looking system dialog title
            adb.setTitle(R.string.dialog_dbproc_title);
            diagTitle.setVisibility(View.GONE);
        }

        cbDryRun.setOnCheckedChangeListener(passiveAgressiveToasts);
        cbBackups.setOnCheckedChangeListener(passiveAgressiveToasts);

        if(worker != null) {
            if(worker.getStatus() == AsyncTask.Status.RUNNING) {
                adb.setCancelable(false);
                uiStart();
            }
            progBar.setProgress(worker.getProgress());
            logView.append(worker.getLog());
            worker.bind();
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_EXT);
                    return;
                }

                // Another check (just in case?)
                if(worker != null && worker.getStatus() == AsyncTask.Status.RUNNING) {
                    return;
                }

                uiStart();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                if(worker != null) {
                    transaction.remove(worker);
                }
                worker = DbProcessorFragment.newInstance(
                        FRAGMENT_TAG, cbDryRun.isChecked(), cbBackups.isChecked(), cbReboot.isChecked()
                );
                transaction.add(worker, DbProcessorFragment.FRAGMENT_TAG);
                transaction.commit();
            }
        });

        return adb.setView(v).create();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start.callOnClick();
                }
        }
    }

    @Override
    public void onDestroy() {
        if(worker != null) {
            worker.unbind();
        }
        super.onDestroy();
    }

    private void uiStart() {
        if(getDialog() != null) {
            getDialog().setCancelable(false);
        }
        start.setEnabled(false);
        cbDryRun.setEnabled(false);
        cbBackups.setEnabled(false);
        cbReboot.setEnabled(false);
        logView.setText("");
    }

    @Override
    public void onProgressUpdate(Integer progress, String msg) {
        if(progress != null) {
            progBar.setProgress(progress);
            start.setText(getString(R.string.dialog_dbproc_execpct, progress / 100));
        }
        if(msg != null) {
            logView.append(msg);
            logVScroll.fullScroll(View.FOCUS_DOWN);
        }
    }

    @Override
    public void onPostExecute() {
        getDialog().setCancelable(true);
        start.setEnabled(true);
        start.setText(R.string.dialog_dbproc_exec);
        cbDryRun.setEnabled(true);
        cbBackups.setEnabled(true);
        cbReboot.setEnabled(true);
    }

    CompoundButton.OnCheckedChangeListener passiveAgressiveToasts = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!isChecked) {
                Toast.makeText(getActivity(), buttonView == cbDryRun ?
                        R.string.dialog_dbproc_toast_dryrun : R.string.dialog_dbproc_toast_backups,
                        Toast.LENGTH_LONG).show();
            }
        }
    };
}
