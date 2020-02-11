These Android tests have been divided in sections: 
* Setup
* Detection
* Payment INPUT
* Payment REQUEST
* Settings
* Transactions
* Navigation 
* Compatibility

Some tests requires a fresh install without data from the previous installation. 
This requires to both uninstall the app and to clear the app cache.
* Long-press app icon to invoke a contextual menu
* Click 'App info'
* Click 'Force stop' to kill the app 
* Click 'Storage'
* Click 'Clear data' AFTER ensuring that the app is not running otherwise it is useless.
* Click 'Uninstall' app
* Now, all data from previous installation are gone 

#### Setup
1. Setting initial PIN-CODE & Verify user can not leave settings until payment target is set
   * Force stop + Clear data + Uninstall app
   * Install & Launch app
   * Confirm that it opens on 'PIN code' screen to create passcode
   * Enter Pin code: 1111
   * Enter Pin code: 2222 to fail confirmation
   * Confirm that an error message appears similar to "PIN code must match"
   * Enter Pin code: 1111
   * Enter Pin code: 1111 to confirm
   * Confirm that the current screen is the 'Settings' screen
   * Press the device BACK button
   * Confirm that an error message prevent from leaving the 'Settings' screen and click OK
   * Press the toolbar top left BACK button
   * Confirm that an error message prevent from leaving the 'Settings' screen and click OK
   * Click the 'Overview' button (OS button) representing a square: It opens a list of thumbnail images of apps.
   * Flick the app or Click 'X' button to kill it or 'Force stop' the app
   * Launch app again
   * Confirm that it opens on the 'Settings' screen
    
#### Detection    
1. Verify language & currency are set based on OS language & country
   * Force stop + Clear data + Uninstall app
   * Ensure that the default OS & device language is ENGLISH  
   * Install & Launch app
   * Confirm that texts are in 'ENGLISH' language  
   * Enter Pin code: 1111
   * Enter Pin code: 1111 to confirm
   * Confirm that Local Currency is 'USD'
   * Put app in background 
   * Add 2nd language of your choice (here French is used)
     * Click device 'Settings' button (usually a gear icon)
     * Click 'System' 
     * Click 'Languages & Input' 
     * Click 'Languages'
     * Click '+ Add language'
     * Select 'Francais' then 'France'
     * Move 'Francais (France)' to the top
     * This will change the OS language to 'Francais' (French)
   * Force stop + Clear data + Uninstall app
   * Install & Launch app
   * Confirm that texts are in 'FRENCH' language  
   * Confirm that Local Currency is 'EUR'
   
#### Payment INPUT: before any test, launch app & do required setup     
1. Verify that amount of 0 are forbidden 
   * Go to 'Payment INPUT' screen
   * Ensure that the amount is 0
   * Click on 'Check-out'
   * Confirm that a temporary error message appears in RED notifying that the amount is invalid
1. USD CURRENCY formatting & keyboard
   * Go to 'Settings' screen
   * Select country: US with currency: USD
   * Go to 'Payment INPUT' screen
   * Enter the amount of 1234,56
   * Confirm that the decimal button ',' on the left of button '0' is NOT greyed and works
   * Confirm that it doesn't introduce a 3rd decimal when pressing any digit
   * Click on 'Check-out'
   * Confirm that the amount is displayed EXACTLY as $1,234.56
   * Click 'Cancel' to return to the 'Payment INPUT' screen
   * Enter the amount of 0,99
   * Click on 'Check-out'
   * Configure your wallet to use US currency: USD
   * Scan the QR code with your wallet and verify same exact amount is displayed on your wallet $0.99
1. JPY CURRENCY formatting & keyboard
   * Go to 'Settings' screen
   * Select country: Japan with currency: JPY
   * Go to 'Payment INPUT' screen
   * Enter the amount of 1234
   * Confirm that the decimal button ',' on the left of button '0' is greyed and does nothing
   * Click on 'Check-out'
   * Confirm that the amount is displayed EXACTLY as ¥1,234
   * Click 'Cancel' to return to the 'Payment INPUT' screen
   * Enter the amount of 95 JPY
   * Click on 'Check-out'
   * Configure your wallet to use Japan currency: JPY
   * Scan the QR code with your wallet and verify same exact amount is displayed on your wallet 95 JPY
1. EUR CURRENCY formatting & keyboard
   * Go to 'Settings' screen
   * Select country: France with currency: EUR
   * Go to 'Payment INPUT' screen
   * Enter the amount of 1234,56
   * Confirm that the decimal button ',' on the left of button '0' is NOT greyed and works
   * Confirm that it doesn't introduce a 3rd decimal when pressing any digit
   * Click on 'Check-out'
   * Confirm that the amount is displayed EXACTLY as 1.234,56 € (Notice inversion of ./, compared to USD)
   * Click 'Cancel' to return to the 'Payment INPUT' screen
   * Enter the amount of 0,50 EUR
   * Click on 'Check-out'
   * Configure your wallet to use France currency: EUR
   * Scan the QR code with your wallet and verify same exact amount is displayed on your wallet 0,50 EUR
1. JOD CURRENCY formatting & keyboard
   * Go to 'Settings' screen
   * Select country: Jordan with currency: JOD
   * Go to 'Payment INPUT' screen
   * Enter the amount of 1234,567
   * Confirm that the decimal button ',' on the left of button '0' is NOT greyed and works
   * Confirm that it doesn't introduce a 4rd decimal when pressing any digit
   * Click on 'Check-out'
   * Confirm that the amount is displayed as 1234,567 (it will varies depending on how the phone supports Arabic)
   * Click 'Cancel' to return to the 'Payment INPUT' screen
   * Enter the amount of 0,349 JOD
   * Click on 'Check-out'
   * Configure your wallet to use Jordan currency: JOD
   * Scan the QR code with your wallet and verify same exact amount is displayed on your wallet 0,349 JOD

#### Payment REQUEST: before any test, launch app & do required setup    
1. create invoice shows QR code & correct FIAT amount 
   * Enter 5.00 USD on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Verify the amount label at the top of the 'Payment REQUEST' screen matches the amount you entered in Step 1. (5.00 USD)
1. Verify CHECKMARK & SOUND after payment is sent
   * Make sure your device's volume is at the MAX setting
   * Enter any amount on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Grab a wallet of your choice on a separate device and pay the invoice by scanning the QR code
   * Pay the invoice by scanning the displayed QR code using any wallet supporting BIP-70
   * Confirm that the screen shows a white CheckMark with a green background
   * Confirm that you hear noticeable ding sound
   * After returning to the 'Payment INPUT' screen, confirm that the amount is reset to 0
1. Invoice generation continue after internet disconnection
   * Turn off internet connection on the merchant device.
   * Enter any amount on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Upon seeing the "Error during invoice generation" popup, do not press continue.
   * Swipe down on your Android device to bring the notification shade into view.
   * Turn on your internet connection
   * Wait for the internet connection to be established
   * Press continue on the "Error during invoice generation" popup
   * The invoice should now be properly generated.
1. Automatic RESUME AFTER CRASH
   * Enter 8 USD on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Kill/Force close the app
   * Launch the app again 
   * Confirm that the app opens on the 'Payment REQUEST' screen with an amount of 8 USD
1. Automatic RECONNECT after internet disconnection
   * Enter any amount on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Confirm connection icon is green/Connected
   * Swipe down on your Android device to bring the notification shade into view.
   * Turn off your internet connection
   * Wait 5 seconds
   * Confirm connection icon is red/Disconnected
   * Repeat Step 4
   * Turn on your internet connection
   * Wait for the internet connection to be established
   * Confirm connection icon is green/Connected
   * Pay the invoice by scanning the displayed QR code using any wallet supporting BIP-70
   * Confirm that the screen shows a CheckMark upon payment.
1. Verify invoice CANCELLATION doesn't crash
   * enter any amount on the 'Payment INPUT' screen
   * Click the 'CHARGE' button to enter 'Payment REQUEST' screen
   * Click the 'CANCEL' button button to exit 'Payment REQUEST' screen
   * Wait 3 seconds and ensure that the app did not crash
   * Verify amount has been properly reset to 0 on the 'Payment INPUT' screen
1. Verify auto-cancellation after TIMER EXPIRY
   * Enter any amount on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Wait for the timer below the QR code to reach 0:00
   * Make sure the 'Payment REQUEST' screen closes itself when the timer reaches 0:00
1. Verify that 2 payments using xPubKey ends up in 2 different addresses 
   * Enter any amount on the 'Payment INPUT' screen
   * Click the "Charge" button to enter the 'Payment REQUEST' screen
   * Pay the invoice
   * Go to 'Transactions' screen
   * View the transaction on the block explorer
   * Make note of the address in the output belonging to you
   * Repeat Steps 1 through 6
   * Compare the addresses and confirm they do NOT match

#### Settings    
1. Verify that access to 'Settings' screen is SECURED BY PIN code
   * Launch app & do required setup (setting PING code + target payment address)
   * Go to 'Settings' screen
   * Confirm that it requires to enter PIN code before accessing the screen
   * Confirm that entering an incorrect PIN code shows an error message and denies access to the screen
   * Confirm that entering the correct PIN code grants access to the screen
1. Verify PIN-CODE change
   * Launch app & do required setup
   * Click on 'Settings' screen
   * Enter the correct PIN code to pass 'PIN code' screen and enter the 'Settings' screen
   * Click on 'PIN Code' button to change the PIN code
   * Enter a NEW PIN code (like 1234)
   * Enter a NON matching confirmation PIN code (like 5678)
   * Confirm that an error message says that the 2 codes are not matching
   * Enter a NEW PIN code (like 3333)
   * Enter a MATCHING confirmation PIN code (like 3333)
   * Confirm that it goes back the 'Settings' screen
   * Click 'BACK' to exit the 'Settings' screen
   * Go to 'Settings' screen
   * Confirm that the new PIN code is required to enter the screen
1. changing COMPANY/MERCHANT name
   * Launch app & do required setup
   * Click on 'Settings' screen
   * Enter the correct PIN code to pass 'PIN code' screen and enter the 'Settings' screen
   * Click on 'Merchant name' button to change the name displayed in the drawer or left side menu
   * Enter a new name and Click on 'Cancel'
   * Confirm that the name has not changed
   * Click on 'Merchant name' button to change the name displayed in the drawer or left side menu
   * Enter a new name and Click on 'OK'
   * Confirm that the name has changed on the 'Settings' screen
   * Kill the app, Relaunch it
   * Confirm that change has been kept in the 'Settings' screen
1. Paste value in payment target and cancel
   * Launch app & do required setup
   * Enter 'Settings' screen
   * Click on 'Destination address' button
   * Paste a cash address like this one: bitcoincash:qzg0jqca4c38uzmkqlqwqgnpemdup9u8hsjyvyc0tz
   * Click 'Cancel' button
1. Paste INVALID value in payment target using: RANDOM data
   * Launch app & do required setup
   * Enter 'Settings' screen
   * Click on 'Destination address' button
   * Click on 'Paste' button
   * Paste invalid value:  0123456
   * Click 'OK' button 
   * Confirm that it fails with an ERROR message
1. Paste INVALID value in payment target using: TEST address
   * Similar to previous test with input:
   *      bchtest:qzgmyjle755g2v5kptrg02asx5f8k8fg55xlze46jr
   * Confirm that it fails with an ERROR message
1. Paste INVALID value in payment target using: TEST address without prefix
   * Similar to previous test with input:
   *      qzgmyjle755g2v5kptrg02asx5f8k8fg55xlze46jr
   * Confirm that it fails with an ERROR message
1. Paste INVALID value in payment target using: TEST address LEGACY
   * Similar to previous test with input:
   *      mtoKs9V381UAhUia3d7Vb9GNak8Qvmcsme
   * Confirm that it does NOT WORK with it
1. Paste payment target using: PubKey/BCH format with "bitcoincash:" prefix
   * Launch app & do required setup
   * Enter 'Settings' screen
   * Click on 'Destination address' button
   * Click on 'Paste' button
   * Paste a valid cash address like this one:
   *      bitcoincash:qzg0jqca4c38uzmkqlqwqgnpemdup9u8hsjyvyc0tz
   *  OR  bitcoincash:qrjautd36xzp2gm9phrgthal4fjp7e6ckcmmajrkcc
   * Click 'OK' button
   * Confirm that the address has been changed and that there is a success notification
1. Paste payment target using: PubKey/BCH format WITHOUT "bitcoincash:" prefix
   * Same as previous test with input:
   *      qzg0jqca4c38uzmkqlqwqgnpemdup9u8hsjyvyc0tz
   *  OR  qrjautd36xzp2gm9phrgthal4fjp7e6ckcmmajrkcc
   * Confirm that the address has been changed and that there is a success notification
1. Paste payment target using: PubKey using LEGACY format
   * Same as previous test with input:
   *      1EDYcHyFgvFm9ZGqdLwjKxZtZUph5i7EQq
   *  OR  1MxRuANd5CmHWcveTwQaAJ36sStEQ5QM5k
   * Confirm that the address has been changed and that there is a success notification
1. Paste payment target using: P2SH
   * Same as previous test with input:
   *      3CSUDH5yW1KHJmMDHfCCWShWgJkbVnfvnJ (legacy)
   *  OR  bitcoincash:pp67j94cfvnfg727etymlst9jts3uhfdkurqvtj2un (cashaddress)
   * Confirm that the address has been changed and that there is a success notification
1. Paste payment target using: API KEY
   * Same as previous test with input:
   *      dtgmfljtkcbwwvkbegpakhwseymimpalanmqjtae
   *  OR  bvcdndeyaropfdlcjeutwghghkyuomespvrctayf
   * Confirm that the address has been changed and that there is a success notification
1. Paste payment target using: xPubKey
   * Same as previous test with input:
   *      xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz
   * Confirm that the address has been changed and that there are 2 success notifications: "syncing xpub" + "xpub synced"
1. Camera to scan payment target using address: PubKey
   * Launch app & do required setup
   * Enter 'Settings' screen
   * Click on 'Destination address' button
   * Click on 'Scan' button
   * Scan one of the following QR code generated from http://goqr.me/
   *      bitcoincash:qzg0jqca4c38uzmkqlqwqgnpemdup9u8hsjyvyc0tz
   *  OR  bitcoincash:qrjautd36xzp2gm9phrgthal4fjp7e6ckcmmajrkcc
   *  OR  qzg0jqca4c38uzmkqlqwqgnpemdup9u8hsjyvyc0tz
   *  OR  qrjautd36xzp2gm9phrgthal4fjp7e6ckcmmajrkcc
   *  OR  1EDYcHyFgvFm9ZGqdLwjKxZtZUph5i7EQq
   *  OR  1MxRuANd5CmHWcveTwQaAJ36sStEQ5QM5k
   * Click 'OK' button
   * Confirm that the address has been changed and that there is a success notification
1. Camera to scan payment target using address: P2SH
   * Same as previous test with input:
   *      3CSUDH5yW1KHJmMDHfCCWShWgJkbVnfvnJ (legacy)
   *  OR  bitcoincash:pp67j94cfvnfg727etymlst9jts3uhfdkurqvtj2un (cashaddress)
   * Confirm that the address has been changed and that there is a success notification
1. Camera to scan payment target using address: API KEY
   * Same as previous test with input:
   *      dtgmfljtkcbwwvkbegpakhwseymimpalanmqjtae
   *  OR  bvcdndeyaropfdlcjeutwghghkyuomespvrctayf
   * Confirm that the address has been changed and that there is a success notification
1. Camera to scan payment target using address: xPubKey
   * Same as previous test with input:
   *      xpub6CUGRUonZSQ4TWtTMmzXdrXDtypWKiKrhko4egpiMZbpiaQL2jkwSB1icqYh2cfDfVxdx4df189oLKnC5fSwqPfgyP3hooxujYzAu3fDVmz
   * Confirm that the address has been changed and that there are 2 success notifications: "syncing xpub" + "xpub synced"

#### Transactions
1. Verify empty TX list
   * Force stop + Clear data + Uninstall app
   * Install & launch app & do required setup
   * Go to Transactions screen
   * Ensure that the list is empty and display a message saying that there is no TX available
1. Verify newly paid invoice is added to the list
   * Go to 'Payment INPUT' screen
   * Enter the amount USD 0,05
   * Click Charge to generate an invoice/QR code
   * Pay the invoice
   * Go to Transactions screen
   * Ensure that the amount is correctly displayed as USD 0,05 (dollar sign is OK)
   * Ensure that the timestamp is correct
   * Ensure that the BCH amount seems correct
1. Verify TX list is populated correctly
   * **NOT READY** - This test requires the deployment of the new Bitcoin Pay Console
   * Force stop + Clear data + Uninstall app
   * Install & launchapp & do required setup using a payment target that has paid invoices in the past 
   * Go to Transactions screen
   * Pull down to refresh the TX list
   * Ensure that the past invoices are downloaded & shown  
1. Verify TX id can be copied to clipboard
   * Prerequisite: TX must be visible in the Transactions screen 
   * Go to Transactions screen
   * Click on "Copy Transaction"
   * Open a browser
   * Click on the address bar
   * Paste the clipboard to ensure that it contains the expected TX id
1. Verify TX address can be copied to clipboard   
   * Prerequisite: TX must be visible in the Transactions screen 
   * Go to Transactions screen
   * Click on "Copy address"
   * Open a browser
   * Click on the address bar
   * Paste the clipboard to ensure that it contains the expected address
1. Verify TX can be viewed in blockchain explorer webpage   
   * Prerequisite: TX must be visible in the Transactions screen 
   * Go to Transactions screen
   * Click on a TX and selet "View Transaction"
   * Confirm that it opens a browser and display info about the same TX

#### Navigation
1. ABOUT: navigate to screen
   * Launch app & do required setup
   * Go to 'About' screen
   * Confirm that it displays app name "Bitcoin Cash Register", version & year
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'About' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. PRIVACY POLICY: navigate to screen
   * Launch app & do required setup
   * Go to 'Privacy Policy' screen
   * Confirm that it displays the correct content or web page 
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'Privacy Policy' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. SERVICE TERMS: navigate to screen
   * **NOT READY** webpage url is NOT yet defined
   * Launch app & do required setup
   * Go to 'Service Terms' screen
   * Confirm that it displays the 'Service Terms' web page 
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'Service Terms' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. TERMS OF USE: navigate to screen
   * Launch app & do required setup
   * Go to 'Terms of use' screen
   * Confirm that it displays the 'Terms of use' web page 
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'Terms of use' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. TRANSACTIONS: navigate to screen 
   * Launch app & do required setup
   * Go to 'Transactions' screen
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'Transactions' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. SETTINGS: navigate to screen
   * Launch app & do required setup for Pin code & Payment Target
   * Go to 'Settings' screen
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 0
   * Go to 'Settings' screen
   * Click 'BACK' button
   * Confirm that it goes back to the 'Payment INPUT' screen 
1. Payment INPUT: navigate to screen
   * Launch app
   * Go to 'Payment INPUT' screen
   * Enter amount: 99
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment INPUT' screen with an amount of 99
   * Click 'BACK' button
   * Confirm that it exits the app
1. Payment REQUEST: navigate to screen
   * Launch app
   * Go to 'Payment INPUT' screen
   * Enter amount: 10
   * Click charge to arrive on the 'Payment REQUEST' screen
   * Click on device 'Home' button to put the app in background
   * Resume the paused app
   * Confirm that it opens on the 'Payment REQUEST' screen with an amount of 10
   * Click 'BACK' button
   * Confirm that it does NOT go back to the 'Payment INPUT' screen and stays on the 'Payment REQUEST' screen otherwise the invoice would get lost

#### Compatibility
1. Use an Android device with OS 10
   * Go to 'Payment INPUT' screen
   * Enter a specific amount
   * Click Charge to generate an invoice/QR code
   * Pay the invoice
   * Confirm that the CheckMark is shown after the payment is sent 
1. Use an Android device with OS 9.x
   * perform same test as 'Use an Android device with OS 10' 
1. Use an Android device with OS 8.x
   * perform same test as 'Use an Android device with OS 10' 
1. Use an Android device with OS 7.x
   * perform same test as 'Use an Android device with OS 10' 
1. Use an Android device with OS 6.x
   * perform same test as 'Use an Android device with OS 10' 
1. Use an Android device with OS 5.x
   * perform same test as 'Use an Android device with OS 10' 
