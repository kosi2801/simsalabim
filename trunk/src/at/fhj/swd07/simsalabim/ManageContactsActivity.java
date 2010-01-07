package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.*;
import android.widget.AdapterView.*;

/*
 * Listhandling inspiriert von: http://www.anddev.org/creating_a_contextmenu_on_a_listview-t2438.html
 */

public class ManageContactsActivity extends TabActivity {

    private TabHost mTabHost;
    private ListView phoneView;
    private ListView simView;
    private ArrayList<Contact> phoneContacts;
    private ArrayList<Contact> simContacts;
    
    private SimUtil simUtil;
    
    private String CONTACT_SIM_URI = "content://icc/adn"; // URI for SIM card is different on Android 1.5 and 1.6, will be detected upon load of class
    
    /** Enum class for action id's in the menus */
    protected enum ContactActions {
        DELETE_SIM_CONTACT,
        COPY_SIM_CONTACT,
        COPY_PHONE_CONTACT,
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        simUtil = new SimUtil(getContentResolver());
        
        CONTACT_SIM_URI = detectSimUri();
        setContentView(R.layout.main);

        // initialize access variables
        mTabHost = getTabHost();
        phoneView = (ListView) findViewById(R.id.phoneview);
        simView = (ListView) findViewById(R.id.simview);

        // add tabs to switch between phone and sim listviews
        mTabHost.addTab(mTabHost.newTabSpec("tab_phone").setIndicator(getString(R.string.phone_tab_title)).setContent(R.id.phoneview));
        mTabHost.addTab(mTabHost.newTabSpec("tab_simcard").setIndicator(getString(R.string.sim_tab_title)).setContent(R.id.simview));

        // start with phone tab
        mTabHost.setCurrentTab(0);

        initListViews();
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
        
        // holds the contact on which the context menu was called 
        Contact contexedContact;

        Uri simUri = Uri.parse(CONTACT_SIM_URI);
        ContentResolver resolver = getContentResolver();
        
        /* Switch on the ID of the item, to get what the user selected. */
        switch (ContactActions.values()[aItem.getItemId()]) {
             case COPY_PHONE_CONTACT:
                 // get selected contact from phoneView
                 contexedContact = (Contact) phoneView.getAdapter().getItem(menuInfo.position);
                 
                 // create new instance for SIM insertion (must have empty id)
                 Contact newSimContact = new Contact("", contexedContact.name, contexedContact.number);
                 
                 // check, if already present on SIM
                 if(simContacts.contains(newSimContact)) {
                     Toast.makeText(ManageContactsActivity.this, "Already on SIM!", Toast.LENGTH_SHORT).show();
                     
                     return true; // true means, event has been handled
                 }
                 
                 // add it on the SIM card
                 ContentValues newSimValues = new ContentValues();
                 newSimValues.put("tag", contexedContact.getSimName());
                 newSimValues.put("number", contexedContact.number);
                 Uri newSimRow = resolver.insert(simUri, newSimValues);
                 
                 // TODO: further row values: "AdnFull", "/adn/0"
                 // TODO: null could also mean that the contact name was too long?
                 if (newSimRow == null) {
                     Toast.makeText(ManageContactsActivity.this, "Error storing on SIM!", Toast.LENGTH_SHORT).show();
                     return true; // true means, event has been handled
                 } else {
                     Toast.makeText(ManageContactsActivity.this, "Contact stored: " + newSimRow.toString(), Toast.LENGTH_SHORT).show();
                 }
                 
                 // TODO: reload contact from sim to get really stored info and add to simContacts list
                  
                 // add it to the simContacts list
                 simContacts.add(contexedContact);

                 refreshListViews();
                 return true; // true means, event has been handled
             case COPY_SIM_CONTACT:
                 // get selected contact from simView
                 contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);
                 
                 // create new instance for Phone insertion (must have empty id)
                 Contact newPhoneContact = new Contact("", contexedContact.name, contexedContact.number);
                 
                 // check, if already present on SIM
                 if(phoneContacts.contains(newPhoneContact)) {
                     Toast.makeText(ManageContactsActivity.this, "Already on Phone!", Toast.LENGTH_SHORT).show();
                     
                     return true; // true means, event has been handled
                 }
                 
                 // first, we have to create the contact
                 ContentValues newPhoneValues = new ContentValues();
                 newPhoneValues.put(Contacts.People.NAME, contexedContact.name);
                 Uri newPhoneRow = resolver.insert(Contacts.People.CONTENT_URI, newPhoneValues);
                 
                 if(newPhoneContact == null) {
                     Toast.makeText(ManageContactsActivity.this, "Error creating contact on Phone!", Toast.LENGTH_SHORT).show();
                     return true; // true means, event has been handled
                 }
                 
                 // then we have to add a number
                 newPhoneValues.clear();
                 newPhoneValues.put(Contacts.People.Phones.TYPE, Contacts.People.Phones.TYPE_MOBILE);
                 newPhoneValues.put(Contacts.Phones.NUMBER, contexedContact.number);
                 // insert the new phone number in the database using the returned uri from creating the contact
                 newPhoneRow = resolver.insert(Uri.withAppendedPath(newPhoneRow, Contacts.People.Phones.CONTENT_DIRECTORY), newPhoneValues);
                 
                 if (newPhoneRow == null) {
                     Toast.makeText(ManageContactsActivity.this, "Error adding number to contact on Phone!", Toast.LENGTH_SHORT).show();
                     return true; // true means, event has been handled
                 } else {
                     Toast.makeText(ManageContactsActivity.this, "Contact number stored: " + newPhoneRow.toString(), Toast.LENGTH_SHORT).show();
                 }
                  
                 // finally add it to the phoneContacts list
                 phoneContacts.add(contexedContact);

                 refreshListViews();
                 return true; /* true means: "we handled the event". */
             case DELETE_SIM_CONTACT:
                 // get selected contact from phoneView
                 contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);
                 
                 // make "Are you sure?"-Dialog handler
                 class DeleteHandler implements DialogInterface.OnClickListener {
                     private ContentResolver res;
                     private Uri uri;
                     private Contact contact;
                     public DeleteHandler(ContentResolver resolver, Uri uri, Contact contact) {
                         this.res = resolver;
                         this.uri = uri;
                         this.contact = contact;
                    }
                     
                     @Override
                    public void onClick(DialogInterface dialog, int which) {
                         // delete from SIM card
                         int count = res.delete(uri, "tag='"+contact.name+"' AND number='"+contact.number+"'", null);
                         
                         // TODO: what to do when more than 1 match? 
                         if(count != 1) {
                             Toast.makeText(ManageContactsActivity.this, count + " contacts removed from SIM! Error!", Toast.LENGTH_SHORT).show();
                         } else {
                             Toast.makeText(ManageContactsActivity.this, "Contact removed from SIM!", Toast.LENGTH_SHORT).show();
                         }
                         
                         // remove from simContacts if successful
                         simContacts.remove(contact);
                         refreshListViews();
                    }
                 }
                 
                 // and display the dialog
                 new AlertDialog.Builder(this) //
                 .setMessage("Are you sure?") //
                 .setPositiveButton("Yes!", new DeleteHandler(resolver, simUri, contexedContact)) //
                 .setNegativeButton("No!", null) //
                 .show();
                 
                 return true;
             default:
                 return false;
        }
    }
    
    private void refreshListViews() {
        // fill ListView for phone contacts
        {
            ArrayAdapter<Contact> phoneContactsAdapter = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_1, phoneContacts);
            phoneView.setAdapter(phoneContactsAdapter);
        }
        
        // fill ListView for SIM contacts
        {
            ArrayAdapter<Contact> simContactsAdapter = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_1, simContacts);
            simView.setAdapter(simContactsAdapter);
        }
    }

    private void initListViews() {
        // retrieve data for display
        phoneContacts = retrievePhoneContacts();
        simContacts = simUtil.retrieveSIMContacts(); 
        
        // set up contents of ListViews
        refreshListViews();
        
        // Set up listeners for Context-Menus on ListViews
        // if long-clicking, display a context-menu with further actions
        phoneView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("Modify Phone Contact");
                menu.add(0, ContactActions.COPY_PHONE_CONTACT.ordinal(), 0, "Copy to SIM card");
            }
        });
        // if short-clicking just display details for this contact
        phoneView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                      long arg3) {
                Contact itemAtPosition = (Contact)arg0.getItemAtPosition(arg2);
                
                new AlertDialog.Builder(ManageContactsActivity.this) //
                .setMessage( //
                        itemAtPosition.name + "\n" + //
                        itemAtPosition.number) //
                .setPositiveButton("Close", null) //
                .show();
                 
            }
        });
        simView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("Modify SIM Contact");
                menu.add(0, ContactActions.COPY_SIM_CONTACT.ordinal(), 0, "Copy to Phone");
                menu.add(0, ContactActions.DELETE_SIM_CONTACT.ordinal(), 0, "Delete from SIM");
            }
        });
        // if short-clicking just display details for this contact
        simView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                      long arg3) {
                Contact itemAtPosition = (Contact)arg0.getItemAtPosition(arg2);
                
                new AlertDialog.Builder(ManageContactsActivity.this) //
                .setMessage( //
                        itemAtPosition.name + "\n" + //
                        itemAtPosition.number) //
                .setPositiveButton("Close", null) //
                .show();
                 
            }
        });
    }

    private ArrayList<Contact> retrievePhoneContacts() {
        // ContentValues values = new ContentValues();
        // values.put("tag", "Jenny Huang");
        // values.put("number", "1234567");

        // get these columns from the content provider
        final String[] phoneProjection  = new String[] { //
                android.provider.Contacts.PeopleColumns.NAME, //
                android.provider.Contacts.PhonesColumns.NUMBER, //
                android.provider.BaseColumns._ID };        
        
        ContentResolver resolver = getContentResolver();
        // resolve all phone numbers
        Cursor results = resolver.query( //
                Contacts.Phones.CONTENT_URI, // URI of contacts with phone numbers 
                phoneProjection,             // get above defined columns
                null, null,                  //
                android.provider.Contacts.PeopleColumns.NAME); // sort by name

        // create array of Phone contacts and fill it
        final ArrayList<Contact> phoneContacts = new ArrayList<Contact>(results.getCount());
        while (results.moveToNext()) {
            final Contact phoneContact = new Contact(// 
                results.getString(2), // _id 
                results.getString(0), // name
                results.getString(1));// number

            phoneContacts.add(phoneContact);
        }
        return phoneContacts;
    }
    
    private String detectSimUri() {
        
        Uri uri15 = Uri.parse("content://sim/adn/"); // URI of Sim card on Android 1.5
//        Uri uri16 = Uri.parse("content://icc/adn/"); // URI of Sim card on Android 1.6
        
        // resolve something from Sim card
        ContentResolver resolver = getContentResolver();
        Cursor results = resolver.query( 
                uri15,                   
                new String[]{android.provider.BaseColumns._ID},
                null, null, null);
        
        // if null, we can use the 1.6 URI, otherwise we're on 1.5
        if(null == results) {
            return "content://icc/adn";
        } else {
            return "content://sim/adn";
        }
    }
    
}