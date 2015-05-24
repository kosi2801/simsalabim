# Welcome #
**SIMsalabim** is a tool to assist with the management of contacts on the SIM cards of mobile phones running Google Android.
Android does not offer a possibility to manage the contacts on your SIM card besides copying them to the phone. This tool
adds this missing functionality.

SIMsalabim allows following operations on your SIM card :
  * view the contacts
  * copy contacts to the phone contacts list
  * copy contacts from the phone contacts list
  * delete contacts

# Usage #
Upon startup of SIMsalabim you are presented with tabs containing the contact lists from your phone and your SIM card.
Contacts are displayed several times if there are several numbers assigned to it. The number of each entry is displayed below the contact name.
On the phone tab you can click an item to launch the Android contacts editor.
If you long-press an entry on the phone and sim tab the context menu for modifying the entries is displayed.

# Screenshots #
The phone tab and its context menu.

![http://simsalabim.googlecode.com/svn/trunk/website/PhoneTab.png](http://simsalabim.googlecode.com/svn/trunk/website/PhoneTab.png) ![http://simsalabim.googlecode.com/svn/trunk/website/PhoneTabMenu.png](http://simsalabim.googlecode.com/svn/trunk/website/PhoneTabMenu.png)

The sim tab and its context menu.

![http://simsalabim.googlecode.com/svn/trunk/website/SimTab.png](http://simsalabim.googlecode.com/svn/trunk/website/SimTab.png) ![http://simsalabim.googlecode.com/svn/trunk/website/SimTabMenu.png](http://simsalabim.googlecode.com/svn/trunk/website/SimTabMenu.png)

# Planned improvements #
  * don't crash, if there is a problem accessing the SIM at startup (eg. on Milestone)
  * ~~add support for different types of numbers (private, fax, etc.)~~
  * add edit-functionality for contacts/numbers on SIM
  * add debug-activity to display SIM data and statistics
  * ~~provide "letter-slider" during scrolling, like in builtin contacts manager~~
  * more testing with difficult SIM conditions, like being full etc.
  * far future: maybe allow similar management of SMS on SIM card, if Android allows this
  * ~~add support for Android 2.0+~~
