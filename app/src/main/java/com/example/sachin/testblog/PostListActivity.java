package com.example.sachin.testblog;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class PostListActivity extends ListActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

	private ArrayList<String> posts;
    LocationClient mLocationClient;
    Location mCurrentLocation;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mLocationClient = new LocationClient(this, this, this);
		ParseAnalytics.trackAppOpened(getIntent());
		posts = new ArrayList<String>();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, posts);
		setListAdapter(adapter);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_post_list, menu);
		return true;
	}

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
        Toast.makeText(this,"Connection successful",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }


    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    public void onConnected(Bundle bundle)
    {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        updatePostList();
    }

    @Override
    public void onDisconnected()
    {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
    }
    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            AddBlogActivity.ErrorDialogFragment errorFragment = new AddBlogActivity.ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(), "TestBlog");
        }
    }
    /*
 * Handle results returned to the FragmentActivity
 * by Google Play services
 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Decide what to do based on the original request code
        switch (requestCode)
        {

            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode)
                {
                    case Activity.RESULT_OK :
                    /*
                     * Try the request again
                     */
                        updatePostList();
                        break;
                }

        }
    }

	/*
	 * Creating posts and refreshing the list will be controlled from the Action
	 * Bar.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_refresh: {
                updatePostList();
                break;
            }

            case R.id.action_new: {
                newPost();
                break;
            }
            case R.id.logout: {
                logout();
                break;
            }
            case R.id.mypost: {
                updatePostList(ParseUser.getCurrentUser());
                break;
            }
            case R.id.Allpost:{
                updatePostList();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

	private void updatePostList() {
		// Create query for objects of type "Post"
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");

		// Restrict to cases where the author is the current user.
		// Note that you should pass in a ParseUser and not the
		// String reperesentation of that user
        mCurrentLocation = mLocationClient.getLastLocation();
        ParseGeoPoint userLocation =  new ParseGeoPoint(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
        query.whereNear("location", userLocation);
        //query.whereEqualTo("")
		// Run the query


		query.findInBackground( new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    // If there are results, update the list of posts
                    // and notify the adapter
                    posts.clear();
                    for (ParseObject post : parseObjects) {
                        posts.add(post.getString("postContent"));
                    }
                    ((ArrayAdapter<String>) getListAdapter())
                            .notifyDataSetChanged();
                } else {
                    Log.d("Post retrieval", "Error: " + e.getMessage());
                }
            }
        });

	}
    private void updatePostList(ParseUser user)
    {
        // Create query for objects of type "Post"
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");

        // Restrict to cases where the author is the current user.
        // Note that you should pass in a ParseUser and not the
        // String reperesentation of that user
        query.whereEqualTo("user", user);
        //query.whereEqualTo("")
        // Run the query


        query.findInBackground( new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    // If there are results, update the list of posts
                    // and notify the adapter
                    posts.clear();
                    for (ParseObject post : parseObjects) {
                        posts.add(post.getString("postContent"));
                    }
                    ((ArrayAdapter<String>) getListAdapter())
                            .notifyDataSetChanged();
                } else {
                    Log.d("Post retrieval", "Error: " + e.getMessage());
                }
            }
        });

    }

	private void newPost() {
		Intent i = new Intent(this, AddBlogActivity.class);
		startActivityForResult(i, 0);
	}



    private void logout()
    {
        ParseUser.logOut();
        if(ParseUser.getCurrentUser()==null)
        {
            Toast.makeText(getApplicationContext(),"Logout successful",Toast.LENGTH_SHORT).show();
            Intent in =  new Intent(getBaseContext(),Login.class);
            in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            in.putExtra("EXIT", true);
            startActivity(in);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                this.finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void closeApp()
    {
        Intent intent = new Intent(getBaseContext(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
    }
}
