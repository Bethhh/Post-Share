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
    private static final int RC_SELECT_IMAGE = 2;
    private String selectedPath = "";
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        //Upload Button
        Button uploadButton = (Button)findViewById(R.id.upload_btn);
        
        uploadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			    //Chooses image to upload
		        Intent intent = new Intent();
		        intent.setType("image/*");
		        intent.setAction(Intent.ACTION_GET_CONTENT);
		        startActivityForResult(Intent.createChooser(intent,"Select Image "), RC_SELECT_IMAGE);	  			
			}
		});
 
        
        //Post Button    
        Button postButton = (Button)findViewById(R.id.post_btn);
        
        postButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
			    //Gets text of status or photo caption
				EditText statusOrCaption = (EditText)findViewById(R.id.statusBlank);		
				String text = statusOrCaption.getText().toString();
				
				//Adds all information of status/photo to bundle
                Bundle args = new Bundle();
				args.putString("statusOrCaption", text);
				args.putString("image", selectedPath);

				//Passes bundle to next activity
				Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
				intent.putExtras(args);
                startActivity(intent);				
			}
		});
    }
       
    public void onActivityResult(int requestCode, int resultCode, Intent data) { 
        if (resultCode == RESULT_OK) {
            if (requestCode == RC_SELECT_IMAGE) {
                Uri selectedImageUri = data.getData(); 
                selectedPath = getPath(selectedImageUri);
                //System.out.println("SELECT_Image Path : " + selectedPath);                
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

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);
	}
}
