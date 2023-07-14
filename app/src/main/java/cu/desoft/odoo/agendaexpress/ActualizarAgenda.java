package cu.desoft.odoo.agendaexpress;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.DatePicker;
import android.widget.Toast;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import de.timroes.axmlrpc.XMLRPCClient;

import static java.util.Collections.emptyMap;


public class ActualizarAgenda extends AppCompatActivity {
    ConnectionData connectiondata; // class to save login data
    private EditText mEditFecha;
    private Button mButtonSyn;
    private View mUpdateProgressView;
    private View mUpdateFormView;

    ArrayList<Event> event_list = new ArrayList<Event>();

    AlertDialog.Builder AlertMsg;
    private String msgResult = "";
    String selectedDate;
//    Calendar data
//    long calendarID = 3;
    private  String CALENDAR_NAME = "Agenda Express";
    private  String CALENDAR_ACCOUNT_NAME = "Agenda Express";
    private  String CALENDAR_DISPLAY_NAME = "Agenda Express";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectiondata = (ConnectionData) getIntent().getSerializableExtra("ConnData");


        setContentView(R.layout.activity_actualizar_agenda);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        AlertMsg = new AlertDialog.Builder(this);

        mEditFecha = (EditText) findViewById(R.id.edit_fecha);
        mEditFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.edit_fecha:
                        showDatePickerDialog();
                        break;
                }
            }
        });
        mButtonSyn = (Button) findViewById(R.id.button_syn);
        mButtonSyn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//
                ActualizarAgenda.ConnectionOdoo tarea;
                tarea = new ActualizarAgenda.ConnectionOdoo();
                Toast.makeText(ActualizarAgenda.this, "Ahora comenzamos a actualizar las tareas, esto puede demorar unos minutos.", Toast.LENGTH_LONG).show();
                tarea.execute();
            }
        });

        mUpdateProgressView = findViewById(R.id.updateprogress);
        mUpdateFormView = findViewById(R.id.updateform);
    }
        private void showDatePickerDialog() {
            DatePickerFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    // +1 because January is zero
                    selectedDate = year+"-"+ (month+1) + "-" + day ;
                    mEditFecha.setText(selectedDate);
                }
            });
            newFragment.show(getSupportFragmentManager(), "datePicker");
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

            mUpdateProgressView.setVisibility(show ? View.GONE : View.VISIBLE);
            mUpdateProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUpdateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mUpdateProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mUpdateProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUpdateProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mUpdateProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mUpdateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void executeAboutUs(View view) {
        Intent i = new Intent(this, AboutUs.class);
        startActivity(i);
    }

    private class ConnectionOdoo extends AsyncTask<Void, ArrayList<String>, Boolean> {
        ProgressDialog mProgressDialog;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
//
                OdooConnect oc = OdooConnect.connect(connectiondata.getUrl(),
                                                    connectiondata.getPort(),connectiondata.getDb(),
                                                    connectiondata.getUsername(), connectiondata.getPassword());
                // buscar el id del partner segun el usuario
                Object[] partner_filter = { new Object[]{
                        new Object[]{"user_id", "=", connectiondata.getUID()},
                        new Object[]{"active", "=", true}
                }
                };
                List<HashMap<String, Object>> partner_data = oc.search_read("res.users",partner_filter, "partner_id");
                int partner_id = Integer.parseInt(partner_data.get(0).get("partner_id").toString());

                // Buscar las tareas por el id del partner y la fecha seleccionada
                Object[] event_filter = { new Object[]{
                        new Object[]{"partner_ids", "in", partner_id},
                        new Object[]{"start", ">=", selectedDate}
                        }
                        };
                List<HashMap<String, Object>> event_data = oc.search_read("calendar.event", event_filter, "name", "start", "stop");

                for (int e = 0; e < event_data.size(); e++) {
                    String event_name =  event_data.get(e).get("name").toString();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date ds = formatter.parse(event_data.get(e).get("start").toString());
                    Calendar startTime = Calendar.getInstance();
                    startTime.setTime(ds);

                    Date de = formatter.parse(event_data.get(e).get("stop").toString());
                    Calendar endTime = Calendar.getInstance();
                    endTime.setTime(de);
                    Boolean allday = false; // Boolean.parseBoolean(event_data.get(e).get("allday").toString());
                    event_list.add(new Event(event_name,startTime,endTime,allday));
                    ArrayList<String> s=new ArrayList<>();
                    s.add(String.valueOf( e* 100 / event_data.size()));
                    publishProgress(s);
                }

            }catch (Exception ex) {
                msgResult = "ERROR!"+ ex.toString();
                return false;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(ArrayList<String>... values) {
            mProgressDialog.setProgress(Integer.parseInt(values[0].get(0)));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create ProgressBar
            mProgressDialog = new ProgressDialog(ActualizarAgenda.this);
            // Set your ProgressBar Title
            mProgressDialog.setTitle("Actualizando");
//            mProgressDialog.setIcon(R.drawable.dwnload);
            // Set your ProgressBar Message
            mProgressDialog.setMessage("Actualizando tareas en el calendario.");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            // Show ProgressBar
            mProgressDialog.setCancelable(false);
            //  mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Boolean pass=false;

            mProgressDialog.setMessage("Creando los eventos en el calendario.");

            if (result) {
                if (ContextCompat.checkSelfPermission(ActualizarAgenda.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
                    {
                    // VERIFICAR SI ESTA LA CUENTA SINO LA CREO
                        long CalendarAccountID = obtainCalendarAccountID();
                    // ELIMINAR EVENTOS A PARTIR DE LA FECHA SELECCIONADA
                        long selectedDate=0;
                        try {
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                            Date ds = formatter.parse(mEditFecha.getText().toString());
                            Calendar startTime = Calendar.getInstance();
                            startTime.setTime(ds);
                            selectedDate = startTime.getTimeInMillis();
                        }catch (ParseException e)
                        {
                            msgResult=e.getMessage();
                        }

                        DeleteAllEventFromDate(String.valueOf(CalendarAccountID),selectedDate);

                    // recorro la lista de eventos para crearlos en el calendario
                    for (int i = 0; i < event_list.size(); i++) {
                        String event_name = event_list.get(i).getName();
                        Calendar beginTime = event_list.get(i).getStartTime();
                        long startMillis = 0;
                        startMillis = beginTime.getTimeInMillis();
                        Calendar endTime = event_list.get(i).getEndTime();
                        long endMillis = 0;
                        endMillis = endTime.getTimeInMillis();
                        // CREAR EVENTOS
                        ContentResolver cr = getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(CalendarContract.Events.DTSTART, startMillis);
                        values.put(CalendarContract.Events.DTEND, endMillis);
                        values.put(CalendarContract.Events.TITLE, event_name);
                        values.put(CalendarContract.Events.DESCRIPTION, "Actualizado con la version 1.0 RC");
                        values.put(CalendarContract.Events.CALENDAR_ID, CalendarAccountID);
//                        values.put(CalendarContract.Events.EVENT_TIMEZONE,"America/New_York");
                        values.put(CalendarContract.Events.EVENT_TIMEZONE,  TimeZone.getDefault().getID());
                        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

                        // get the event ID that is the last element in the Uri
                        long eventID = Long.parseLong(uri.getLastPathSegment());
                        // CREAR RECORDATORIOS
                        Uri RemindersUri = CalendarContract.Reminders.CONTENT_URI;
                        ContentValues reminders = new ContentValues();
                        reminders.put(CalendarContract.Reminders.EVENT_ID, eventID);
                        reminders.put(CalendarContract.Reminders.MINUTES, 10);
                        reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                        Uri reminderUri = cr.insert(RemindersUri, reminders);

                        int progress = i* 100 / event_list.size();
                        mProgressDialog.setProgress(progress);

                    }
                    AlertMsg.setMessage("Se han actualizado "+ String.valueOf(event_list.size())+" tareas.");
                    AlertMsg.show();
                    pass=true;

                }else{
                    AlertMsg.setMessage("No se han actualizado "+ String.valueOf(event_list.size()+" tareas por problemas de permisos."));
                    AlertMsg.show();
                }
            }else {
                AlertMsg.setMessage(msgResult);
                AlertMsg.show();
            }
//            mUpdateProgressView.setVisibility(View.GONE);
            mProgressDialog.dismiss();
            if (pass)
            {
            executeAboutUs(null);
            }
        }

        @Override
        protected void onCancelled() {
            Toast.makeText(ActualizarAgenda.this, "Conexion cancelada.", Toast.LENGTH_SHORT).show();
            mProgressDialog.dismiss();
        }
/*
* Calendar manager methods ===========================================
*/

    @SuppressWarnings("WeakerAccess")
    public long obtainCalendarAccountID() {
        long calID = checkCalendarAccount();
        if (calID >= 0) {
            return calID;
        } else {
            return CreateCalendarAccount();
        }
    }
    /**

     */
    private long checkCalendarAccount() {
        ContentResolver resolver = getContentResolver();
        if (ContextCompat.checkSelfPermission(ActualizarAgenda.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
        {
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("+ CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
            String[] selectionArgs = new String[]{CALENDAR_ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL};
            Cursor cursor = resolver.query(uri,null,selection,selectionArgs,null);

            if (null == cursor) {
                return -1;
            }
            int count = cursor.getCount();

            if (count > 0) {
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID));
            } else {
                return -1;
            }
        }else{
            return -1;
        }
    }
        private void DeleteAllEventFromDate(String cal, long selectedDate) {
            Cursor cursor;
            Uri eventsUri;
            ContentResolver resolver = getContentResolver();

            String selection = "((" + CalendarContract.Events.CALENDAR_ID + " = ?) AND ("+ CalendarContract.Events.DTSTART + " > ?))";
            String[] selectionArgs = new String[]{cal, String.valueOf(selectedDate)};

            int osVersion = android.os.Build.VERSION.SDK_INT;
            if (osVersion <= 7) { //up-to Android 2.1
                eventsUri = Uri.parse("content://calendar/events");
                cursor = resolver.query(eventsUri, new String[]{ "_id" }, selection , selectionArgs, null);
            } else { //8 is Android 2.2 (Froyo) (http://developer.android.com/reference/android/os/Build.VERSION_CODES.html)
                eventsUri = Uri.parse("content://com.android.calendar/events");
                cursor = resolver.query(eventsUri, new String[]{ "_id" }, selection , selectionArgs, null);
            }

            while(cursor.moveToNext()) {
                long eventId = cursor.getLong(cursor.getColumnIndex("_id"));
                resolver.delete(ContentUris.withAppendedId(eventsUri, eventId), null, null);
            }
            cursor.close();
        }

    /**
     *
     * @return success：ACCOUNT ID , create failed：-1 , permission deny：-2
     */
    private long CreateCalendarAccount() {
        ContentResolver resolver = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        Uri accountUri;

        ContentValues account = new ContentValues();

        account.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);

        account.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);

        account.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME);

        account.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME);

        account.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.parseColor("#015c6c"));

        account.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);

        account.put(CalendarContract.Calendars.VISIBLE, 1);

        account.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());

        account.put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1);

        account.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

        account.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDAR_ACCOUNT_NAME);

        account.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1);

        account.put(CalendarContract.Calendars.MAX_REMINDERS, 8);

        account.put(CalendarContract.Calendars.ALLOWED_REMINDERS, "0,1,2,3,4");

        account.put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, "0,1,2");

        account.put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, "0,1,2");


        uri = uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.Calendars.CALENDAR_LOCATION)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(ActualizarAgenda.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                accountUri = resolver.insert(uri, account);
            } else {
                return -2;
            }
        } else {
            accountUri = resolver.insert(uri, account);
        }

        return accountUri == null ? -1 : ContentUris.parseId(accountUri);
    }

        //     * @return -2: permission deny  0: No designated account  1: delete success
//     */
    public int DeleteCalendarAccountByName() {
        int deleteCount;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        ContentResolver resolver = getContentResolver();

        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        String[] selectionArgs = new String[]{CALENDAR_ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if  (ContextCompat.checkSelfPermission(ActualizarAgenda.this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            {
                deleteCount = resolver.delete(uri, selection, selectionArgs);
            } else {
                return -2;
            }
        } else {
            deleteCount = resolver.delete(uri, selection, selectionArgs);
        }

        return deleteCount;
    }

    }


}

