package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.util.Log;

public class PhoneUtilDonut extends PhoneUtil {
    private static final String TAG = PhoneUtilDonut.class.getSimpleName();

    private ContentResolver resolver;

    PhoneUtilDonut(ContentResolver resolver) {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "PhoneUtil(ContentResolver)");
        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " ContentResolver(" + resolver + ")");

        this.resolver = resolver;
    }

    /*
     * (non-Javadoc)
     * 
     * @see at.fhj.swd07.simsalabim.PhoneInterface#retrievePhoneContacts()
     */
    public ArrayList<Contact> retrievePhoneContacts() {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "retrievePhoneContacts()");

        // Run query
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] { PhoneLookup._ID, PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        // resolve all phone numbers
        Cursor results = resolver.query( //
                uri, //
                projection, //
                selection, //
                selectionArgs, //
                sortOrder);

        // create array of Phone contacts and fill it
        final ArrayList<Contact> phoneContacts = new ArrayList<Contact>(results.getCount());
        while (results.moveToNext()) {
            final Contact phoneContact = new Contact(// 
                    results.getString(0), // _id
                    results.getString(1), // name
                    results.getString(2));// number

            phoneContacts.add(phoneContact);
        }

        results.close();
        return phoneContacts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * at.fhj.swd07.simsalabim.PhoneInterface#createContact(at.fhj.swd07.simsalabim
     * .Contact)
     */
    public Uri createContact(Contact newPhoneContact) {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "createContact(Contact)");
        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " Contact(" + newPhoneContact + ")");

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newPhoneContact.name).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(
                        ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneContact.number).withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN).build());

        ContentProviderResult[] results;
        try {
            results = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            return results[0].uri;
        } catch (Exception e) {
            Log.e(TAG, " Exception during creation of contact. " + e.getMessage());
            return null;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * at.fhj.swd07.simsalabim.PhoneInterface#retrieveContactUri(at.fhj.swd07
     * .simsalabim.Contact)
     */
    public Uri retrieveContactUri(Contact contact) {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "retrieveContactUri()");
        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, " Contact(" + contact + ")");

        // needed stuff for URI
        String lookupKey = null;
        Long contactId = null;
        Cursor result = null;

        // uri and projection
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] { ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.RawContacts.CONTACT_ID };

        // Lookup query + alternate query
        String selection = //
            PhoneLookup._ID + "=?";
        String selectionAlternate = //
            ContactsContract.Contacts.DISPLAY_NAME + " = '" + contact.name //
                    + "' AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + contact.number + "'";

        // at first try to resolve with contacts id
        if (contact.id != null) {
            result = resolver.query(uri, projection,
                    selection, new String[] { contact.id }, null);
            
            // check if unique result
            if(result.getCount() != 1) {
                result.close();
                result = null;
            }
        }
        
        // if no contact id or no result, try alternate method
        if(result == null) {
            result = resolver.query(uri, projection,
                    selectionAlternate, null, null);
            
            // check if unique result
            if(result.getCount() != 1) {
                result.close();
                result = null;
            }
        }
                
        // check for result
        if (result == null) {
            return null;
        }
        
        // get results
        result.moveToNext();
        lookupKey = result.getString(0);
        contactId = result.getLong(1);
        result.close();

        // create contact URI
        return ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
    }
}
