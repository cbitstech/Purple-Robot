package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.HashMap;

import android.content.Context;
import android.net.Uri;

public class RegressionModel extends TrainedModel 
{
	public RegressionModel(Context context, Uri uri) 
	{
		super(context, uri);
	}

	protected void generateModel(Context context, String modelString) 
	{
		/*		
		 *	undefined_feature_value_dt_hopelessnessr1 =
		 *	0.171  * networkprobe_hostname=wireless-165-124-136-32nuwlannorthwesternedu,?,192168110,wireless-165-124-137-123nuwlannorthwesternedu,0000 +
		 *	      0.1958 * networkprobe_hostname=?,192168110,wireless-165-124-137-123nuwlannorthwesternedu,0000 +
		 *	      0.1958 * networkprobe_hostname=192168110,wireless-165-124-137-123nuwlannorthwesternedu,0000 +
		 *	      0.1861 * networkprobe_hostname=wireless-165-124-137-123nuwlannorthwesternedu,0000 +
		 *	      0.1521 * networkprobe_hostname=0000 +
		 *	      0.1958 * networkprobe_ip_address=?,0000,192168110,165124137123 +
		 *	      0.1958 * networkprobe_ip_address=0000,192168110,165124137123 +
		 *	      0.1209 * networkprobe_ip_address=165124137123 +
		 *	      0.6011 * robothealthprobe_cpu_usage +
		 *	      0.1209 * runningsoftwareproberunning_tasks_running_tasks_package_name=?,comcbitsmobilyze_pro +
		 *	      0.1209 * runningsoftwareproberunning_tasks_running_tasks_package_name=comcbitsmobilyze_pro +
		 *	     -0.0071 * wifiaccesspointsprobe_access_point_count +
		 *	      0.171  * wifiaccesspointsprobe_current_bssid=00246c0854a8,?,c03f0edd43e6,000000000000 +
		 *	      0.1045 * wifiaccesspointsprobe_current_bssid=?,c03f0edd43e6,000000000000 +
		 *	      0.1045 * wifiaccesspointsprobe_current_bssid=c03f0edd43e6,000000000000 +
		 *	      0.908
		 */
	}
	
	protected Object evaluateModel(Context context, HashMap<String, Object> snapshot) 
	{
		return Double.valueOf(123);
	}
}
