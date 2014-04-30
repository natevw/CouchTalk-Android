package com.couchbase.couchtalk_android.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import com.couchbase.lite.ReplicationFilter;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Database;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.net.URL;
import java.util.Set;


public class MainActivity extends ActionBarActivity {
    protected static final String HOST_URL = "http://sync.couchbasecloud.com/couchtalk-dev2";
    protected static final String ITEM_TYPE = "com.couchbase.labs.couchtalk.message-item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String TAG = "HelloWorld";
        Log.d(TAG, "Begin Hello World App");

        // create a manager
        Manager manager;
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
            return;
        }

        // create a name for the database and make sure the name is legal
        String dbname = "couchtalk";
        if (!Manager.isValidDatabaseName(dbname)) {
            Log.e(TAG, "Bad database name");
            return;
        }
        // create a new database
        Database _database;
        try {
            _database = manager.getDatabase("couchtalk");
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get database");
            return;
        }
        final Database database = _database;

        database.setFilter("app/roomItems", new ReplicationFilter() {
            @Override
            public boolean filter(SavedRevision revision, Map<String, Object> params) {
                Map<String, Object> doc = revision.getProperties();
                //return (ITEM_TYPE.equals(doc.get("type")) && doc.get("room").equals(params.get("room")));
                // WORKAROUND: https://github.com/couchbase/couchbase-lite-android/issues/284
                if (!ITEM_TYPE.equals(doc.get("type"))) return false;
                return (params == null) || doc.get("room").equals(params.get("room"));
            }
        });

        URL centralHost;
        try {
            centralHost = new URL(HOST_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Bad host URL");
            return;
        }

        Replication pushReplication = database.createPushReplication(centralHost);
        pushReplication.setContinuous(true);
        pushReplication.start();

        final Replication pullReplication = database.createPullReplication(centralHost);
        pullReplication.setContinuous(true);
        // don't start until we have rooms to watch

        final Set<String> roomsUsed = new HashSet<String>();
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                for (DocumentChange change : event.getChanges()) {
                    if (change.getSourceUrl() != null) continue;
                    Map<String,Object> doc = database.getExistingDocument(change.getDocumentId()).getProperties();
                    String room = ITEM_TYPE.equals(doc.get("type")) ? String.format("room-%s", doc.get("room")) : null;
                    if (room != null && !roomsUsed.contains(room)) {
                        roomsUsed.add(room);
                        pullReplication.setChannels(new ArrayList<String>(roomsUsed));
                        if (!pullReplication.isRunning()) pullReplication.start();
                        Log.d(TAG, String.format("Now syncing with %s", pullReplication.getChannels()));
                    }
                }
            }
        });

        // TODO: "easy URL" listener, get local WiFi and IP address displayed in UI

        WifiManager wifiManager = (WifiManager) this.getSystemService(this.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        String helperText = String.format("http://%s:%d — %s", ipAddress, 0, wifiInfo.getSSID());
        Log.d(TAG, String.format("WiFi is %s", helperText));

        LiteListener listener = new LiteListener(manager, 59842);
        //int boundPort = listener.getListenPort();
        Thread thread = new Thread(listener);
        thread.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
