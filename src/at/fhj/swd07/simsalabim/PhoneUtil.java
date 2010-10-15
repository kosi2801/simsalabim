package at.fhj.swd07.simsalabim;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;

public abstract class PhoneUtil {

    private static PhoneUtil instance;
    
    public static PhoneUtil getInstance(final ContentResolver resolver) {
        if(instance != null) {
            return instance;
        }
        
        if (Integer.parseInt(Build.VERSION.SDK) <= 4)
            instance = new PhoneUtilCupcake(resolver);
        else
            instance = new PhoneUtilDonut(resolver);
        
        return instance;
    }
    
    /**
     * Retrieves all contacts from the phones contact list.
     * 
     * @return List containing all Contact objects from the phones contact list
     */
    public abstract ArrayList<Contact> retrievePhoneContacts();

    /**
     * Creates a contact on the phone.
     * 
     * @param newPhoneContact The Contact object containing the name and number of the contact
     * @return the Uri of the newly created contact
     */
    public abstract Uri createContact(Contact newPhoneContact);

    /**
     * Retrieves the Uri to the contact using the Contacts.Phones path. If the given Contact contains an ID it will be
     * assumed that it's an ID from the Contacts.Phones path and used. If no ID is present, the name and number is used
     * to resolve the first matching contact. 
     * 
     * @param contact The contact containing the required information to be able to resolve the Uri
     * @return the Uri of the found contact or null if none found 
     */
    public abstract Uri retrieveContactUri(Contact contact);

}