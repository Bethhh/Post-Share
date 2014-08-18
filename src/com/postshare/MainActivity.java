package com.postshare;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.postshare.R;
import com.facebook.AppEventsLogger;


public class MainActivity extends FragmentActivity {
	private FBActivity mainFragment;
    private static final int SELECT_IMAGE = 2;
    String selectedPath = "";
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            /*mainFragment = new MainFragment();
            getSupportFragmentManager()
            .beginTransaction()
            .add(android.R.id.content, mainFragment)
            .commit();*/
        } else {
            // Or set the fragment from restored state info
            /*mainFragment = (MainFragment) getSupportFragmentManager()
            .findFragmentById(android.R.id.content);*/
        }
        //210AB929285D383D
        setContentView(R.layout.activity_main);
        
        
        Button c = (Button)findViewById(R.id.button2);
        c.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        Intent intent = new Intent();
		        intent.setType("image/*");
		        intent.setAction(Intent.ACTION_GET_CONTENT);
		        startActivityForResult(Intent.createChooser(intent,"Select Image "), SELECT_IMAGE);	
		        			
			}
		});
        
        

        
        
        Button b = (Button)findViewById(R.id.button1);
        b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EditText status = (EditText)findViewById(R.id.editText1);
				EditText caption = (EditText)findViewById(R.id.caption);
				Intent intent = new Intent(MainActivity.this, FBActivity.class);
				String a = caption.getText().toString();
				String b = status.getText().toString();
				Bundle args = new Bundle();
				args.putString("status", status.getText().toString());
				args.putString("image", selectedPath);
				args.putString("caption", caption.getText().toString());
				//mainFragment.setArguments(args);
				intent.putExtras(args);
                startActivity(intent);

	           /* getSupportFragmentManager()
	            .beginTransaction()
	            .add(android.R.id.content, mainFragment)
	            .commit();*/
				
			}
		});
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
   	 
        if (resultCode == RESULT_OK) {
 
            if (requestCode == SELECT_IMAGE)
            {
                System.out.println("SELECT_IMAGE");
                Uri selectedImageUri = data.getData();
                selectedPath = getPath(selectedImageUri);
                System.out.println("SELECT_Image Path : " + selectedPath);                
            }
 
        }
    }
 
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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
    
	@Override
	public void onResume() {
	    super.onResume();
	    // For scenarios where the main activity is launched and user
	    // session is not null, the session state change notification
	    // may not be triggered. Trigger it if it's open/closed.
	    
	 /*   Session session = Session.getActiveSession();
	    if (session != null &&
	           (session.isOpened() || session.isClosed()) ) {
	        onSessionStateChange(session, session.getState(), null);
	    }*/


        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);

	}


}
