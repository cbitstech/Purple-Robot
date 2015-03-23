package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.ArrayList;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.NfcProbe;

public class NfcActivity extends ActionBarActivity
{
    private static boolean RUN = false;
    private NfcAdapter _adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LogManager.getInstance(this);
        ModelManager.getInstance(this);

        this.getSupportActionBar().setTitle(R.string.title_nfc_scanner);
        this.getSupportActionBar().setSubtitle(R.string.subtitle_nfc_scanner);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setContentView(R.layout.layout_nfc_activity);
    }

    public static void cancelScan()
    {
        NfcActivity.RUN = false;
    }

    public static void startScan(Context context)
    {
        Intent intent = new Intent(context, NfcActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean canScan(Context context)
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);

        return (adapter != null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        NfcActivity.RUN = true;

        final NfcActivity me = this;

        this._adapter = NfcAdapter.getDefaultAdapter(this);

        if (this._adapter == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_nfc_unavailable);
            builder.setMessage(R.string.message_nfc_unavailable);

            builder.setPositiveButton(R.string.action_close, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    me.finish();
                }
            });

            builder.create().show();
        }
        else
        {
            Intent intent = new Intent(this, NfcActivity.class);

            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            this._adapter.enableForegroundDispatch(this, pi, null, null);
        }

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                while (NfcActivity.RUN)
                {
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e)
                    {
                        LogManager.getInstance(me).logException(e);
                    }
                }

                me.finish();
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (this._adapter != null)
        {
            this._adapter.disableForegroundDispatch(this);
            this._adapter = null;
        }

        NfcActivity.RUN = false;
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        final NfcActivity me = this;

        NdefMessage[] msgs = (NdefMessage[]) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (msgs != null)
        {
            for (NdefMessage msg : msgs)
            {
                Log.e("PR", "TAG MSG: " + msg);

                for (NdefRecord record : msg.getRecords())
                {
                    Log.e("PR", "TAG REC: " + record);
                    Log.e("PR", "TAG REC MIME: " + record.toMimeType());
                    Log.e("PR", "TAG REC URI: " + record.toUri());
                    Log.e("PR", "TAG REC PAYLOAD: " + NfcActivity.formatBytes(record.getPayload()));
                    Log.e("PR", "TAG REC ID: " + NfcActivity.formatBytes(record.getId()));
                }
            }
        }

        ArrayList<String> techList = new ArrayList<String>();

        StringBuffer techs = new StringBuffer();

        for (String tech : tag.getTechList())
        {
            if (techs.length() > 0)
                techs.append("\n");

            techs.append(tech);

            techList.add(tech);
        }

        final TextView statusLabel = (TextView) this.findViewById(R.id.label_status);
        TextView tagIdLabel = (TextView) this.findViewById(R.id.label_tag_id);
        TextView tagTechsLabel = (TextView) this.findViewById(R.id.label_tag_technologies);

        statusLabel.setText(R.string.label_nfc_found);

        final ProgressBar scanner = (ProgressBar) this.findViewById(R.id.scanner);
        scanner.setVisibility(View.GONE);

        final ImageView scannedImage = (ImageView) this.findViewById(R.id.image_scanned);
        scannedImage.setVisibility(View.VISIBLE);

        byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        tagIdLabel.setText(NfcActivity.formatBytes(tagId));

        tagTechsLabel.setText(techs);

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    LogManager.getInstance(me).logException(e);
                }

                me.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        scanner.setVisibility(View.VISIBLE);
                        scannedImage.setVisibility(View.GONE);

                        statusLabel.setText(R.string.label_nfc_looking);
                    }
                });
            }
        };

        Thread t = new Thread(r);
        t.start();

        NfcProbe probe = new NfcProbe();

        Bundle bundle = new Bundle();
        bundle.putString("PROBE", probe.name(this));
        bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
        bundle.putString("TAG_ID", NfcActivity.formatBytes(tagId));

        bundle.putStringArrayList("TECHNOLOGIES", techList);

        UUID uuid = UUID.randomUUID();
        bundle.putString("GUID", uuid.toString());

        LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
        Intent xmitIntent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
        xmitIntent.putExtras(bundle);

        localManager.sendBroadcast(xmitIntent);
    }

    private static String formatBytes(byte[] payload)
    {
        if (payload == null)
            return "null";

        int i, j, in;
        String[] hex =
        { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        String out = "";

        for (j = 0; j < payload.length; ++j)
        {
            in = (int) payload[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }

        return out;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home)
            this.finish();

        return true;
    }
}
