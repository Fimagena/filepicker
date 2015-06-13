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

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.BitSet;
import java.util.List;


public class FilePickerFragment extends Fragment implements NewFolderFragment.OnNewFolderListener,
                                                            FileItemAdapter.FileListListener {
    public static final int SELECT_FILE = 0;
    public static final int SELECT_DIR = 1;
    public static final int ALLOW_CREATE_DIR = 2;
    public static final int ALLOW_MULTIPLE_SELECT = 3;
    private File mStartPath = null;
    private BitSet mParams = null;
    public BitSet getParams() {return mParams;}

    protected static final String KEY_CURRENT_PATH = "KEY_CURRENT_PATH";
    protected static final String KEY_PARAMS = "KEY_PARAMS";

    private TextView mCurrentDirBar = null;
    private File[] mFileSystemRoots = null;

    private FileItemAdapter mAdapter = null;
    public List<File> getCheckedFiles() {return mAdapter.getCheckedFiles();}
    public boolean hasChecked() {return mAdapter.hasChecked();}
    public void clearSelection() {mAdapter.clearSelection();}
    public File getCurrentPath() {return mAdapter.getCurrentPath();}

    public interface SelectionListener {void onHasCheckedChange(boolean pickedSomething);}
    private SelectionListener mListener = null;
    public void registerSelectionListener(SelectionListener listener) {mListener = listener;}


    @Override public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_PARAMS)) {
            mParams = (BitSet) savedInstanceState.getSerializable(KEY_PARAMS);
            mStartPath = new File(savedInstanceState.getString(KEY_CURRENT_PATH));
        }
        else {
            // read xml custom-attributes
            TypedArray attrArray = activity.obtainStyledAttributes(attrs, R.styleable.FilePickerFragment);

            mParams = new BitSet(4);
            mParams.set(SELECT_FILE, attrArray.getBoolean(R.styleable.FilePickerFragment_select_file, false));
            mParams.set(SELECT_DIR, attrArray.getBoolean(R.styleable.FilePickerFragment_select_dir, false));
            mParams.set(ALLOW_CREATE_DIR, attrArray.getBoolean(R.styleable.FilePickerFragment_allow_dir_create, false));
            mParams.set(ALLOW_MULTIPLE_SELECT, attrArray.getBoolean(R.styleable.FilePickerFragment_allow_multiple, false));
            if (!mParams.get(SELECT_FILE) && !mParams.get(SELECT_DIR))
                mParams.set(SELECT_FILE, true);

            if (attrArray.hasValue(R.styleable.FilePickerFragment_start_path))
                mStartPath = new File(attrArray.getText(R.styleable.FilePickerFragment_start_path).toString());
            else mStartPath = Environment.getExternalStorageDirectory();

            attrArray.recycle();
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        File mnt = new File("/storage");
        if (!mnt.exists()) mnt = new File("/mnt");
        mFileSystemRoots = mnt.listFiles(new FileFilter() {
            @Override public boolean accept(File f) {
                try {
                    File canon = (f.getParent() == null) ? f : new File(f.getParentFile().getCanonicalFile(), f.getName());
                    boolean isSymlink = !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
                    return f.isDirectory() && f.exists() && f.canWrite() && !f.isHidden() && !isSymlink;
                }
                catch (Exception e) {return false;}
            }});

        mAdapter = new FileItemAdapter(this, mParams, mStartPath);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filepicker, container, false);

        mCurrentDirBar = (TextView) view.findViewById(R.id.current_dir);
        if (mAdapter.getCurrentPath() != null) mCurrentDirBar.setText(mAdapter.getCurrentPath().getPath());
        mAdapter.registerListener(this);

        RecyclerView recyclerListView = (RecyclerView)view.findViewById(android.R.id.list);
        recyclerListView.setHasFixedSize(true);
        recyclerListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerListView.setAdapter(mAdapter);

        return view;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.picker_actions, menu);
        MenuItem item = menu.findItem(R.id.action_createdir);
        item.setVisible(mParams.get(ALLOW_CREATE_DIR));

        for (int root = 0; root < mFileSystemRoots.length; root++)
            menu.add(Menu.NONE, root, Menu.NONE, "Switch to " + mFileSystemRoots[root].getName());
    }

    @Override public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.action_createdir) {
            Activity activity = getActivity();
            if (activity instanceof AppCompatActivity)
                NewFolderFragment.showDialog(((AppCompatActivity) activity).getSupportFragmentManager(), this);
            return true;
        }
        if (id == R.id.action_internal) {
            mAdapter.loadDirectory(Environment.getExternalStorageDirectory());
            return true;
        }
        if ((id >= 0) && (id < mFileSystemRoots.length)) {
            mAdapter.loadDirectory(mFileSystemRoots[id].getAbsoluteFile());
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString(KEY_CURRENT_PATH, getCurrentPath().getPath());
        b.putSerializable(KEY_PARAMS, mParams);
        // TODO: could save and load checked items, so they survive the fragment being destroyed when memory is low
    }

    @Override public void onNewFolder(final String name) {
        File folder = new File(getCurrentPath(), name);

        if (folder.mkdir()) mAdapter.loadDirectory(folder);
        else Toast.makeText(getActivity(), R.string.create_folder_error, Toast.LENGTH_SHORT).show();
    }

    @Override public void onHasCheckedChange(boolean pickedSomething) {if (mListener != null) mListener.onHasCheckedChange(pickedSomething);}

    @Override public void onDirectoryChange(File directory) {
        mCurrentDirBar.setText(directory.getPath());}

    public void setParams(BitSet params, File startPath) {
        mParams = params;

        if (startPath == null) startPath = Environment.getExternalStorageDirectory();
        mAdapter = new FileItemAdapter(this, mParams, startPath);
        mAdapter.registerListener(this);

        RecyclerView mRecyclerView = (RecyclerView)getView().findViewById(android.R.id.list);
        mRecyclerView.setAdapter(mAdapter);

        getActivity().invalidateOptionsMenu();
    }
}
