package edu.northwestern.cbits.purple_robot_manager;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.JSONReportBuilder;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WearReportSender implements ReportSender
{
    private File _fileDir = null;

    public WearReportSender(File fileDir)
    {
        super();

        this._fileDir = fileDir;
    }

    @Override
    public void send(CrashReportData errorContent) throws ReportSenderException
    {
        try
        {
            Log.e("PW", "LOGGING CRASH: " + errorContent.toJSON().toString(2));
        }
        catch (JSONException | JSONReportBuilder.JSONReportException e)
        {
            e.printStackTrace();
        }

        DataMap report = new DataMap();

        for (ReportField field : errorContent.keySet())
        {
            report.putString(field.name(), errorContent.getProperty(field));
        }

        byte[] contents = report.toByteArray();

        try
        {
            File crashFile = new File(this._fileDir, "crash.acra");
            FileOutputStream out = new FileOutputStream(crashFile);
            out.write(contents);
            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
