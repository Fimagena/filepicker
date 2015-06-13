/*
 * Copyright (c) 2015 Fimagena
 * Based on code with copyright (c) 2015 Jonas Kalderstam
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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.FileListItem>
                             implements LoaderManager.LoaderCallbacks<List<File>> {
    private Fragment mFragment = null;
    private BitSet mParams = null;

    private List<File> mFiles = null;
    private HashSet<Integer> mChecked = null;
    private HashSet<FileListItem> mCheckedListItems = null;

    private FileListListener mListener = null;
    public interface FileListListener {
        void onHasCheckedChange(boolean pickedSomething);
        void onDirectoryChange(File directory);
    }
    public void registerListener(FileListListener listener) {mListener = listener;}


    public FileItemAdapter(Fragment fragment, BitSet params, File startPath) {
        this.mFragment = fragment;
        this.mParams = params;
        mChecked = new HashSet<>();
        mCheckedListItems = new HashSet<>();
        mFiles = new ArrayList<>(1);
        mFiles.add(startPath);

        loadDirectory(startPath);
    }

    public File getCurrentPath() {return mFiles.get(0);}

    public boolean hasChecked() {return !mChecked.isEmpty();}
    public List<File> getCheckedFiles() {
        ArrayList<File> checkedFiles = new ArrayList<>();
        for (int checked : mChecked) checkedFiles.add(mFiles.get(checked));
        return checkedFiles;
    }

    public void clearSelection() {
        boolean hadChecked = hasChecked();

        for (FileListItem listItem : mCheckedListItems) listItem.checkbox.setChecked(false);
        mChecked.clear();
        mCheckedListItems.clear();

        if (hadChecked && (mListener != null)) mListener.onHasCheckedChange(false);
    }

    @Override public FileListItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FileListItem(LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.filepicker_listitem, parent, false));
    }

    @Override public void onBindViewHolder(FileListItem listItem, int position) {
        File file = mFiles.get(position);

        if (position == 0) {
            listItem.icon.setVisibility(View.VISIBLE);
            listItem.text.setText("..");
            listItem.file = file;
            listItem.checkbox.setVisibility(View.GONE);
        }
        else {
            boolean checkable = (file.isDirectory()) ? (mParams.get(FilePickerFragment.SELECT_DIR) && mParams.get(FilePickerFragment.ALLOW_MULTIPLE_SELECT)) :
                                                       (mParams.get(FilePickerFragment.SELECT_FILE));
            listItem.icon.setVisibility(file.isDirectory() ? View.VISIBLE : View.GONE);
            listItem.text.setText(file.getName());
            listItem.file = file;
            listItem.checkbox.setVisibility(checkable ? View.VISIBLE : View.GONE);

            boolean wasChecked = listItem.checkbox.isChecked();
            boolean isChecked = mChecked.contains(position);
            if (wasChecked && !isChecked) mCheckedListItems.remove(listItem);
            if (!wasChecked && isChecked) mCheckedListItems.add(listItem);

            listItem.checkbox.setChecked(isChecked);
        }
    }

    @Override public int getItemCount() {return mFiles.size();}


    protected class FileListItem extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public View icon;
        public TextView text;
        public File file;
        public CheckBox checkbox;

        public FileListItem(View v) {
            super(v);
            icon = v.findViewById(R.id.item_icon);
            text = (TextView) v.findViewById(R.id.filename);
            checkbox = (CheckBox) v.findViewById(R.id.checkbox);

            v.setOnClickListener(this);
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {selectFile(true);}
            });
        }

        @Override public void onClick(View v) {
            if (text.getText().equals("..")) {
                if (file.getParentFile() != null)
                    loadDirectory(file.getParentFile());
            }
            else if (file.isDirectory()) loadDirectory(file);
            else selectFile(false);
        }

        @Override public boolean onLongClick(View v) {selectFile(false); return true;}

        private void selectFile(boolean clickedCheckbox) {
            if (checkbox.getVisibility() == View.GONE) return;

            boolean wasChecked = checkbox.isChecked() ^ clickedCheckbox;
            boolean hadChecked = hasChecked();
            if (!wasChecked && hadChecked && !mParams.get(FilePickerFragment.ALLOW_MULTIPLE_SELECT))
                clearSelection();

            checkbox.setChecked(!wasChecked);
            if (!wasChecked) {
                mChecked.add(getAdapterPosition());
                mCheckedListItems.add(this);
            }
            else {
                mChecked.remove(getAdapterPosition());
                mCheckedListItems.remove(this);
            }

            if ((mListener != null) && ((wasChecked && !hasChecked()) || (!wasChecked && !hadChecked)))
                mListener.onHasCheckedChange(!wasChecked);
        }
    }

    public void loadDirectory(File directory) {
        Bundle args = new Bundle();
        args.putParcelable(FilePickerFragment.KEY_CURRENT_PATH, Uri.fromFile(directory));
        mFragment.getActivity().getSupportLoaderManager().restartLoader(0, args, this);
    }

    @Override public void onLoadFinished(final Loader<List<File>> loader, final List<File> files) {
        clearSelection(); // TODO: maybe we shouldn't clear out selection if the current directory didn't change (i.e., observer change)
        mFiles = files;
        notifyDataSetChanged();
        if (mListener != null) mListener.onDirectoryChange(files.get(0));
    }
    @Override public void onLoaderReset(final Loader<List<File>> loader) {}

    @Override public Loader<List<File>> onCreateLoader(final int id, final Bundle args) {
        return new FolderLoader(mFragment.getActivity(), args);
    }

    private static class FolderLoader extends AsyncTaskLoader<List<File>> {
        FileObserver mFileObserver = null;
        File mCurrentPath = null;

        public FolderLoader(Context context, Bundle args) {
            super(context);
            mCurrentPath = new File(((Uri)args.getParcelable(FilePickerFragment.KEY_CURRENT_PATH)).getPath());
        }

        @Override public List<File> loadInBackground() {
            List<File> filesList;
            File[] filesArray = mCurrentPath.listFiles();

            if (filesArray != null) {
                Arrays.sort(filesArray, new Comparator<File>() {
                    @Override public int compare(File lhs, File rhs) {
                        if (lhs.isDirectory() && !rhs.isDirectory()) return -1;
                        else if (rhs.isDirectory() && !lhs.isDirectory()) return 1;
                        else return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }});
                filesList = new ArrayList<>(Arrays.asList(filesArray));
            }
            else filesList = new ArrayList<>(1);

            filesList.add(0, mCurrentPath);

            return filesList;
        }

        @Override protected void onStartLoading() {
            super.onStartLoading();

            // Start watching for changes
            mFileObserver = new FileObserver(mCurrentPath.getPath(), FileObserver.CREATE | FileObserver.DELETE |
                    FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
                @Override public void onEvent(int event, String path) {onContentChanged();} //Reload
            };
            mFileObserver.startWatching();

            forceLoad();
        }

        @Override protected void onReset() {
            super.onReset();

            if (mFileObserver != null) mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }
}
