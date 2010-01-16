package at.fhj.swd07.simsalabim;

import java.util.*;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;

public class SimUtil {
    private ContentResolver resolver;
    private Uri simUri;
    
    private Integer maxContactNameLength; // Maximum length of contact names may differ from SIM to SIM, will be detected upon load of class
    
    SimUtil(ContentResolver resolver) {
        this.resolver = resolver;
        
     // URI for SIM card is different on Android 1.5 and 1.6
        simUri = Uri.parse(detectSimUri()); 
        maxContactNameLength = detectMaxContactNameLength();
    }

    /**
     * Detects the URI identifier for accessing the SIM card. Is different, depending on
     * Android version.
     *  
     * @return Uri of the SIM card on this system
     */
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

    /**
     * Detects the maximum length of a contacts name which is accepted by the SIM card.
     *  
     * @return Length of the longest contact name the SIM card accepts 
     */
    private Integer detectMaxContactNameLength() {
        String nameString = "sImSaLabiMXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"; // 51 chars
        Integer currentMax = nameString.length();
        
        // used for test-creation
        Uri createdUri = null;
        Contact testContact = null;
        
        // loop from longest to shortest contact name length until a contact was stored successfully
        for(currentMax = nameString.length(); ((createdUri == null) && currentMax > 0); currentMax--) {
            testContact = new Contact(null, nameString.substring(0, currentMax), "74672522246");
            createdUri = createContact(testContact);
        }
        
        // if not stored successfully
        if((null == createdUri) || (!createdUri.toString().contains("/adn/0"))) {
            return null;
        }
        
        // if stored successfully, remove contact again
        deleteContact(testContact);
        
        return currentMax;
    }
        
    /**
     * Retrieves all contacts from the SIM card.
     * 
     * @return List containing Contact objects from the stored SIM information
     */
    public List<Contact> retrieveSIMContacts() {
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
     * Creates a contact on the SIM card.
     * 
     * @param newSimContact The Contact object containing the name and number of the contact
     * @return the Uri of the newly created contact, "AdnFull" if there was no more space left on 
     *         the SIM card or null if an error occured (ie. the contact name was too long for the SIM).
     */
    public Uri createContact(Contact newSimContact) {
        // add it on the SIM card
        ContentValues newSimValues = new ContentValues();
        newSimValues.put("tag", newSimContact.name);
        newSimValues.put("number", newSimContact.number);
        Uri newSimRow = resolver.insert(simUri, newSimValues);
        
        // TODO: further row values: "AdnFull", "/adn/0"
        // TODO: null could also mean that the contact name was too long?
        return newSimRow;
    }

    /**
     * Delete a contact on the SIM card. Will only be removed if identified uniquely. Identification happens 
     * on the contact.name and contact.number attributes.
     * 
     * @param contact The contact to delete.
     * @return 0 if the contact was deleted, -1 if an error happened during deletion and nothing was deleted, 
     *         otherwise the number of multiple contacts which were identified. 
     */
    public int deleteContact(Contact contact) {
        // check, that only one contact with this identifiers existing
        // TODO: currently this always returns ALL contacts on the SIM. Is this a bug in the content provider?
//        Cursor results = resolver.query(simUri, new String[]{android.provider.BaseColumns._ID}, "tag='"+contact.name+"' AND number='"+contact.number+"'", null, null);
//        int rowCount = results.getCount();
//        if (rowCount > 1) {
//            return rowCount;
//        }
        
        // TODO: Can this ever return >1 after check above?
        int deleteCount = resolver.delete(simUri, "tag='"+contact.name+"' AND number='"+contact.number+"'", null);
        if(deleteCount != 1) {
            return -1;
        }
        
        return 0;
    }
    
    /**
     * Converts a contact to a SIM card conforming contact by stripping the name to the maximum allowed length and setting ID to null.
     * 
     * @param contact The contact to convert to SIM conforming values 
     * @return a contact which does not contain values which exceed the SIM cards limits or null if there was a problem detecting the limits
     */
    public Contact convertToSimContact(Contact contact){
        // if no max length yet, try to detect once more
        if(maxContactNameLength == null) {
            maxContactNameLength = detectMaxContactNameLength();
            
            // if still null, give up
            if(maxContactNameLength == null) {
                return null;
            }
        }
        
        // convert and return
        return new Contact(null, contact.name.substring(0, Math.min(contact.name.length(), maxContactNameLength)), contact.number);
    }
}
