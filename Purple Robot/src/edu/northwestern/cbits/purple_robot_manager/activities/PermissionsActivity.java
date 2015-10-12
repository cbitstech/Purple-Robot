package edu.northwestern.cbits.purple_robot_manager.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class PermissionsActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 1;

    public enum StatusCode {
        ALL_GRANTED, OPTIONAL_MISSING, REQUIRED_MISSING
    }

    private class Permission
    {
        public String name = null;
        public String label = null;
        public boolean enabled = false;
        public boolean required = false;
    }

    public static String status(Context context) {
        List<String> permissions = PermissionsActivity.allPermissions(context);

        int total = permissions.size();
        int granted = 0;
        int missing = 0;

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
                granted += 1;
            else
                missing += 1;
        }

        return context.getString(R.string.status_permissions, total, granted, missing);
    }

    public static StatusCode statusCode(Context context) {
        List<String> permissions = PermissionsActivity.allPermissions(context);

        String[] requiredPermissions = context.getResources().getStringArray(R.array.required_permissions);

        int missing = 0;
        int required = 0;

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missing += 1;

                for (String requiredPermission : requiredPermissions) {
                    if (requiredPermission.equals(permission))
                        required += 1;
                }
            }
        }

        if (required > 0)
            return StatusCode.REQUIRED_MISSING;
        else if (missing > 0)
            return StatusCode.OPTIONAL_MISSING;

        return StatusCode.ALL_GRANTED;
    }

    public static List<String> allPermissions(Context context) {
        ArrayList<String> permissions = new ArrayList<>();

        String[] systemOnly = context.getResources().getStringArray(R.array.permissions_system_only);

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions) {

                    boolean include = true;

                    for (String systemPermission : systemOnly)
                        if (systemPermission.equals(permission))
                            include = false;

                    if (include)
                        permissions.add(permission);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.getInstance(context).logException(e);
        }

        return permissions;
    }

    @Override
    @SuppressLint("SimpleDateFormat")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_permissions);

        this.setContentView(R.layout.layout_permissions_activity);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onResume()
    {
        super.onResume();

        this.refreshList();
    }

    private void refreshList()
    {
        this.getSupportActionBar().setSubtitle(PermissionsActivity.status(this));

        final PermissionsActivity me = this;

        String[] required = this.getResources().getStringArray(R.array.required_permissions);

        ListView list = (ListView) this.findViewById(R.id.list_permissions);
        ArrayList<Permission> permissions = new ArrayList<>();

        for (String permission : PermissionsActivity.allPermissions(this)) {
            Permission p = new Permission();

            p.name = permission;
            p.label = PermissionsActivity.getLabel(this, permission);

            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
                p.enabled = true;
            else
                p.enabled = false;

            for (String requiredPermission : required)
            {
                if (requiredPermission.equals(p.name))
                    p.required = true;
            }

            permissions.add(p);
        }

        Collections.sort(permissions, new Comparator<Permission>() {
            @Override
            public int compare(Permission one, Permission two) {
                if (one.enabled != two.enabled) {
                    if (one.enabled)
                        return 1;

                    return -1;
                }

                if (one.required != two.required) {
                    if (one.required)
                        return -1;

                    return 1;
                }

                return one.label.compareToIgnoreCase(two.label);
            }
        });

        ArrayAdapter<Permission> adapter = new ArrayAdapter<Permission>(this, R.layout.layout_permission_row, permissions)
        {
            @SuppressLint("InflateParams")
            public View getView(final int position, View convertView, ViewGroup parent)
            {
                if (convertView == null)
                {
                    LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    convertView = inflater.inflate(R.layout.layout_permission_row, null);
                }

                final Permission permission = this.getItem(position);

                CheckBox nameField = (CheckBox) convertView.findViewById(R.id.text_permission_name);
                nameField.setText(permission.label);

                nameField.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                        // Do nothing...
                    }
                });

                nameField.setChecked(permission.enabled);
                nameField.setEnabled(permission.enabled != true);

                nameField.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton arg0, boolean isSelected) {
                        ActivityCompat.requestPermissions(me, new String[]{ permission.name }, PermissionsActivity.REQUEST_PERMISSION);

                    }
                });

                TextView requiredLabel = (TextView) convertView.findViewById(R.id.text_permission_required);

                if (permission.required)
                    requiredLabel.setVisibility(View.VISIBLE);
                else
                    requiredLabel.setVisibility(View.GONE);

                final View view = convertView;

                convertView.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View arg0)
                    {
                        CheckBox nameField = (CheckBox) view.findViewById(R.id.text_permission_name);

                        nameField.setChecked(nameField.isChecked() == false);
                    }
                });

                return convertView;
            }
        };

        list.setAdapter(adapter);
    }

    public static String getLabel(Context context, String permission)
    {
        String idName = "permission_" + permission.toLowerCase().replace(".", "_");

        int id = context.getResources().getIdentifier(idName, "string", context.getPackageName());

        if (id != 0)
            return context.getString(id);

        return permission;
    }

    public static String getTitle(Context context, String permission)
    {
        String idName = "permission_title_" + permission.toLowerCase().replace(".", "_");

        int id = context.getResources().getIdentifier(idName, "string", context.getPackageName());

        if (id != 0)
            return context.getString(id);

        return permission;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {

        if (requestCode == PermissionsActivity.REQUEST_PERMISSION)
        {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }

        this.refreshList();
    }

}

