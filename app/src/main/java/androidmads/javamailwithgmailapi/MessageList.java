package androidmads.javamailwithgmailapi;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import com.google.api.services.gmail.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import androidmads.javamailwithgmailapi.helper.Utils;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MessageList extends Activity
        implements EasyPermissions.PermissionCallbacks {

    private TextView mOutputText;
    private Button mCallApiButton;


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    public static String id;

    private static final String BUTTON_TEXT = "Call Gmail API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { GmailScopes.GMAIL_LABELS };
    private ListView list;
    public static   String id1;
    public static List<String> test1;
    public static List<String> testt=new ArrayList<String>();
    public static com.google.api.services.gmail.Gmail mService = null;
    FloatingActionButton sendFabButton;
    EditText edtToAddress, edtSubject, edtMessage;
    Toolbar toolbar;
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    LinearLayout activityLayout;
    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        list = new ListView(this);
        mCallApiButton = new Button(this);
       Button mCallApiButton1 = new Button(this);
        mCallApiButton.setText("Return to Send a Email");
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
               // getResultsFromApi();
                Intent i = new Intent(MessageList.this, MainActivity.class);
              startActivity(i);
                mCallApiButton.setEnabled(true);
                Button mCallApiButton1 = new Button(MessageList.this);


             // activityLayout.addView(mCallApiButton1);
            }
        });
        activityLayout.addView(mCallApiButton);
       // activityLayout.addView(list);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT +"\' button to test the API.");
      //  activityLayout.addView(mOutputText);
        activityLayout.addView(list);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Gmail API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
       // makeRequest(mCredential);
        getResultsFromApi();
    }



    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new CallMessageList(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MessageList.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
    public Gmail makeRequest(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Gmail API Android Quickstart")
                .build();
return  mService;
    }
    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class CallMessageList extends AsyncTask<Void, Void, List<String>> {

        private Exception mLastError = null;

        public CallMessageList (GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

        }

        /**
         * Fetch a list of Gmail labels attached to the specified account.
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<String> labels = new ArrayList<String>();

            List<String> test=new ArrayList<String>();
           // test.add("ll");
           // try{



            ArrayList<List<String>> data=new ArrayList<List<String>>();

               // List<Message> m=new ArrayList<Message>();
                //m=listMessagesMatchingQuery(mService, "me", "");
            Looper.prepare();
            try{
                List<Message> messages = new ArrayList<Message>();
             test=listMessagesMatchingQuery(mService, "me", "");
                 // id = message.getId();
                 // Listemessages.add(id);

                  //getMimeMessage(service,"me",id);

//                test1 = getMimeMessage(mService, "me", id1);
                 AlertDialog alertDialog = new AlertDialog.Builder(MessageList.this).create(); //Read Update
                  alertDialog.setTitle("");

                  ListView modeList = new ListView(MessageList.this);
                  //  String[] stringArray = new String[] { "Programme", "Détails" ,"S'inscrire"};
                  ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(MessageList.this, android.R.layout.simple_list_item_1, android.R.id.text1, test);

                  modeList.setAdapter(modeAdapter);
                Button mCallApiButton1 = new Button(MessageList.this);
                // MainActivity3 m=new MainActivity3();

               // activityLayout.addView(mCallApiButton1);
                modeList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id){
                        Toast.makeText(MessageList.this, "" + position, Toast.LENGTH_SHORT).show();
                     ;
                    }
                });
                //  alertDialog.show();

                // Toast.makeText(MainActivity2.this, "testt",Toast.LENGTH_SHORT).show();
               // List<Message> m=new ArrayList<Message>();

        } catch (MessagingException ex) {
            Logger.getLogger(MessageList.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessageList.class.getName()).log(Level.SEVERE, null, ex);
        }

                    //System.out.println("ff"+m.get(i).);


              //  }
                // Toast.makeText(MainActivity2.this,"tt",Toast.LENGTH_LONG);
                //   System.out.println(id);

                //getMessage(service, "me", "14bdc1d033faebf3");7
                //getMimeMessage(service,"me","14bdc1d033faebf3");

            return test;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Click on one of the identifiers of this list to show the detail of the message:");
                mOutputText.setText(TextUtils.join("\n", output));

                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(MessageList.this, android.R.layout.simple_list_item_1, android.R.id.text1, output);

                list.setAdapter(modeAdapter);
                list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                   //   try{
                        Toast.makeText(MessageList.this, "" + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
                        id1 = parent.getItemAtPosition(position).toString();
                        Gmail service= makeRequest(mCredential);
                      //  getMimeMessage(service, "me", id1);
                        Intent i = new Intent();
                      //  i.setClass(MessageList.this, MainActivity4.class);
                      //  getResultsFromApi();
                       // getTest();
                        new CallDetailMessage(mCredential).execute();
                      //   startActivity(i);
//                        getTest();

                    }
                });
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MessageList.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }

    }

        public  List<String> listMessagesMatchingQuery(Gmail service, String userId,
                String query) throws IOException, MessagingException {
            ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

            List<Message> messages = new ArrayList<Message>();
            List<String> Listemessages = new ArrayList<String>();

            while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId).setQ(query)
                            .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }

            for (Message message : messages) {
                id=message.getId();
                Listemessages.add(id);

                //getMimeMessage(service,"me",id);
                // System.out.println(id);
                //  MimeMessage msg = createEmail(to, from, subject, bodyText);
                //    getMimeMessage(service,"me",id);

            }

            return Listemessages;
        }



    /////////////////////////////////////////////////////

    // ...








    public static List<Message> listMessagesWithLabels(Gmail service, String userId,
                                                       List<String> labelIds) throws IOException {
        ListMessagesResponse response = service.users().messages().list(userId)
                .setLabelIds(labelIds).execute();

        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setLabelIds(labelIds)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        for (Message message : messages) {
            //System.out.println(message.toPrettyString());
        }

        return messages;
    }




    public static List<String> getMimeMessage(Gmail service, String userId, String messageId)
            throws IOException, MessagingException {
        Message message = service.users().messages().get(userId, messageId).setFormat("raw").execute();

        byte[] emailBytes = Base64.decodeBase64(message.getRaw());

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

        Address[] a=email.getFrom();

        //System.out.println("date"+email.getSentDate().toLocaleString());

        // System.out.println("From "+a[0]+email.getSubject());
        List<String> test=new ArrayList<String>();


        ListMessagesResponse response = service.users().messages().list(userId).setQ("").execute();

        List<Message> messages = new ArrayList<Message>();
        List<String> Listemessages = new ArrayList<String>();

        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ("")
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        test.add("\n"+"\n"+"From: "+a[0]+"\n"+"\n"+"Sent Date: "+email.getSentDate().toLocaleString()+"\n"+"\n"+"Subject: "+email.getSubject()+"\n"+"\n"+"Content: "+email.getContent());

        //getMimeMessage(service,"me",id);
        // System.out.println(id);
        //  MimeMessage msg = createEmail(to, from, subject, bodyText);
        //    getMimeMessage(service,"me",id);


        return test;
    }




    public static Message getMessage(Gmail service, String userId, String messageId)
            throws IOException {
        Message message = service.users().messages().get(userId, messageId).setFormat("full").execute();

        System.out.println("Message snippet: " + message.getSnippet());

        return message;
    }


    private class CallDetailMessage extends AsyncTask<Void, Void, List<String>> {
        private Gmail mService = null;
        private Exception mLastError = null;

        public CallDetailMessage(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();

        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

        }

        /**
         * Fetch a list of Gmail labels attached to the specified account.
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<String> labels = new ArrayList<String>();

            List<String> test11=new ArrayList<String>();
            //   test.add("ll");
            // try{



            // List<Message> m=new ArrayList<Message>();
            //m=listMessagesMatchingQuery(mService, "me", "");
            //  Looper.prepare();
            try{
                //    List<Message> messages = new ArrayList<Message>();
                //test=listMessagesMatchingQuery(mService, "me", "");
                // id = message.getId();
                // Listemessages.add(id);

                //getMimeMessage(service,"me",id);

                test11 = getMimeMessage(mService, "me", id1);


            } catch (MessagingException ex) {
                Logger.getLogger(MessageList.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MessageList.class.getName()).log(Level.SEVERE, null, ex);
            }

            //System.out.println("ff"+m.get(i).);


            //  }
            // Toast.makeText(MainActivity2.this,"tt",Toast.LENGTH_LONG);
            //   System.out.println(id);

            //getMessage(service, "me", "14bdc1d033faebf3");7
            //getMimeMessage(service,"me","14bdc1d033faebf3");

            return test11;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute( List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
               // output.add(0, "Data retrieved using the Gmail API:");
                // mOutputText.setText(TextUtils.join("\n", output));

                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(MessageList.this, android.R.layout.simple_list_item_1, android.R.id.text1, output);



                AlertDialog alertDialog = new AlertDialog.Builder(MessageList.this).create(); //Read Update
                //String g=(String) tv1.getText();

                // Toast.makeText(getApplicationContext(),TsiteWeb.getText(),Toast.LENGTH_SHORT).show();



                //TraisonSociale.setGravity(Gravity.CENTER);

                //TsiteWeb.setGravity(Gravity.CENTER);
                //int c=(Color.parseColor("#FF7F27"));
                // TsiteWeb.setTextColor(Color.parseColor("#005d61"));

//				        	    	   /setDividerColor(c);
                //   alertDialog.setTitle( TraisonSociale.getText()+"\n"+   TsiteWeb.getText());
                //setTitleColor(c);


                //txtView.setGravity(Gravity.CENTER_VERTICAL);
                alertDialog.setMessage(""+output);

                //  alertDialog.setButton("Continue..", new DialogInterface.OnClickListener() {
                //   public void onClick(DialogInterface dialog, int which) {
                // here you can add functions
                alertDialog.show();
                //  startActivity(i);

            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MessageList.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
///////////////
private class MakeRequestTask3 extends AsyncTask<Void, Void, String> {
    private Gmail mService = null;
    private Exception mLastError = null;
    private View view = sendFabButton;

    public MakeRequestTask3(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    private String getDataFromApi() throws IOException {
        // getting Values for to Address, from Address, Subject and Body
        String user = "me";
        String to = Utils.getString(edtToAddress);
        String from = mCredential.getSelectedAccountName();
        String subject = Utils.getString(edtSubject);
        String body = Utils.getString(edtMessage);
        MimeMessage mimeMessage;
        String response = "";
        try {
            mimeMessage = createEmail(to, from, subject, body);
            response = sendMessage(mService, user, mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return response;
    }

    // Method to send email
    private String sendMessage(Gmail service,
                               String userId,
                               MimeMessage email)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(email);
        // GMail's official method to send email with oauth2.0
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message.getId();
    }

    // Method to create email Params
    private MimeMessage createEmail(String to,
                                    String from,
                                    String subject,
                                    String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        InternetAddress tAddress = new InternetAddress(to);
        InternetAddress fAddress = new InternetAddress(from);

        email.setFrom(fAddress);
        email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    @Override
    protected void onPreExecute() {
        mProgress.show();
    }

    @Override
    protected void onPostExecute(String output) {
        mProgress.hide();
        if (output == null || output.length() == 0) {
            showMessage(view, "No results returned.");
        } else {
            showMessage(view, output);
        }
    }

    @Override
    protected void onCancelled() {
        mProgress.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        Utils.REQUEST_AUTHORIZATION);
            } else {
                showMessage(view, "The following error occurred:\n" + mLastError.getMessage());
                Log.v("Error", mLastError.getMessage());
            }
        } else {
            showMessage(view, "Request Cancelled.");
        }
    }
    private void showMessage(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}


}