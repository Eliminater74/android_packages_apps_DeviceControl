/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.namelessrom.devicecontrol.ui.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.configuration.BootupConfiguration;
import org.namelessrom.devicecontrol.objects.BootupItem;
import org.namelessrom.devicecontrol.utils.Utils;

import alexander.martinz.libs.materialpreferences.MaterialEditTextPreference;
import alexander.martinz.libs.materialpreferences.MaterialPreference;

/**
 * Automatically handles reading to files to automatically set the value,
 * writing to files on preference change, even with multiple files,
 * handling bootup restoration.
 */
public class AutoEditTextPreference extends MaterialEditTextPreference {
    private boolean mStartup = true;
    private boolean mMultiFile = false;

    private String mPath;
    private String[] mPaths;

    public AutoEditTextPreference(Context context) {
        super(context);
    }

    public AutoEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override public boolean init(final Context context, final AttributeSet attrs) {
        if (!super.init(context, attrs)) {
            return false;
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AwesomePreference);

        int filePath = -1, filePathList = -1;
        if (a != null) {
            filePath = a.getResourceId(R.styleable.AwesomePreference_filePath, -1);
            filePathList = a.getResourceId(R.styleable.AwesomePreference_filePathList, -1);
            mStartup = a.getBoolean(R.styleable.AwesomePreference_startup, mStartup);
            mMultiFile = a.getBoolean(R.styleable.AwesomePreference_multifile, mMultiFile);
            a.recycle();
        }

        final Resources res = context.getResources();
        if (filePath != -1) {
            mPath = Utils.checkPath(res.getString(filePath));
            mPaths = null;
        } else if (filePathList != -1) {
            mPaths = res.getStringArray(filePathList);
            mPath = Utils.checkPaths(mPaths);
            if (mPath.isEmpty() || !mMultiFile) {
                mPaths = null;
            }
        } else {
            mPath = "";
            mPaths = null;
        }

        handleSelf(true);

        return true;
    }

    public void initValue() {
        if (isSupported()) {
            final String value = Utils.readOneLine(mPath);
            setValue(value);
        }
    }

    public String getPath() { return mPath; }

    public boolean isSupported() {
        return ((mPath != null && !mPath.isEmpty()) || (mPaths != null && mPaths.length != 0));
    }

    public void setPath(String path) {
        path = Utils.checkPath(path);
        if (!TextUtils.isEmpty(path)) {
            mPath = path;
            mPaths = null;
        }
    }

    public void setPaths(String[] paths) {
        String path = Utils.checkPaths(paths);
        if (!TextUtils.isEmpty(path)) {
            mPath = path;
            if (mPath.isEmpty() || !mMultiFile) {
                mPaths = null;
            } else {
                mPaths = paths;
            }
        }
    }

    public void setMultiFile(boolean isMultiFile) {
        mMultiFile = isMultiFile;
    }

    public void setStartup(boolean isStartup) {
        mStartup = isStartup;
    }

    public void writeValue(final String value) {
        if (!isSupported()) {
            return;
        }

        if (mPaths != null && mMultiFile) {
            final int length = mPaths.length;
            for (int i = 0; i < length; i++) {
                Utils.writeValue(mPaths[i], value);
                if (mStartup) {
                    BootupConfiguration.setBootup(getContext(), new BootupItem(
                            "default", getKey() + String.valueOf(i), mPaths[i], value, true));
                }
            }
        } else {
            Utils.writeValue(mPath, value);
            if (mStartup) {
                BootupConfiguration.setBootup(getContext(),
                        new BootupItem("default", getKey(), mPath, value, true));
            }
        }

        postDelayed(new Runnable() {
            @Override public void run() {
                initValue();
            }
        }, 200);
    }

    public void handleSelf(boolean handleSelf) {
        MaterialPreferenceChangeListener listener = null;
        if (handleSelf) {
            listener = new MaterialPreferenceChangeListener() {
                @Override public boolean onPreferenceChanged(MaterialPreference pref, Object o) {
                    writeValue(String.valueOf(o));
                    return true;
                }
            };
        }
        setOnPreferenceChangeListener(listener);
    }

}
