/*
 * Copyright (c) 2015 Fimagena
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.fimagena.filepicker;


import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;


public class FilePickerActivity extends AppCompatActivity  {

    protected FilePickerFragment mFilePicker;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filepicker);
        setResult(AppCompatActivity.RESULT_CANCELED);

        mFilePicker = (FilePickerFragment)getSupportFragmentManager().findFragmentById(R.id.file_picker_fragment);

        Intent intent = getIntent();
        if (intent.getStringExtra(Intent.EXTRA_TITLE) != null)
            getSupportActionBar().setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
        if (savedInstanceState == null) {
            File startPath = (intent.getData() != null) ? new File(intent.getData().getPath()) : null;
            if (intent.hasExtra("com.fimagena.filepicker.params"))
                mFilePicker.setParams((BitSet) intent.getSerializableExtra("com.fimagena.filepicker.params"), startPath);
        }

        findViewById(R.id.button_fp_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) {
                setResult(AppCompatActivity.RESULT_CANCELED);
                finish();
            }
        });
        findViewById(R.id.button_fp_ok).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) {
                if ((mFilePicker.getParams().get(FilePickerFragment.ALLOW_MULTIPLE_SELECT) ||
                     !mFilePicker.getParams().get(FilePickerFragment.SELECT_DIR)) && !mFilePicker.hasChecked()) {
                    Toast.makeText(FilePickerActivity.this, R.string.select_something_first, Toast.LENGTH_SHORT).show();
                    return;
                }

                List<File> checkedFiles = mFilePicker.getCheckedFiles();
                if (checkedFiles.isEmpty()) checkedFiles.add(mFilePicker.getCurrentPath());

                ArrayList<Uri> checkedUris = new ArrayList<>(checkedFiles.size());
                for (File file : checkedFiles) checkedUris.add(Uri.fromFile(file));

                Intent results = new Intent();
                results.putParcelableArrayListExtra("com.fimagena.filepicker.checkedFiles", checkedUris);

                setResult(AppCompatActivity.RESULT_OK, results);
                finish();
            }
        });
    }
}
