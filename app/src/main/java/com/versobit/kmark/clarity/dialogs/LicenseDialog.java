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

package com.versobit.kmark.clarity.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.webkit.WebView;

import com.versobit.kmark.clarity.R;

public final class LicenseDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_diag_license";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        WebView wv = new WebView(getActivity());
        wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.loadUrl("file:///android_asset/license.html");

        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(wv)
                .setPositiveButton(R.string.dialog_about_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }
}
