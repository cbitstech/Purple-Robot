package edu.northwestern.cbits.purple_robot_manager;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey="", 
	mode=ReportingInteractionMode.TOAST,
	mailTo="cjkarr@me.com",
	forceCloseDialogAfterToast=false,
	resToastText=R.string.crash_toast_text)
public class PurpleRobotApplication extends Application 
{
    public void onCreate() 
    {
        ACRA.init(this);
        
        super.onCreate();
    }
}
