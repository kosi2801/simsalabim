package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class SimUtil {
    private ContentResolver resolver;
    
    private String CONTACT_SIM_URI = "content://icc/adn"; // URI for SIM card is different on Android 1.5 and 1.6, will be detected upon load of class
    
    SimUtil(ContentResolver resolver) {
        this.resolver = resolver;
        
        CONTACT_SIM_URI = detectSimUri();
    }

    public ArrayList<Contact> retrieveSIMContacts() {
        // create URI for content provider
        Uri uri = Uri.parse(CONTACT_SIM_URI);

        // get these columns
        final String[] simProjection  = new String[] { //
                android.provider.Contacts.PeopleColumns.NAME, //
                android.provider.Contacts.PhonesColumns.NUMBER, //
                android.provider.BaseColumns._ID }; 
        
        Cursor results = resolver.query( //
                uri,                   // URI of contacts on SIM card
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
    
}
