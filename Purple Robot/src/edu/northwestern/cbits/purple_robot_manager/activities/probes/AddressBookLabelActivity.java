package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.calibration.ContactCalibrationHelper;
import edu.northwestern.cbits.purple_robot_manager.calibration.ContactRecord;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;

public class AddressBookLabelActivity extends AppCompatActivity
{
    private int _clickedIndex = -1;
    private List<ContactRecord> _contacts = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this._contacts = ContactCalibrationHelper.fetchContactRecords(this);

        this.setContentView(R.layout.layout_address_label_activity);
        this.getSupportActionBar().setTitle(R.string.title_address_book_label);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_address_book_label_activity, menu);

        return true;
    }

    @SuppressLint("ValidFragment")
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
            this.finish();
        else if (item.getItemId() == R.id.menu_accept_label)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

            Editor e = prefs.edit();
            e.putLong("last_address_book_calibration", System.currentTimeMillis());
            e.commit();

            this.finish();

            final SanityManager sanity = SanityManager.getInstance(this);
            final String title = this.getString(R.string.title_address_book_label_check);
            sanity.clearAlert(title);

        }

        return true;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        ContactRecord contact = this._contacts.get(this._clickedIndex);

        if ("".equals(contact.name) == false)
            menu.setHeaderTitle(contact.name);
        else
            menu.setHeaderTitle(contact.number);

        String[] groups = this.getResources().getStringArray(R.array.contact_groups);

        for (int i = 0; i < groups.length; i++)
        {
            String group = groups[i];

            menu.add(Menu.NONE, i, i, group);
        }
    }

    public void refresh()
    {
        ListView list = (ListView) this.findViewById(R.id.list_view);

        Parcelable state = list.onSaveInstanceState();

        final AddressBookLabelActivity me = this;

        list.setAdapter(new ArrayAdapter<ContactRecord>(this, R.layout.layout_contact_count_row, this._contacts) {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(me);
                    convertView = inflater.inflate(R.layout.layout_contact_count_row, parent, false);
                }

                TextView contactName = (TextView) convertView.findViewById(R.id.label_contact_name);
                TextView contactNumber = (TextView) convertView.findViewById(R.id.label_contact_number);
                TextView contactType = (TextView) convertView.findViewById(R.id.label_contact_type);

                ContactRecord contact = me._contacts.get(position);

                if ("".equals(contact.name) == false)
                    contactName.setText(contact.name);
                else
                    contactName.setText(contact.number);

                contactName.setText(contactName.getText() + " (" + contact.count + ")");

                contactNumber.setText(contact.number);

                if (contact.group != null)
                    contactType.setText(contact.group);
                else
                    contactType.setText(R.string.contact_group_label_unknown);

                return convertView;
            }
        });

        list.onRestoreInstanceState(state);
    }

    public void onResume()
    {
        super.onResume();

        final AddressBookLabelActivity me = this;

        ListView list = (ListView) this.findViewById(R.id.list_view);
        this.registerForContextMenu(list);

        this.refresh();

        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                me._clickedIndex = position;

                parent.showContextMenu();
            }
        });
    }

    protected void onPause()
    {
        super.onPause();

        ContactCalibrationHelper.check(this);
        SanityManager.getInstance(this).refreshState();
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        ContactRecord contact = this._contacts.get(this._clickedIndex);
        contact.group = item.getTitle().toString();

        String key = contact.name;

        if ("".equals(key))
            key = contact.number;

        ContactCalibrationHelper.setGroup(this, key, contact.group);

        this.refresh();

        return true;
    }
}
