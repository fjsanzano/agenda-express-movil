package cu.desoft.odoo.agendaexpress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;

import static android.Manifest.permission.READ_CONTACTS;
import static java.util.Collections.emptyMap;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    ConnectionData connectionData = new ConnectionData();
    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mServerView;
    private EditText mPortView;
    private AutoCompleteTextView mDatabaseView;
    private AutoCompleteTextView mUserView;
    private EditText mPasswordView;
    private CheckBox save_account_data;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Google Play will install latest OpenSSL
//            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        }
        catch ( KeyManagementException | NoSuchAlgorithmException  e)
        {
            e.printStackTrace();
        }


        setContentView(R.layout.activity_login);
        // Set up the login form.
        mServerView = (AutoCompleteTextView) findViewById(R.id.server);
        mPortView = (EditText) findViewById(R.id.port);
        mDatabaseView = (AutoCompleteTextView) findViewById(R.id.database);
        mUserView = (AutoCompleteTextView) findViewById(R.id.user);
        save_account_data= (CheckBox) findViewById(R.id.save_acount_data);
//        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        LoadPreferences();

       /* mButtonTest = (Button) findViewById(R.id.button);
        mButtonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (save_account_data.isChecked()==true) {
                    SavePreferences();
                    Toast.makeText(LoginActivity.this, "Save Preferences.", Toast.LENGTH_LONG).show();
                }else{
                    LoadPreferences();
                    Toast.makeText(LoginActivity.this, "Load preferences.", Toast.LENGTH_LONG).show();

                }
            }
        });*/


    }

    public void executeActualizarAgenda(View view) {
        Intent i = new Intent(this, ActualizarAgenda.class);
        i.putExtra("ConnData", connectionData);
        startActivity(i);
    }

    /*private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }*/
/*
    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }*/

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mServerView.setError(null);
        mPortView.setError(null);
        mDatabaseView.setError(null);
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String server = mServerView.getText().toString();
        Integer port = Integer.parseInt(mPortView.getText().toString());
        String database = mDatabaseView.getText().toString();
        String user = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        // Check for a valid user.
        if (TextUtils.isEmpty(user)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        }
        // Check for a valid server.
        if (TextUtils.isEmpty(server)) {
            mServerView.setError(getString(R.string.error_field_required));
            focusView = mServerView;
            cancel = true;
        }
        // Check for a valid port.
        if (port<=0) {
            mPortView.setError(getString(R.string.error_field_required));
            focusView = mPortView;
            cancel = true;
        }
        // Check for a valid database.
        if (TextUtils.isEmpty(database)) {
            mDatabaseView.setError(getString(R.string.error_field_required));
            focusView = mDatabaseView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(server,port,database,user, password);
            mAuthTask.execute((Void) null);
        }
    }
    /*private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }*/

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                                                                     .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
       /* List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);*/
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
    public void SavePreferences(){
        if (save_account_data.isChecked()==true) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences("AgendaExpressAccount", 0); // 0 - for private mode
            SharedPreferences.Editor editor = pref.edit();
            //editor.putBoolean("key_name", true); // Storing boolean - true/false
            editor.putString("server", mServerView.getText().toString()); // Storing string
            editor.putString("port", mPortView.getText().toString()); // Storing string
            editor.putString("database", mDatabaseView.getText().toString()); // Storing string
            editor.putString("user", mUserView.getText().toString()); // Storing string
            editor.putString("password", mPasswordView.getText().toString()); // Storing string
            //editor.putInt("key_name", "int value"); // Storing integer
//            editor.putFloat("key_name", "float value"); // Storing float
//            editor.putLong("key_name", "long value"); // Storing long

            editor.commit(); // commit changes
        }

    }
    public void LoadPreferences(){
        SharedPreferences pref = getApplicationContext().getSharedPreferences("AgendaExpressAccount", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        mServerView.setText(pref.getString("server", "odoo.desoft.cu")); // getting String
        mPortView.setText(pref.getString("port", "80")); // getting String
        mDatabaseView.setText(pref.getString("database", "desoft_online")); // getting String
        mUserView.setText(pref.getString("user", "usuario.apellido")); // getting String
        mPasswordView.setText(pref.getString("password", null)); // getting String

//        pref.getInt("key_name", -1); // getting Integer
//        pref.getFloat("key_name", null); // getting Float
//        pref.getLong("key_name", null); // getting Long
//        pref.getBoolean("key_name", null); // getting boolean
    }

   /* private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }*/

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mServer;
        private final Integer mPort;
        private final String mDatabase;
        private final String mUser;
        private final String mPassword;
        private OdooConnect odooconexion;
        private String msgResult;

        UserLoginTask(String server,Integer port, String database,String user,String password) {
            mServer = server;
            mPort = port;
            mDatabase = database;
            mUser = user;
            mPassword = password;
            odooconexion = null;
            msgResult = "";

        }
        private static final String CONNECTOR_NAME = "OdooConnect";
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {

                URL loginUrl = new URL(String.format("%s/xmlrpc/common", mServer));
                XMLRPCClient client = new XMLRPCClient(loginUrl,XMLRPCClient.FLAGS_SSL_IGNORE_INVALID_CERT | XMLRPCClient.FLAGS_SSL_IGNORE_INVALID_HOST);
                Object[] list = {mDatabase, mUser, mPassword, emptyMap()};
                int uid = (int) client.call("authenticate", list);
                msgResult = "Se ha autenticado como: "+ mUser;
                connectionData.setUrl(mServer);
                connectionData.setPort(mPort);
                connectionData.setDb(mDatabase);
                connectionData.setUsername(mUser);
                connectionData.setPassword(mPassword);
                connectionData.setUID(uid);
                return true;
            }
            catch (XMLRPCException e) {
                msgResult = e.toString();
                return false;
                }
            catch (MalformedURLException e) {
                msgResult = e.toString();
                return false;
                }
            catch (ClassCastException e) {
                msgResult = e.toString(); // Bad login or password
                return false;
                }
            catch (Exception ex) {
                msgResult = ex.toString();
                return false;
            }

            // TODO: register the new account here.
//            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
//                si todo esta ok se salvan los datos y se pasa a la proxima activity
                SavePreferences();
                executeActualizarAgenda(null);
                Toast.makeText(LoginActivity.this,msgResult, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this,msgResult, Toast.LENGTH_SHORT).show();

            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

