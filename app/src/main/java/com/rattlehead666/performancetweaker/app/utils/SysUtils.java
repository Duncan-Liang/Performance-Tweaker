package com.rattlehead666.performancetweaker.app.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SysUtils implements Constants {

    public static String readOutputFromFile(String pathToFile) {
        StringBuilder buffer = new StringBuilder();
        String data = null;
        Process process;
        BufferedReader stdinput;
        if (Constants.debug) {
            Log.d(App_Tag, "Reading Output from " + pathToFile);
        }
        File file = new File(pathToFile);
        if (!(file.exists())) {
            return "";
        }
        if (file.canRead()) {
            try {
                process = Runtime.getRuntime().exec("cat " + pathToFile);
                stdinput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((data = stdinput.readLine()) != null) {
                    buffer.append(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return buffer.toString();
        }
        /*
         * try reading the file as root
		 */
        else {
            InputStream inputStream;
            DataOutputStream dos;

            try {
                process = prepareRootShell();
                dos = new DataOutputStream(process.getOutputStream());
                dos.writeBytes("cat " + pathToFile);
                dos.flush();
                dos.writeBytes("\n exit ");
                dos.flush();
                dos.close();
                if (process.waitFor() == 0) {
                    inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data = line;
                    }
                }
            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
            }
            return data;
        }
    }

    public static boolean executeRootCommand(ArrayList<String> commands) {
        InputStream is;
        DataOutputStream dos;

        try {
            Process mProcess = prepareRootShell();
            if (mProcess == null) return false;
            dos = new DataOutputStream(mProcess.getOutputStream());
            for (String cmd : commands) {
                dos.writeBytes(cmd);
                dos.flush();
                if (debug) {
                    //   Log.d(Constants.App_Tag, cmd);
                }
            }
            if (mProcess.waitFor() == 0) {
                is = mProcess.getInputStream();
                printOutputOnStdout(is);
                return true;
            } else {
                is = mProcess.getErrorStream();
            }
            dos.close();
            if (is != null) {
                printOutputOnStdout(is);
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String executeCommandWithOutput(String command) {
        DataOutputStream dos;
        InputStream is;
        try {
            Process mProcess = prepareRootShell();
            if (mProcess == null) return "";
            dos = new DataOutputStream(mProcess.getOutputStream());
            dos.writeBytes(command);
            dos.flush();
            dos.writeBytes("exit \n");
            dos.flush();
            if (mProcess.waitFor() == 0) {
                is = mProcess.getInputStream();
                StringBuilder builder = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) builder.append(line);
                return builder.toString();
            }

            dos.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isPropActive(String key) {
        return executeCommandWithOutput("getprop | grep " + key + "\n").split("]:")[1].contains("running");
    }

    private static void printOutputOnStdout(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                Log.e(Constants.App_Tag, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Process prepareRootShell() throws IOException {
        return Runtime.getRuntime().exec(getSUbinaryPath());
    }

    public static String getSUbinaryPath() {
        String path = "/system/bin/su";
        if (new File(path).exists()) {
            return path;
        }
        path = "/system/xbin/su";
        if (new File(path).exists()) {
            return path;
        }
        return null;
    }

    public static String secToString(long tSec) {
        long h = (long) Math.floor(tSec / (60 * 60));
        long m = (long) Math.floor((tSec - h * 60 * 60) / 60);
        long s = tSec % 60;
        String sDur;
        sDur = h + ":";
        if (m < 10) sDur += "0";
        sDur += m + ":";
        if (s < 10) sDur += "0";
        sDur += s;

        return sDur;
    }

    public static String getKernelInfo() {
        String data = readOutputFromFile("/proc/version");
        if (data != null) return data;
        return "";
    }

    public static void mount(boolean writeable, String mountpoint) {
        ArrayList<String> command = new ArrayList<>();
        if (writeable) {
            command.add("mount -o remount,rw " + mountpoint + " " + mountpoint + "\n");
        } else {
            command.add("mount -o remount,ro " + mountpoint + " " + mountpoint + "\n");
        }
        command.add("exit" + "\n");
        executeRootCommand(command);
    }
}