package edu.northwestern.cbits.purple_robot_manager;

import android.app.Application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey="", mailTo="crashes@example.com")
public class WatchApplication extends Application
{
    public void onCreate()
    {
        super.onCreate();

        ACRA.init(this);
        ACRA.getErrorReporter().setReportSender(new WearReportSender(this.getFilesDir()));
    }
}
