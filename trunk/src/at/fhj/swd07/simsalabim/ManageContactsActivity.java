package at.fhj.swd07.simsalabim;

import java.util.List;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.*;
import android.widget.AdapterView.*;
import android.util.Log;

/*
 * Listhandling inspiriert von: http://www.anddev.org/creating_a_contextmenu_on_a_listview-t2438.html
 */

public class ManageContactsActivity extends TabActivity {

    private static final String TAG = ManageContactsActivity.class.getSimpleName();
    
    private TabHost mTabHost;
    private ListView phoneView;
    private ListView simView;
    private List<Contact> phoneContacts;
    private List<Contact> simContacts;
    
    private SimUtil simUtil;
    private PhoneUtil phoneUtil;
    
    /** Enum class for action id's in the menus */
    protected enum ContactActions {
        DELETE_SIM_CONTACT,
        COPY_SIM_CONTACT,
        EDIT_PHONE_CONTACT,
        COPY_PHONE_CONTACT,
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "onCreate(Bundle)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " Bundle("+savedInstanceState+")");
        
        super.onCreate(savedInstanceState);
        
        // initialize utility accessors for the two contact stores
        simUtil = new SimUtil(getContentResolver());
        phoneUtil = new PhoneUtil(getContentResolver());
        
        // set up main UI
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

    public boolean onCreateOptionsMenu(Menu menu) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "onCreateOptionsMenu(Menu)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " Menu("+menu+")");
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "onOptionsItemSelected(MenuItem)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " MenuItem("+item+")");
        
        switch (item.getItemId()) {
        case R.id.about:
            // and display the dialog
            new AlertDialog.Builder(this) //
                    .setCancelable(false) //
                    .setMessage(getString(R.string.about)) //
                    .setPositiveButton(getString(R.string.close), null) //
                    .show();
            return true;
        case R.id.quit:
            this.finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "onContextItemSelected(MenuItem)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " MenuItem("+aItem+")");
        
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
        
        // holds the contact on which the context menu was called 
        Contact contexedContact;
        
        // at first, check if a context menu was selected...
        /* Switch on the ID of the item, to get what the user selected. */
        switch (ContactActions.values()[aItem.getItemId()]) {
        case COPY_PHONE_CONTACT:
            // get selected contact from phoneView
            contexedContact = (Contact) phoneView.getAdapter().getItem(menuInfo.position);

            // convert to Contact suitable for storage on SIM
            Contact newSimContact = simUtil.convertToSimContact(contexedContact);

            // was conversion successful? could fail if SIM full and
            // maxContactNameLength couldn't be detected
            if (newSimContact == null) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_contact_conversion_failed), Toast.LENGTH_SHORT).show();
                return true; // true means, event has been handled
            }

            // check, if already present on SIM
            if (simContacts.contains(newSimContact)) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_contact_already_present), Toast.LENGTH_SHORT).show();
                return true; // true means, event has been handled
            }

            // create contact on SIM card
            Uri newSimRow = simUtil.createContact(newSimContact);

            // output feedback
            if (newSimRow == null) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_contact_not_stored), Toast.LENGTH_SHORT).show();
                return true; // true means, event has been handled
            } else {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.confirm_sim_contact_stored) + " " + newSimRow.toString(), Toast.LENGTH_SHORT)
                        .show();
            }

            // add it to the simContacts list
            simContacts.add(0, newSimContact);

            refreshSimListView();
            return true; // true means, event has been handled
        case COPY_SIM_CONTACT:
            // get selected contact from simView
            contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);

            // create new instance for Phone insertion (must have empty id)
            Contact newPhoneContact = new Contact("", contexedContact.name, contexedContact.number);

            // check, if already present on SIM
            if (phoneContacts.contains(newPhoneContact)) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_contact_already_present), Toast.LENGTH_LONG).show();

                return true; // true means, event has been handled
            }

            // create contact on phone
            Uri newContactUri = phoneUtil.createContact(contexedContact);

            // if NULL returned, the contact could not be created
            if (newPhoneContact == null) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_contact_not_stored), Toast.LENGTH_LONG).show();
                return true; // true means, event has been handled
            }

            // if contacts uri returned, there was an error with adding the
            // number
            if (newContactUri.getPath().contains("people")) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_number_not_stored), Toast.LENGTH_LONG).show();
                return true; // true means, event has been handled
            }

            // if phone uri returned, everything went OK
            if (newContactUri.getPath().contains("phones")) {
                Toast.makeText(ManageContactsActivity.this, getString(R.string.confirm_phone_contact_number_stored) + " " + newContactUri.toString(),
                        Toast.LENGTH_SHORT).show();
            } else {
                // some unknown error has happened
                Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_number_error) + " " + newContactUri.toString(), Toast.LENGTH_LONG)
                        .show();
                return true; // true means, event has been handled
            }

            // finally add it to the phoneContacts list
            newPhoneContact.id = newContactUri.getLastPathSegment();
            phoneContacts.add(0, newPhoneContact);

            refreshPhoneListView();
            return true; /* true means: "we handled the event". */
        case DELETE_SIM_CONTACT:
            // get selected contact from phoneView
            contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);

            // make "Are you sure?"-Dialog handler
            class DeleteHandler implements DialogInterface.OnClickListener {
                final String TAG2 = TAG+" - " + DeleteHandler.class.getSimpleName();
                private Contact contact;

                public DeleteHandler(Contact contact) {
                    if(Log.isLoggable(TAG, Log.DEBUG))
                        Log.d(TAG2, "DeleteHandler(Contact)");
                    if(Log.isLoggable(TAG, Log.VERBOSE))
                        Log.v(TAG2, " Contact("+contact+")");
                    
                    this.contact = contact;
                }

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(Log.isLoggable(TAG, Log.DEBUG))
                        Log.d(TAG2, "onClick(DialogInterface, int)");
                    if(Log.isLoggable(TAG, Log.VERBOSE))
                        Log.v(TAG2, " DialogInterface("+dialog+"), int("+which+")");
                    
                    int status = simUtil.deleteContact(contact);

                    // contact removed correctly
                    if (status == 0) {
                        Toast.makeText(ManageContactsActivity.this, getString(R.string.confirm_sim_contact_removed), Toast.LENGTH_SHORT).show();
                    } else if (status > 0) { // more than one match
                        Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_x_contacts_existing) + " " + status, Toast.LENGTH_LONG).show();
                        return;
                    } else { // nothing removed
                        Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_error_during_contact_removal), Toast.LENGTH_LONG).show();
                        return;
                    }

                    // remove from simContacts if successful
                    simContacts.remove(contact);
                    refreshSimListView();
                }
            }

            // and display the dialog
            new AlertDialog.Builder(this) //
                    .setCancelable(false) //
                    .setMessage(getString(R.string.are_you_sure)) //
                    .setPositiveButton(getString(R.string.yes), new DeleteHandler(contexedContact)) //
                    .setNegativeButton(getString(R.string.no), null) //
                    .show();

            return true;
        default:
            return super.onContextItemSelected(aItem);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "initListViews(int, int, Intent)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " int("+requestCode+"), int("+resultCode+"), Intent("+data+")");
        
        // if this was called after editing a phone contact, refresh the view
        if(requestCode == ContactActions.EDIT_PHONE_CONTACT.ordinal()) {
            phoneContacts = phoneUtil.retrievePhoneContacts();
            refreshPhoneListView();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    /**
     * refreshes the sim contacts ListViews using the current values stored in simContacts
     */
    private void refreshSimListView() {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "refreshSimListView()");
        
        // fill ListView for SIM contacts
        simView.setAdapter(new ContactRowAdapter(simContacts));
    }

    /**
     * refreshes the phone contacts ListView using the current values stored in phoneContacts
     */
    private void refreshPhoneListView() {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "refreshPhoneListView()");
        
        // fill ListView for phone contacts
        phoneView.setAdapter(new ContactRowAdapter(phoneContacts));
    }

    /**
     * initializes the Phone and SIM ListViews by reading the phoneContacts and simContacts, setting up
     * the ListViews and adding all required handlers to them 
     */
    private void initListViews() {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "initListViews()");
        
        // retrieve data for display
        phoneContacts = phoneUtil.retrievePhoneContacts();
        simContacts = simUtil.retrieveSIMContacts(); 
        
        // set up contents of ListViews
        refreshPhoneListView();
        refreshSimListView();
        
        // Set up listeners for Context-Menus on ListViews
        // if long-clicking, display a context-menu with further actions
        phoneView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            final String TAG2 = TAG+" - phoneView - " + OnCreateContextMenuListener.class.getSimpleName();
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                if(Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG2, "onCreateContextMenu(ContextMenu, View, ContextMenuInfo)");
                if(Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG2, " ContextMenu("+menu+"), View("+v+"), ContextMenuInfo("+menuInfo+")");
                
                menu.setHeaderTitle(getString(R.string.context_header_phone));
                menu.add(0, ContactActions.COPY_PHONE_CONTACT.ordinal(), 0, getString(R.string.context_entry_copy_to_sim));
            }
        });
        // if short-clicking start native contact editor
        phoneView.setOnItemClickListener(new OnItemClickListener(){
            final String TAG2 = TAG+" - phoneView - " + OnItemClickListener.class.getSimpleName();
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                      long arg3) {
                if(Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG2, "onItemClick(AdapterView, View, int, long)");
                if(Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG2, " AdapterView("+arg0+"), View("+arg1+"), int("+arg2+"), long("+arg3+")");
                
                Contact itemAtPosition = (Contact)arg0.getItemAtPosition(arg2);
                
                Intent editContact = new Intent(Intent.ACTION_EDIT);
                Uri contactUri = phoneUtil.retrieveContactUri(itemAtPosition);
                editContact.setData(contactUri);

                // start editor, refresh of listviews is handled in onActivityResult()
                startActivityForResult(editContact, ContactActions.EDIT_PHONE_CONTACT.ordinal());                
            }
        });
        simView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            final String TAG2 = TAG+" - simView - " + OnCreateContextMenuListener.class.getSimpleName();
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                if(Log.isLoggable(TAG, Log.DEBUG))
                    Log.d(TAG2, "onCreateContextMenu()");
                if(Log.isLoggable(TAG, Log.VERBOSE))
                    Log.v(TAG2, " ContextMenu("+menu+"), View("+v+"), ContextMenuInfo("+menuInfo+")");
                
                menu.setHeaderTitle(getString(R.string.context_header_sim));
                menu.add(0, ContactActions.COPY_SIM_CONTACT.ordinal(), 0, getString(R.string.context_entry_copy_to_phone));
                menu.add(0, ContactActions.DELETE_SIM_CONTACT.ordinal(), 0, getString(R.string.context_entry_delete_from_sim));
            }
        });
    }

    /**
     * Custom adapter which displays the contacts name and number in the listview
     */
    class ContactRowAdapter extends BaseAdapter {
        List<Contact> contacts;
        protected LayoutInflater inflater;
        
        public ContactRowAdapter(List<Contact> contacts) {
            super();
            this.contacts = contacts;
            this.inflater = getLayoutInflater();
        }
        
        public int getCount() {
            return contacts.size();
        }
        public long getItemId(int position) {
            return position;
        }
        public Object getItem(int position) {
            return contacts.get(position);
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            
            try{
                Contact contact = (Contact)this.getItem(position);
                
                ((TextView)convertView.findViewById(android.R.id.text1)).setText(contact.name);
                ((TextView)convertView.findViewById(android.R.id.text2)).setText(contact.number);
            } catch(Exception e) {
                e.printStackTrace();
                ((TextView)convertView.findViewById(android.R.id.text1)).setText("");
                ((TextView)convertView.findViewById(android.R.id.text2)).setText("");
            }
            
            return convertView;
        }
    }    
}