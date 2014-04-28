package com.couchbase.couchtalk_android.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.*;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String TAG = "HelloWorld";
        Log.d(TAG, "Begin Hello World App");

        // create a manager
        Manager manager = null;
        try {
            manager = new Manager(getApplicationContext().getFilesDir(), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
            return;
        }

        // create a name for the database and make sure the name is legal
        String dbname = "hello";
        if (!Manager.isValidDatabaseName(dbname)) {
            Log.e(TAG, "Bad database name");
            return;
        }
        // create a new database
        Database database = null;
        try {
            database = manager.getDatabase(dbname);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get database");
            return;
        }


        // get the current date and time
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        // create an object that contains data for a document
        Map<String, Object> docContent = new HashMap<String, Object>();
        docContent.put("message", "Hello Couchbase Lite");
        docContent.put("creationDate", currentTimeString);
        Log.d(TAG, "docContent=" + String.valueOf(docContent));

        // create and store contents to actual document
        Document document = database.createDocument();
        try {
            document.putProperties(docContent);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write document to database", e);
        }

        // fetch back document
        String docID = document.getId();
        Document retrievedDocument = database.getDocument(docID);
        Log.d(TAG, "retrievedDocument=" + String.valueOf(retrievedDocument.getProperties()));
        Log.d(TAG, "End Hello World App");
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
