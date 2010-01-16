package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;

public class PhoneUtil {
    private ContentResolver resolver;
    
    
    PhoneUtil(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public ArrayList<Contact> retrievePhoneContacts() {
        // get these columns from the content provider
        final String[] phoneProjection  = new String[] { //
                android.provider.Contacts.PeopleColumns.NAME, //
                android.provider.Contacts.PhonesColumns.NUMBER, //
                android.provider.BaseColumns._ID };        
        
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
    
    /**
     * 
     * @param context
     * @param newPhoneContact
     * @return Uri of the newly created contact
     */
    public Uri createContact(Contact newPhoneContact) {
        // first, we have to create the contact
        ContentValues newPhoneValues = new ContentValues();
        newPhoneValues.put(Contacts.People.NAME, newPhoneContact.name);
        Uri newPhoneRow = resolver.insert(Contacts.People.CONTENT_URI, newPhoneValues);
        
        if(newPhoneContact == null) {
            return null;
        }
        
        // then we have to add a number
        newPhoneValues.clear();
        newPhoneValues.put(Contacts.People.Phones.TYPE, Contacts.People.Phones.TYPE_MOBILE);
        newPhoneValues.put(Contacts.Phones.NUMBER, newPhoneContact.number);
        // insert the new phone number in the database using the returned uri from creating the contact
        newPhoneRow = resolver.insert(Uri.withAppendedPath(newPhoneRow, Contacts.People.Phones.CONTENT_DIRECTORY), newPhoneValues);
        
        return newPhoneRow;
    }
}
