/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by Peter Kuterna to support Devoxx.
 * List speakers instead of vendors.
 * Support for the Devoxx MySchedule functionality.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.Constants;
import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SessionCounts;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.ui.MyScheduleActivity.MySchedulePrefs;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.SyncUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class StarredActivity extends TabActivity implements AsyncQueryListener {

    private static final String TAG = "StarredActivity";

    public static final String TAG_SESSIONS = "sessions";
    public static final String TAG_SPEAKERS = "speakers";
    
    private static final int EMAIL_SHOW_MYSCHEDULE_REGISTRATION = 0x01;
    private static final int PUBLISH_SHOW_MYSCHEDULE_REGISTRATION = 0x02;

    private NotifyingAsyncQueryHandler mHandler;
    private MyScheduleTask task;

    private View mEmailSeparator;
    private View mEmailButton;
    private View mPublishSeparator;
    private View mPublishButton;

	private SharedPreferences mySchedulePrefs;
    private SharedPreferences settingsPrefs;
    
    private int starredSessionCount = 0;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred);

        ((TextView) findViewById(R.id.title_text)).setText(getTitle());

        mEmailSeparator = findViewById(R.id.email_separator);
        mEmailButton = findViewById(R.id.btn_title_email);
        mPublishSeparator = findViewById(R.id.publish_separator);
        mPublishButton = findViewById(R.id.btn_title_publish);

    	mySchedulePrefs = getSharedPreferences(MyScheduleActivity.MySchedulePrefs.DEVOXXSCHED_MYSCHEDULE, Context.MODE_PRIVATE);
    	settingsPrefs = getSharedPreferences(SettingsActivity.SETTINGS_NAME, MODE_PRIVATE);
    	
    	setupSessionsTab();
        setupSpeakersTab();
        
        getTabHost().setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				onTabChange(tabId);
			}
		});
        
        onTabChange(TAG_SESSIONS);
        
        task = (MyScheduleTask) getLastNonConfigurationInstance();
        if (task != null) {
        	task.attach(this);
        }

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(Sessions.CONTENT_STARRED_URI, SessionsQuery.PROJECTION);
    }

    @Override
	public Object onRetainNonConfigurationInstance() {
    	if (task != null && AsyncTask.Status.RUNNING.equals(task.getStatus())) {
    		task.detach();
    		return task;
    	}
    	return null;
	}

	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.id.dialog_myschedule_email: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.myschedule_email_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.myschedule_email_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new EmailConfirmClickListener())
                        .setCancelable(false)
                        .create();
            }
            case R.id.dialog_myschedule_publish: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.myschedule_publish_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.myschedule_publish_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new PublishConfirmClickListener())
                        .setCancelable(false)
                        .create();
            }
        }
        return null;
    }

    private class EmailConfirmClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
	    	if (checkMySchedulePrefs()) {
	    		emailMySchedule();
	    	} else {
	            final Intent intent = new Intent(StarredActivity.this, MyScheduleActivity.class);
	            startActivityForResult(intent, EMAIL_SHOW_MYSCHEDULE_REGISTRATION);
	    	}
        }
    }

    private class PublishConfirmClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
	    	if (checkMySchedulePrefs()) {
	    		publishMySchedule();
	    	} else {
	            final Intent intent = new Intent(StarredActivity.this, MyScheduleActivity.class);
	            startActivityForResult(intent, PUBLISH_SHOW_MYSCHEDULE_REGISTRATION);
	    	}
        }
    }

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }
    
    public void onEmailClick(View v) {
    	showDialog(R.id.dialog_myschedule_email);
    }
    
    public void onPublishClick(View v) {
    	showDialog(R.id.dialog_myschedule_publish);
    }

    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    		case EMAIL_SHOW_MYSCHEDULE_REGISTRATION:
    			if (resultCode == RESULT_OK && checkMySchedulePrefs())  {
    				emailMySchedule();
    			}
    			break;
    		case PUBLISH_SHOW_MYSCHEDULE_REGISTRATION:
    			if (resultCode == RESULT_OK && checkMySchedulePrefs())  {
    				publishMySchedule();
    			}
    			break;
    	}
	}

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) return;
        	starredSessionCount = cursor.getCount();
        	onTabChange(getTabHost().getCurrentTabTag());
        } finally {
            cursor.close();
        }
    }
    
    private void emailMySchedule() {
    	task = new EmailTask(this);
    	task.execute();
    }

	private void publishMySchedule() {
    	task = new PublishTask(this);
    	task.execute();
    }

    private void onTabChange(String tabId) {
		if (TAG_SESSIONS.equals(tabId) && starredSessionCount > 0) {
	        mEmailSeparator.setVisibility(View.VISIBLE);
	        mEmailButton.setVisibility(View.VISIBLE);
	        mPublishSeparator.setVisibility(View.VISIBLE);
	        mPublishButton.setVisibility(View.VISIBLE);
		} else {
	        mEmailSeparator.setVisibility(View.GONE);
	        mEmailButton.setVisibility(View.GONE);
	        mPublishSeparator.setVisibility(View.GONE);
	        mPublishButton.setVisibility(View.GONE);
		}
    }

	/** Build and add "sessions" tab. */
    private void setupSessionsTab() {
        final TabHost host = getTabHost();

        final Uri uri = Sessions.CONTENT_STARRED_URI
	    	.buildUpon()
	    	.appendQueryParameter(SessionCounts.SESSION_INDEX_EXTRAS, Boolean.TRUE.toString())
	    	.build();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_TAB);
        final boolean highlightParalleStarred = settingsPrefs.getBoolean(getString(R.string.visualize_parallel_starred_sessions_key), false);
        intent.putExtra(SessionsActivity.EXTRA_HIHGLIGHT_PARALLEL_STARRED, highlightParalleStarred);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);
        
        // Sessions content comes from reused activity
        host.addTab(host.newTabSpec(TAG_SESSIONS)
                .setIndicator(buildIndicator(R.string.starred_sessions))
                .setContent(intent));
    }

    /** Build and add "speakers" tab. */
    private void setupSpeakersTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Speakers.CONTENT_STARRED_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        // Speakers content comes from reused activity
        host.addTab(host.newTabSpec(TAG_SPEAKERS)
                .setIndicator(buildIndicator(R.string.starred_speakers))
                .setContent(intent));
    }
    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested
     * string resource as its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                getTabWidget(), false);
        indicator.setText(textRes);
        return indicator;
    }
    
    private boolean checkMySchedulePrefs() {
    	final String email = mySchedulePrefs.getString(MyScheduleActivity.MySchedulePrefs.EMAIL, null);
    	final String activationCode = mySchedulePrefs.getString(MySchedulePrefs.ACTIVATION_CODE, null);
    	
    	return (email != null 
    				&& email.trim().length() > 0 
    				&& activationCode != null 
    				&& activationCode.trim().length() > 0);
    }
    
	private static HttpClient sHttpClient;

    private static synchronized HttpClient getHttpClient(Context context) {
        if (sHttpClient == null) {
            sHttpClient = SyncUtils.getHttpClient(context);
        }
        return sHttpClient;
    }
    
    private abstract class MyScheduleTask extends AsyncTask<Void, Void, Void> {
    	
    	private StarredActivity activity;
    	private final String mUrl;
    	private final String mMessage;
    	private final String mToastOk;
    	private final String mToastNok;

    	private ProgressDialog mDialog;  
    	private boolean mResultOk = false;

    	public MyScheduleTask(StarredActivity activity, String url, String message, String toastOk, String toastNok) {
			this.activity = activity;
    		this.mUrl = url;
			this.mMessage = message;
			this.mToastOk = toastOk;
			this.mToastNok = toastNok;
		}
    	
		protected void onPreExecute() {
        	mDialog = ProgressDialog.show(StarredActivity.this, null, mMessage, true, false);
        }  

        @Override
        protected Void doInBackground(Void... params) {
        	final String activationCode = getActivationCode();
        	final String email = getEmail();
        	final ArrayList<String> favorites = getStarredSessions();
        	
            try {
                final Context context = StarredActivity.this;
                final HttpClient httpClient = getHttpClient(context);
                final HttpPost httpPost = new HttpPost(mUrl);
                
                final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("code", activationCode));
                nameValuePairs.add(new BasicNameValuePair("email", email));
                nameValuePairs.add(new BasicNameValuePair("event", "1"));
                for (String favorite : favorites) {
                    nameValuePairs.add(new BasicNameValuePair("favorites", favorite));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                final HttpResponse resp = httpClient.execute(httpPost);
                final int statusCode = resp.getStatusLine().getStatusCode();
                
                if (statusCode != HttpStatus.SC_CREATED) {
                	mResultOk = false;
                	return null;
                }
                
            	mResultOk = true;
            } catch (Exception e) {
            	Log.e(TAG, e.getMessage());
            	mResultOk = false;
            	cancel(true);
            }
            
            return null;
        }

        protected void onPostExecute(Void unused) {
        	if (mDialog != null && mDialog.isShowing()) {
        		mDialog.dismiss();
        	}
            
            if (mResultOk) {
                Toast.makeText(StarredActivity.this, mToastOk, Toast.LENGTH_LONG).show();  
            } else {  
                Toast.makeText(StarredActivity.this, mToastNok, Toast.LENGTH_LONG).show();  
            }  
        }
        
        private String getEmail() {
        	return mySchedulePrefs.getString(MyScheduleActivity.MySchedulePrefs.EMAIL, null);
        }
        
        private String getActivationCode() {
        	return mySchedulePrefs.getString(MyScheduleActivity.MySchedulePrefs.ACTIVATION_CODE, null);
        }
        
        private ArrayList<String> getStarredSessions() {
        	final ContentResolver resolver = getContentResolver();
        	final Uri uri = Sessions.CONTENT_STARRED_URI;
            final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION, null, null, null);
            final ArrayList<String> sessionIds = Lists.newArrayList();
            try {
            	while (cursor.moveToNext()) {
            		final String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
            		try {
	            		if (Integer.valueOf(sessionId) < 901) {
	            			sessionIds.add(sessionId);
	            		}
            		} catch (NumberFormatException e) {
            			// just eat the exception and continue
            		}
            	}
            } finally {
                cursor.close();
            }
            return sessionIds;
        }

		void detach() {
			this.activity = null;
		}

		void attach(StarredActivity activity) {
			this.activity = activity;
			if (Status.RUNNING.equals(getStatus())) {
				onPreExecute();
			}
		}
		
    }
    
    private class EmailTask extends MyScheduleTask {

		public EmailTask(StarredActivity activity) {
			super(activity, 
				  Constants.MYSCHEDULE_EMAIL_URL,
				  getResources().getString(R.string.myschedule_email_message),
				  getResources().getString(R.string.myschedule_email_ok),
				  getResources().getString(R.string.myschedule_email_nok));
		}

    }

    private class PublishTask extends MyScheduleTask {

		public PublishTask(StarredActivity activity) {
			super(activity,
				  Constants.MYSCHEDULE_PUBLISH_URL,
				  getResources().getString(R.string.myschedule_publish_message),
				  getResources().getString(R.string.myschedule_publish_ok),
				  getResources().getString(R.string.myschedule_publish_nok));
		}

    }

    /** {@link Sessions} query parameters */
    private interface SessionsQuery {
        String[] PROJECTION = {
        		Sessions.SESSION_ID,
        };

        int SESSION_ID = 0;
    }

}
