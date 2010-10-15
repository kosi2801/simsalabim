package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.*;
import android.provider.Contacts.People;
import android.util.Log;

public class PhoneUtilCupcake extends PhoneUtil {
    private static final String TAG = PhoneUtilCupcake.class.getSimpleName();
    
    private ContentResolver resolver;
    
    PhoneUtilCupcake(final ContentResolver resolver) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "PhoneUtil(ContentResolver)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " ContentResolver("+resolver+")");
        
        this.resolver = resolver;
    }

    /* (non-Javadoc)
     * @see at.fhj.swd07.simsalabim.PhoneInterface#retrievePhoneContacts()
     */
    public ArrayList<Contact> retrievePhoneContacts() {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "retrievePhoneContacts()");
        
        // get these columns from the content provider
//        final String[] phoneProjection  = new String[] { //
//                android.provider.Contacts.PeopleColumns.NAME, //
//                android.provider.Contacts.PhonesColumns.NUMBER, //
//                android.provider.BaseColumns._ID };        
        
        // resolve all phone numbers
//        Cursor results = resolver.query( //
//                Contacts.Phones.CONTENT_URI, // URI of contacts with phone numbers 
//                phoneProjection,             // get above defined columns
//                null, null,                  //
//                android.provider.Contacts.PeopleColumns.NAME); // sort by name

        // get these columns from the content provider
      final String[] phoneProjection  = new String[] { //
              People.NAME, //
              People.NUMBER, //
              People._ID };        
      
      // resolve all phone numbers
      Cursor results = resolver.query( //
              People.CONTENT_URI, // URI of contacts with phone numbers 
              phoneProjection,             // get above defined columns
              null, null,                  //
              People.NAME); // sort by name
        
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
    
    /* (non-Javadoc)
     * @see at.fhj.swd07.simsalabim.PhoneInterface#createContact(at.fhj.swd07.simsalabim.Contact)
     */
    public Uri createContact(Contact newPhoneContact) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "createContact(Contact)");
        if(Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " Contact("+newPhoneContact+")");
        
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
    
    /* (non-Javadoc)
     * @see at.fhj.swd07.simsalabim.PhoneInterface#retrieveContactUri(at.fhj.swd07.simsalabim.Contact)
     */
    public Uri retrieveContactUri(Contact contact) {
        if(Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "retrieveContactUri()");
        if(Log.isLoggable(TAG, Log.VERBOSE))        
            Log.v(TAG, " Contact("+contact+")");

        // try to resolve using the Contacts.Phones id
        if (contact.id != null) {
            Uri uri = ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI, Long.valueOf(contact.id));

            Cursor result = resolver.query(uri, new String[] { Contacts.Phones.PERSON_ID }, null, null, null);
            if (result.getCount() != 1) {
                return null;
            }
        }
        
        // otherwise resolve by name and number
        Cursor result = resolver.query(Contacts.Phones.CONTENT_URI, new String[] { Contacts.Phones.PERSON_ID }, "name='" + contact.name + "' AND number='"
                + contact.number + "'", null, null);
        if (result.getCount() != 1) {
            return null;
        }
        
        result.moveToNext();
        return Uri.withAppendedPath(Contacts.People.CONTENT_URI, result.getString(0));
    }
}
