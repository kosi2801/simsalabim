package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;

public class SimUtil {
    private ContentResolver resolver;
    private Uri simUri;
    
    private String CONTACT_SIM_URI = "content://icc/adn"; // URI for SIM card is different on Android 1.5 and 1.6, will be detected upon load of class
    
    SimUtil(ContentResolver resolver) {
        this.resolver = resolver;
        
        CONTACT_SIM_URI = detectSimUri();
        simUri = Uri.parse(CONTACT_SIM_URI);
    }

    private String detectSimUri() {
        Uri uri15 = Uri.parse("content://sim/adn/"); // URI of Sim card on Android 1.5
//        Uri uri16 = Uri.parse("content://icc/adn/"); // URI of Sim card on Android 1.6
        
        // resolve something from Sim card
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
    
    public ArrayList<Contact> retrieveSIMContacts() {
        // get these columns
        final String[] simProjection  = new String[] { //
                android.provider.Contacts.PeopleColumns.NAME, //
                android.provider.Contacts.PhonesColumns.NUMBER, //
                android.provider.BaseColumns._ID }; 
        
        Cursor results = resolver.query( //
                simUri,                   // URI of contacts on SIM card
                simProjection,           // get above defined columns
                null, null, 
                android.provider.Contacts.PeopleColumns.NAME); // order by name

        // create array of SIM contacts and fill it
        final ArrayList<Contact> simContacts = new ArrayList<Contact>(results.getCount());
        while (results.moveToNext()) {
            final Contact simContact = new Contact(// 
                results.getString(2), // _id
                results.getString(0), // name
                results.getString(1));// number

            simContacts.add(simContact);
        }
        return simContacts;
    }
    
    /**
     * 
     * @param context
     * @param newSimContact
     * @return Uri of the newly created contact
     */
    public Uri createContact(Contact newSimContact) {
        // add it on the SIM card
        ContentValues newSimValues = new ContentValues();
        newSimValues.put("tag", newSimContact.getSimName());
        newSimValues.put("number", newSimContact.number);
        Uri newSimRow = resolver.insert(simUri, newSimValues);
        
        // TODO: further row values: "AdnFull", "/adn/0"
        // TODO: null could also mean that the contact name was too long?
        return newSimRow;
    }

    public int deleteContact(Contact contact) {
        // TODO: check, that only 1 matching contact before deleting
        int count = resolver.delete(simUri, "tag='"+contact.name+"' AND number='"+contact.number+"'", null);
        
        return count;
    }
}
