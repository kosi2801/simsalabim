package at.fhj.swd07.simsalabim;

import java.util.List;

import android.app.*;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
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
    private List<Contact> phoneContacts;
    private List<Contact> simContacts;
    
    private SimUtil simUtil;
    private PhoneUtil phoneUtil;
    
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
        phoneUtil = new PhoneUtil(getContentResolver());
        
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
    public boolean onOptionsItemSelected(MenuItem item) {
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
                 
                 // was conversion successful? could fail if SIM full and maxContactNameLength couldn't be detected
                 if(newSimContact == null) {
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.error_sim_contact_conversion_failed), Toast.LENGTH_SHORT).show();
                     return true; // true means, event has been handled
                 }
                 
                 // check, if already present on SIM
                 if(simContacts.contains(newSimContact)) {
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
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.confirm_sim_contact_stored) + " " + newSimRow.toString(), Toast.LENGTH_SHORT).show();
                 }
                 
                 // add it to the simContacts list
                 simContacts.add(0, newSimContact);

                 refreshListViews();
                 return true; // true means, event has been handled
             case COPY_SIM_CONTACT:
                 // get selected contact from simView
                 contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);
                 
                 // create new instance for Phone insertion (must have empty id)
                 Contact newPhoneContact = new Contact("", contexedContact.name, contexedContact.number);
                 
                 // check, if already present on SIM
                 if(phoneContacts.contains(newPhoneContact)) {
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_contact_already_present), Toast.LENGTH_LONG).show();
                     
                     return true; // true means, event has been handled
                 }

                 // create contact on phone
                 Uri newContactUri = phoneUtil.createContact(contexedContact);
                 
                 // if NULL returned, the contact could not be created
                 if(newPhoneContact == null) {
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_contact_not_stored), Toast.LENGTH_LONG).show();
                     return true; // true means, event has been handled
                 }
                 
                 // if contacts uri returned, there was an error with adding the number
                 if(newContactUri.getPath().contains("people")) {
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_number_not_stored), Toast.LENGTH_LONG).show();
                     return true; // true means, event has been handled
                 }
                 
                 // if phone uri returned, everything went OK
                 if(newContactUri.getPath().contains("phones")) {
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.confirm_phone_contact_number_stored) + " " + newContactUri.toString(), Toast.LENGTH_SHORT).show();
                 } else {
                     // some unknown error has happened
                     Toast.makeText(ManageContactsActivity.this, getString(R.string.error_phone_number_error) + " " + newContactUri.toString(), Toast.LENGTH_LONG).show();
                     return true; // true means, event has been handled
                 }
                  
                 // finally add it to the phoneContacts list
                 phoneContacts.add(0, contexedContact);

                 refreshListViews();
                 return true; /* true means: "we handled the event". */
             case DELETE_SIM_CONTACT:
                 // get selected contact from phoneView
                 contexedContact = (Contact) simView.getAdapter().getItem(menuInfo.position);
                 
                 // make "Are you sure?"-Dialog handler
                 class DeleteHandler implements DialogInterface.OnClickListener {
                     private Contact contact;
                     public DeleteHandler(Contact contact) {
                         this.contact = contact;
                    }
                     
                     @Override
                    public void onClick(DialogInterface dialog, int which) {
                         int status = simUtil.deleteContact(contact);

                         // contact removed correctly
                         if(status == 0) {
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
                         refreshListViews();
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
        phoneContacts = phoneUtil.retrievePhoneContacts();
        simContacts = simUtil.retrieveSIMContacts(); 
        
        // set up contents of ListViews
        refreshListViews();
        
        // Set up listeners for Context-Menus on ListViews
        // if long-clicking, display a context-menu with further actions
        phoneView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.setHeaderTitle(getString(R.string.context_header_phone));
                menu.add(0, ContactActions.COPY_PHONE_CONTACT.ordinal(), 0, getString(R.string.context_entry_copy_to_sim));
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
                menu.setHeaderTitle(getString(R.string.context_header_sim));
                menu.add(0, ContactActions.COPY_SIM_CONTACT.ordinal(), 0, getString(R.string.context_entry_copy_to_phone));
                menu.add(0, ContactActions.DELETE_SIM_CONTACT.ordinal(), 0, getString(R.string.context_entry_delete_from_sim));
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
}