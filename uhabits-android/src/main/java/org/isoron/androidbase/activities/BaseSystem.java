/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.androidbase.activities;

import android.content.*;
import android.os.*;
import android.support.annotation.*;
import android.support.v4.content.*;
import android.util.*;
import android.view.*;

import org.isoron.androidbase.utils.*;
import org.isoron.uhabits.*;
import org.isoron.uhabits.utils.*;

import java.io.*;
import java.lang.Process;
import java.util.*;

import javax.inject.*;

/**
 * Base class for all systems class in the application.
 * <p>
 * Classes derived from BaseSystem are responsible for handling events and
 * sending requests to the Android operating system. Examples include capturing
 * a bug report, obtaining device information, or requesting runtime
 * permissions.
 */
@ActivityScope
public class BaseSystem
{
    private Context context;

    @Inject
    public BaseSystem(@ActivityContext Context context)
    {
        this.context = context;
    }

    @Nullable
    public File getFilesDir(@Nullable String relativePath)
    {
        File externalFilesDirs[] = ContextCompat.getExternalFilesDirs(context, null);
        if (externalFilesDirs == null)
        {
            Log.e("BaseSystem", "getFilesDir: getExternalFilesDirs returned null");
            return null;
        }

        return FileUtils.getDir(externalFilesDirs, relativePath);
    }

    /**
     * Captures a bug report and saves it to a file in the SD card.
     * <p>
     * The contents of the file are generated by the method {@link
     * #getBugReport()}. The file is saved in the apps's external private
     * storage.
     *
     * @return the generated file.
     * @throws IOException when I/O errors occur.
     */
    @NonNull
    public File dumpBugReportToFile() throws IOException
    {
        String date =
            DateFormats.getBackupDateFormat().format(DateUtils.getLocalTime());

        if (context == null) throw new RuntimeException(
            "application context should not be null");
        File dir = getFilesDir("Logs");
        if (dir == null) throw new IOException("log dir should not be null");

        File logFile =
            new File(String.format("%s/Log %s.txt", dir.getPath(), date));
        FileWriter output = new FileWriter(logFile);
        output.write(getBugReport());
        output.close();

        return logFile;
    }

    /**
     * Captures and returns a bug report.
     * <p>
     * The bug report contains some device information and the logcat.
     *
     * @return a String containing the bug report.
     * @throws IOException when any I/O error occur.
     */
    @NonNull
    public String getBugReport() throws IOException
    {
        String logcat = getLogcat();
        String deviceInfo = getDeviceInfo();

        String log = "---------- BUG REPORT BEGINS ----------\n";
        log += deviceInfo + "\n" + logcat;
        log += "---------- BUG REPORT ENDS ------------\n";

        return log;
    }

    public String getLogcat() throws IOException
    {
        int maxLineCount = 250;
        StringBuilder builder = new StringBuilder();

        String[] command = new String[]{ "logcat", "-d" };
        Process process = Runtime.getRuntime().exec(command);

        InputStreamReader in = new InputStreamReader(process.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(in);

        LinkedList<String> log = new LinkedList<>();

        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
            log.addLast(line);
            if (log.size() > maxLineCount) log.removeFirst();
        }

        for (String l : log)
        {
            builder.append(l);
            builder.append('\n');
        }

        return builder.toString();
    }

    private String getDeviceInfo()
    {
        if (context == null) return "null context\n";

        WindowManager wm =
            (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        return
            String.format("App Version Name: %s\n", BuildConfig.VERSION_NAME) +
            String.format("App Version Code: %s\n", BuildConfig.VERSION_CODE) +
            String.format("OS Version: %s (%s)\n",
                System.getProperty("os.version"), Build.VERSION.INCREMENTAL) +
            String.format("OS API Level: %s\n", Build.VERSION.SDK) +
            String.format("Device: %s\n", Build.DEVICE) +
            String.format("Model (Product): %s (%s)\n", Build.MODEL,
                Build.PRODUCT) +
            String.format("Manufacturer: %s\n", Build.MANUFACTURER) +
            String.format("Other tags: %s\n", Build.TAGS) +
            String.format("Screen Width: %s\n",
                wm.getDefaultDisplay().getWidth()) +
            String.format("Screen Height: %s\n",
                wm.getDefaultDisplay().getHeight()) +
            String.format("External storage state: %s\n\n",
                Environment.getExternalStorageState());
    }
}
