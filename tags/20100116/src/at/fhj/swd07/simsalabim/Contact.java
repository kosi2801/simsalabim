package at.fhj.swd07.simsalabim;

/** small class which represents a contact on any view */
public class Contact {
    protected String name;
    protected String number;
    protected String id;
    
    protected Contact(String id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    /** The ListView is going to display the toString() return-value! 
     * @return the string for the Listview */
    @Override
    public String toString() {
         return name;
    }

    // null-safe string compare
    public boolean compareStrings(final String one, final String two) {
        if (one == null ^ two == null) {
            return false;
        }
        if (one == null && two == null) {
            return true;
        }
        return one.compareTo(two) == 0;
    }

    @Override
    public boolean equals(Object o) {
        // if not Contact, can't be true
        if(!(o instanceof Contact)) 
            return false;
        Contact c = (Contact)o;
        
        // only if id's present, compare them
        if((id != null) && (id.length()) > 0 && (c.id.length() > 0))
            return c.id.compareTo(id) == 0;
        
        // if SimNames not equal...
        if(compareStrings(name, c.name) == false) {
            return false;
        }

        // finally if numbers not equal...
        return compareStrings(number, c.number);
    }
}