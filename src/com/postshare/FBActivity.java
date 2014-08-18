package com.postshare;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.postshare.R;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphPlace;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.LoginButton;
import com.facebook.widget.PickerFragment;
import com.facebook.widget.PlacePickerFragment;
import com.facebook.widget.ProfilePictureView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.renn.rennsdk.RennClient;
import com.renn.rennsdk.RennClient.LoginListener;
import com.renn.rennsdk.RennExecutor.CallBack;
import com.renn.rennsdk.RennResponse;
import com.renn.rennsdk.exception.RennException;
import com.renn.rennsdk.param.PutStatusParam;
import com.renn.rennsdk.param.UploadPhotoParam;

public class FBActivity extends ActionBarActivity implements
ConnectionCallbacks, OnConnectionFailedListener {
    private static final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };
    private static final String PERMISSION = "publish_actions";
    private final String PENDING_ACTION_BUNDLE_KEY = "com.postshare:PendingAction";

    private Button postStatusUpdateButton;
    private Button postPhotoButton;
    private Button pickFriendsButton;
    private Button pickPlaceButton;
    private LoginButton loginButton;
    private ProfilePictureView profilePictureView;
    private TextView greeting;
    private PendingAction pendingAction = PendingAction.NONE;
    private ViewGroup controlsContainer;
    private GraphUser user;
    private GraphPlace place;
    private List<GraphUser> tags;
    private boolean canPresentShareDialog;
    private boolean canPresentShareDialogWithPhotos;
    private String API_KEY = "271029";
    private String APP_ID = "22d8d580b4e14bc898daa3ab3f9b676d";
    private String SECRET_KEY = "58baed9ddc4a48e888794b0a3b2523f6";
    private RennClient rennClient;

    private ProgressDialog mProgressDialog;

    
    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    private UiLifecycleHelper uiHelper;
	

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(com.facebook.Session session, SessionState state, Exception exception) {
			// TODO Auto-generated method stub
			try {
				onSessionStateChange(session, state, exception);
			} catch (RennException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            pendingAction = PendingAction.valueOf(name);
        }
        
        setContentView(R.layout.main);
        
        loginButton = (LoginButton) findViewById(R.id.login_button);
        //loginButton.setFragment(this);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                FBActivity.this.user = user;//?
                updateUI();
                // It's possible that we were waiting for this.user to be populated in order to post a
                // status update.
                try {
					handlePendingAction();
				} catch (RennException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
        greeting = (TextView) findViewById(R.id.greeting);

        postStatusUpdateButton = (Button) findViewById(R.id.postStatusUpdateButton);
        postStatusUpdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
					onClickPostStatusUpdate();
				} catch (RennException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        postPhotoButton = (Button) findViewById(R.id.postPhotoButton);
        postPhotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
					onClickPostPhoto();
				} catch (RennException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        pickPlaceButton = (Button) findViewById(R.id.pickPlaceButton);
        pickPlaceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickPlace();
            }
        });

        controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);

        final FragmentManager fm = this.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // If we're being re-created and have a fragment, we need to a) hide the main UI controls and
            // b) hook up its listeners again.
            controlsContainer.setVisibility(View.GONE);
            if (fragment instanceof FriendPickerFragment) {
                setFriendPickerListeners((FriendPickerFragment) fragment);
            } else if (fragment instanceof PlacePickerFragment) {
                setPlacePickerListeners((PlacePickerFragment) fragment);
            }
        }

        // Listen for changes in the back stack so we know if a fragment got popped off because the user
        // clicked the back button.
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() == 0) {
                    // We need to re-show our UI.
                    controlsContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        // Can we present the share dialog for regular links?
        canPresentShareDialog = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG);
        // Can we present the share dialog for photos?
        canPresentShareDialogWithPhotos = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.PHOTOS);	 
	    findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
	    	  public void onClick(View view) {
	    		if (view.getId() == R.id.sign_in_button
	    		  && !mGoogleApiClient.isConnecting()) {
	    		  mSignInClicked = true;
	   			  resolveSignInError();
	   			}
	   		  }
	    });
	    mGoogleApiClient = new GoogleApiClient.Builder(this)
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener(this)
	        .addApi(Plus.API)
	        .addScope(Plus.SCOPE_PLUS_LOGIN)
	        .build();

	    
	    Button shareButton = (Button) findViewById(R.id.share_button);
	    shareButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	          // Launch the Google+ share dialog with attribution to your app.
	          String path = FBActivity.this.getIntent().getExtras().getString("image");
	          Uri uriPath = Uri.fromFile(new File(path));
	          final String message = FBActivity.this.getIntent().getExtras().getString("status");
	          Intent shareIntent = new PlusShare.Builder(getApplicationContext())
	              .setType("image/*")
	              .addStream(uriPath)
	              .setText(message)
	              .getIntent();

	          startActivityForResult(shareIntent, RC_SHARE);
	        }
	    });
	    
	    
	    //renren
	    rennClient = RennClient.getInstance(this);
	    rennClient.init(APP_ID, API_KEY, SECRET_KEY);
	    rennClient.setScope("status_update photo_upload publish_feed");
	    rennClient.setTokenType("bearer"); 
	    
	    Button renrenButton = (Button) findViewById(R.id.renren_signin_button);
	    renrenButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
		   
	        	rennClient.login(FBActivity.this);
	        }
	    });
		rennClient.setLoginListener(new LoginListener() {
			@Override
			public void onLoginSuccess() {
				// TODO Auto-generated method stub
			}
			@Override
			public void onLoginCanceled() {
				// TODO Auto-generated method stub
			}
	
		});
		
		
        
	}

    private void onSessionStateChange(Session session, SessionState state, Exception exception) throws RennException {
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
                new AlertDialog.Builder(this)//?
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
        }
        updateUI();
    }	

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
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

	    uiHelper.onResume();

        updateUI();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
    	if (requestCode == RC_SIGN_IN) {
	    	  if (resultCode != RESULT_OK) {
	    	    mSignInClicked = false;
	    	  }

	    	  mIntentInProgress = false;

	    	  if (!mGoogleApiClient.isConnecting()) {
	    	    mGoogleApiClient.connect();
	    	  }
	    }
	}
	
	
	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}





    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
        }

        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.d("HelloFacebook", "Success!");
        }
    };




	 private void updateUI() {
	    Session session = Session.getActiveSession();
	    boolean enableButtons = (session != null && session.isOpened());

	    
	    postStatusUpdateButton.setEnabled(enableButtons || canPresentShareDialog);
	    postPhotoButton.setEnabled(enableButtons || canPresentShareDialogWithPhotos);
	    pickFriendsButton.setEnabled(enableButtons);
	    pickPlaceButton.setEnabled(enableButtons);
	
	    if (enableButtons && user != null) {
	        profilePictureView.setProfileId(user.getId());
	        greeting.setText(getString(R.string.hello_user, user.getFirstName()));
	    } else {
	        profilePictureView.setProfileId(null);
	        greeting.setText(null);
	    }
	} 

    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() throws RennException {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_PHOTO:
                postPhoto();
                break;
            case POST_STATUS_UPDATE:
                postStatusUpdate();
                break;
        }
    }

    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }

    private void showPublishResult(String message, GraphObject result, FacebookRequestError error) {
        String title = null;
        String alertMessage = null;
        if (error == null) {
            title = getString(R.string.success);
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = getString(R.string.successfully_posted_post, message, id);
        } else {
            title = getString(R.string.error);
            alertMessage = error.getErrorMessage();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void onClickPostStatusUpdate() throws RennException {
        performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
    }

    private FacebookDialog.ShareDialogBuilder createShareDialogBuilderForLink() {
        return new FacebookDialog.ShareDialogBuilder(this)
                .setName("Hello Facebook")
                .setDescription("The 'Hello Facebook' sample application showcases simple Facebook integration")
                .setLink("http://developers.facebook.com/android");
    }

    private void postStatusUpdate() {
        if (canPresentShareDialog) {
            FacebookDialog shareDialog = createShareDialogBuilderForLink().build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else if (user != null && hasPublishPermission()) {
         
            final String message = this.getIntent().getExtras().getString("status");//getString(R.string.status_update, user.getFirstName(), (new Date().toString()));
            Request request = Request
                    .newStatusUpdateRequest(Session.getActiveSession(), message, place, tags, new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            showPublishResult(message, response.getGraphObject(), response.getError());
                        }
                    });
            request.executeAsync();
        } else {
            pendingAction = PendingAction.POST_STATUS_UPDATE;
        }
        //Renren
        PutStatusParam putStatusParam = new PutStatusParam();
        putStatusParam.setContent(this.getIntent().getExtras().getString("status"));
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(FBActivity.this);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setTitle("请等待");
            mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
            mProgressDialog.setMessage("正在发布状态");
            mProgressDialog.show();
        }
        try {
            rennClient.getRennService().sendAsynRequest(putStatusParam, new CallBack() {    
                
                @Override
                public void onSuccess(RennResponse response) {
                    //textView.setText(response.toString());
                    Toast.makeText(FBActivity.this, "状态发布成功", Toast.LENGTH_SHORT).show();  
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }                           
                }
                
                @Override
                public void onFailed(String errorCode, String errorMessage) {
                    //textView.setText(errorCode+":"+errorMessage);
                    Toast.makeText(FBActivity.this, "状态发布失败", Toast.LENGTH_SHORT).show();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }                            
                }
            });
        } catch (RennException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        
        //G+ share
        String message = this.getIntent().getExtras().getString("status");
        
        Intent shareIntent = new PlusShare.Builder(getApplicationContext())
            .setType("text/plain")
            .setText(message)
            .getIntent();

        startActivityForResult(shareIntent, RC_SHARE);
    }

    private void onClickPostPhoto() throws RennException {
        performPublish(PendingAction.POST_PHOTO, canPresentShareDialogWithPhotos);
    }

    private FacebookDialog.PhotoShareDialogBuilder createShareDialogBuilderForPhoto(Bitmap... photos) {
        return new FacebookDialog.PhotoShareDialogBuilder(this)
                .addPhotos(Arrays.asList(photos));
    }

    private void postPhoto() throws RennException {
        Bitmap image = BitmapFactory.decodeFile(this.getIntent().getExtras().getString("image"));//decodeResource(this.getResources(), R.drawable.ic_launcher);
        if (canPresentShareDialogWithPhotos) {
            FacebookDialog shareDialog = createShareDialogBuilderForPhoto(image).build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else if (hasPublishPermission()) {

        	String cap = this.getIntent().getExtras().getString("caption");
        	
            Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    showPublishResult(getString(R.string.photo_post), response.getGraphObject(), response.getError());
                }
            });
            Bundle params = request.getParameters();
            params.putString("name", cap);
            request.setParameters(params);
            request.executeAsync();
        } else {
            pendingAction = PendingAction.POST_PHOTO;
        }
        
        //Renren
        UploadPhotoParam param = new UploadPhotoParam();
        try{
        	 param.setFile(new File(FBActivity.this.getIntent().getExtras().getString("image")));
        	 param.setAlbumId(Long.valueOf("318080934"));
        	 String cap = FBActivity.this.getIntent().getExtras().getString("caption");
        	 if(cap.equals("")) cap = FBActivity.this.getIntent().getExtras().getString("status");
             param.setDescription(cap);
        }catch(Exception e){                	
        }              
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(FBActivity.this);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setTitle("请等待");
            mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
            mProgressDialog.setMessage("正在获取信息");
            mProgressDialog.show();
        }
        try {
            rennClient.getRennService().sendAsynRequest(param, new CallBack() {    

                @Override
                public void onSuccess(RennResponse response) {
                    //textView.setText(response.toString());
                    Toast.makeText(FBActivity.this, "获取成功", Toast.LENGTH_SHORT)
                            .show();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                }

                @Override
                public void onFailed(String errorCode, String errorMessage) {
                    //textView.setText(errorCode + ":" + errorMessage);
                    Toast.makeText(FBActivity.this, "获取失败", Toast.LENGTH_SHORT)
                            .show();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                }
            });
        } catch (RennException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        
        
        //G+ share
        String path = FBActivity.this.getIntent().getExtras().getString("image");
        Uri uriPath = Uri.fromFile(new File(path));
        
        String message = this.getIntent().getExtras().getString("status");
        if(message.equals("")) message = this.getIntent().getExtras().getString("caption");
        
        Intent shareIntent = new PlusShare.Builder(getApplicationContext())
            .setType("image/*")
            .addStream(uriPath)
            .setText(message)
            .getIntent();

        startActivityForResult(shareIntent, RC_SHARE);
        
     
    }

    private void showPickerFragment(PickerFragment<?> fragment) {
        fragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> pickerFragment, FacebookException error) {
                String text = getString(R.string.exception, error.getMessage());
                Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        FragmentManager fm = this.getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        controlsContainer.setVisibility(View.GONE);

        // We want the fragment fully created so we can use it immediately.
        fm.executePendingTransactions();

        fragment.loadData(false);
    }

    private void onClickPickFriends() {
        final FriendPickerFragment fragment = new FriendPickerFragment();

        setFriendPickerListeners(fragment);

        showPickerFragment(fragment);
    }

    private void setFriendPickerListeners(final FriendPickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new FriendPickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onFriendPickerDone(fragment);
            }
        });
    }

    private void onFriendPickerDone(FriendPickerFragment fragment) {
        FragmentManager fm = this.getSupportFragmentManager();
        fm.popBackStack();

        String results = "";

        List<GraphUser> selection = fragment.getSelection();
        tags = selection;
        if (selection != null && selection.size() > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (GraphUser user : selection) {
                names.add(user.getName());
            }
            results = TextUtils.join(", ", names);
        } else {
            results = getString(R.string.no_friends_selected);
        }

        showAlert(getString(R.string.you_picked), results);
    }

    private void onPlacePickerDone(PlacePickerFragment fragment) {
        FragmentManager fm = this.getSupportFragmentManager();
        fm.popBackStack();

        String result = "";

        GraphPlace selection = fragment.getSelection();
        if (selection != null) {
            result = selection.getName();
        } else {
            result = getString(R.string.no_place_selected);
        }

        place = selection;

        showAlert(getString(R.string.you_picked), result);
    }

    private void onClickPickPlace() {
        final PlacePickerFragment fragment = new PlacePickerFragment();
        fragment.setLocation(SEATTLE_LOCATION);
        fragment.setTitleText(getString(R.string.pick_seattle_place));

        setPlacePickerListeners(fragment);

        showPickerFragment(fragment);
    }

    private void setPlacePickerListeners(final PlacePickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new PlacePickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onPlacePickerDone(fragment);
            }
        });
        fragment.setOnSelectionChangedListener(new PlacePickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(PickerFragment<?> pickerFragment) {
                if (fragment.getSelection() != null) {
                    onPlacePickerDone(fragment);
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }

    private void performPublish(PendingAction action, boolean allowNoSession) throws RennException {
        Session session = Session.getActiveSession();
        if (session != null) {
            pendingAction = action;
            if (hasPublishPermission()) {
                // We can do the action right away.
                handlePendingAction();
                return;
            } else if (session.isOpened()) {
                // We need to get new permissions, then complete the action when we get called back.
                session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSION));
                return;
            }
        }

        if (allowNoSession) {
            pendingAction = action;
            handlePendingAction();
        }
    }
    
    
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;
    private static final int RC_SHARE = 1;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;
    /* Track whether the sign-in button has been clicked so that we know to resolve
     * all issues preventing sign-in without waiting.
     */
    private boolean mSignInClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can
     * resolve them when the user clicks sign-in.
     */
    private ConnectionResult mConnectionResult;

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
      if (mConnectionResult.hasResolution()) {
        try {
          mIntentInProgress = true;
          startIntentSenderForResult(mConnectionResult.getResolution().getIntentSender(),
              RC_SIGN_IN, null, 0, 0, 0);
        } catch (SendIntentException e) {
          // The intent was canceled before it was sent.  Return to the default
          // state and attempt to connect to get an updated ConnectionResult.
          mIntentInProgress = false;
          mGoogleApiClient.connect();
        }
      }
    }

    public void onConnectionFailed(ConnectionResult result) {
      if (!mIntentInProgress) {
        // Store the ConnectionResult so that we can use it later when the user clicks
        // 'sign-in'.
        mConnectionResult = result;

        if (mSignInClicked) {
          // The user has already clicked 'sign-in' so we attempt to resolve all
          // errors until the user is signed in, or they cancel.
          resolveSignInError();
        }
      }
    }
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
      }

      protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
      }


      @Override
      public void onConnected(Bundle connectionHint) {
        mSignInClicked = false;
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
      }
      
     
      public void onConnectionSuspended(int cause) {
    	mGoogleApiClient.connect();
      }

    
    
}
