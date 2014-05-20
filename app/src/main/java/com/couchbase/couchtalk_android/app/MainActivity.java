package com.couchbase.couchtalk_android.app;

import android.content.res.AssetManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.widget.TextView;

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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.net.URL;
import java.util.Set;


public class MainActivity extends ActionBarActivity {
    protected static final String HOST_URL = "http://sync.couchbasecloud.com/couchtalk";
    protected static final String ITEM_TYPE = "com.couchbase.labs.couchtalk.message-item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String TAG = "CouchTalk";
        Log.i(TAG, "App has started");

        // create a manager
        Manager manager;
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
            return;
        }

        // setup database (copying initial data if needed)
        Database _database;
        try {
            _database = manager.getExistingDatabase("couchtalk");
            if (_database == null) {
                Log.i(TAG, "Database not found, extracting initial dataset.");
                try {
                    AssetManager assets = this.getAssets();
                    InputStream cannedDb = assets.open("couchtalk.cblite");
                    String attsFolder = "couchtalk attachments";
                    HashMap<String, InputStream> cannedAtts = new HashMap<String, InputStream>();
                    for (String attName : assets.list(attsFolder)) {
                        InputStream att = assets.open(String.format("%s/%s", attsFolder, attName));
                        Log.d(TAG, String.format("Loading attachment %s", attName));
                        cannedAtts.put(attName.toLowerCase(), att);
                    }
                    manager.replaceDatabase("couchtalk", cannedDb, cannedAtts);
                } catch (IOException e) {
                    Log.e(TAG, String.format("Couldn't load canned database. %s", e));
                }
                // HACK: intentionally may remain `null` so app crashes instead of silent trouble…
                _database = manager.getExistingDatabase("couchtalk");
            }
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

        final TextView roomDisplay = (TextView) findViewById(R.id.roomList);
        class RoomHandler {
            protected Set<String> channelsUsed = new HashSet<String>();
            protected ArrayList<String> roomNames = new ArrayList<String>();
            public void subscribeToRoom(String room) {
                String channel = String.format("room-%s", room);
                if (!channelsUsed.contains(channel)) {
                    channelsUsed.add(channel);
                    pullReplication.setChannels(new ArrayList<String>(channelsUsed));
                    if (!pullReplication.isRunning()) {
                        pullReplication.start();
                    } else {
                        pullReplication.restart();
                    }
                    Log.i(TAG, String.format("Now syncing with %s", pullReplication.getChannels()));

                    roomNames.add(room);
                    StringBuilder roomJoiner = new StringBuilder();
                    boolean first = true;
                    for (String s : roomNames) {
                        if (!first) roomJoiner.append(", ");
                        else first = false;
                        roomJoiner.append(s);
                    }
                    roomDisplay.setText("Syncing rooms:\n"+roomJoiner.toString());
                }
            }
        }
        final RoomHandler roomHandler = new RoomHandler();
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                for (DocumentChange change : event.getChanges()) {
                    if (change.getSourceUrl() != null) continue;
                    final Map<String,Object> doc = database.getExistingDocument(change.getDocumentId()).getProperties();
                    if (ITEM_TYPE.equals(doc.get("type"))) runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            roomHandler.subscribeToRoom((String)doc.get("room"));
                        }
                    });
                }
            }
        });
        roomHandler.subscribeToRoom("howto");

        Redirector redirector = new Redirector();
        int redirectPort = redirector.getListenPort();
        new Thread(redirector).start();

        WifiManager wifiManager = (WifiManager) this.getSystemService(this.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        String helperText = String.format("http://%s:%d — %s", ipAddress, redirectPort, wifiInfo.getSSID());
        Log.i(TAG, String.format("WiFi is %s", helperText));
        TextView urlDisplay = (TextView) findViewById(R.id.urlDisplay);
        urlDisplay.setText(helperText);

        LiteListener listener = new LiteListener(manager, 59840, null);
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
