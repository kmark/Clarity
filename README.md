# Clarity
![Clarity](https://raw.githubusercontent.com/kmark/Clarity/master/app/src/main/res/mipmap-xxhdpi/ic_launcher.png)

***Android contact thumbnails you don't mind looking at.***

> **XDA-Developers:**  [forum.xda-developers.com/xposed/modules/clarity-t3063804](http://forum.xda-developers.com/xposed/modules/clarity-contact-thumbnails-don-t-mind-t3063804)  
> **Xposed Repository:** [repo.xposed.info/module/com.versobit.kmark.clarity](http://repo.xposed.info/module/com.versobit.kmark.clarity)

Clarity is an Xposed module and root Android application for increasing the size and quality of contact thumbnails in the Android contacts database. It's comprised of two components. An Xposed modification and a root-required Database Processor.

### Xposed Mod

Android stores two versions of contact images. The *contact photo* and the *contact thumbnail*. While both are downscaled appropriately from the original image, the two are very different in size. Contact photos clock in at around 720×720 pixels or so depending on the original image. Contact thumbnails are a measly 96×96 pixels. Contact photos are used in places where contact imagery is going to be distinctively large like the KitKat/Lollipop Phone app. Or when you receive an incoming call. Thumbnails are used for notifications, messaging applications, and other roles which the full contact photo is unnecessarily big. Unfortunately, as screen PPIs have skyrocketed since the 96×96 limit was put in place, contact thumbnails are now too small for their original purpose. Regardless, most applications continue to use contact thumbnails.

Clarity forces Android to use a user-defined size (defaulting to 256×256) when adding contact thumbnails to the database. While these new images are larger and look much better they will load slightly slower and increase memory requirements. For newer devices this should not be a problem. For reference, I have not witnessed any side effects on my Galaxy S4 (CM12, Android 5.0.2) using the default 256×256 setting. Since the size can be adjusted you can optimize it for your device.

In the name of simplicity, Clarity does not change the method Android uses to downscale contact thumbnails, just the final dimensions. However since Clarity allows you to increase the size of the thumbnail and consequently decrease the magnitude of the downscale, there will be noticeably fewer artifacts in your new thumbnails.

The Xposed module does not magically increase the quality of thumbnails already added in the database. It only affects newly added/updated contacts and images. To upgrade your entire contacts database either use the Database Processor detailed below or re-import the contacts. Re-importing may require you to deconnect/desync the account the contacts are associated with and then reconnect/resync the account. Some sync applications may have a force refresh option and that should work nicely. For instance, HaxSync has a *Force redownload* option under its contact settings.

### Database Processor

In addition to the Xposed-based modification described above, Clarity includes a *Database Processor*. This powerful feature forcibly updates every contact thumbnail in your database. It rips out the internal contacts database file, finds all your contacts with photos and puts in new thumbnails. It'll then replace the old database with the new one. It is highly recommended to immediately reboot after processing to avoid Android going berserk. It will if you don't. :)

**While powerful, the Database Processor is also extremely dangerous.** I am *not* responsible for any damage to your device as a result of using Clarity. It performs several operations as root and modifies core Android files that were never meant to be touched by anything other than Android itself. I highly recommend a full recovery-based backup of your device alongside the built-in backup feature. *Please* use the dry-run feature first (hell, multiple times even) to ensure the operation will succeed for your ROM. It is possible a successful dry-run could still fail when doing the real-deal so, as I mentioned before, backup your stuff. The built-in contact backups will be saved to a *Clarity* folder on your "primary" external storage. What primary means depends on your ROM and device. It could be your actual external SD card or it could be an internal storage location. Regardless, the current backup location will be noted, assuming backups are enabled, in the processor's log. As an added bonus, if the backup feature is on it will also save the log file alongside your backups!

The Database Processor has been tested on CyanogenMod 10 (based on Android 4.1.2), CyanogenMod 11 (based on Android 4.4.4), and CyanogenMod 12 (based on Android 5.0.2). It *should* work on any AOSP-based ROM from Ice Cream Sandwich (4.0) to Lollipop MR1 (5.1). For stock ROMs and anything created by carriers I have no idea. If it works for you please report back! If it doesn't, well, report that too.

Clarity is developed in my spare time and will always remain free and open-source software. If you find this application useful please feel free to [donate](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=M9Z5Y8EF3DH7G).

## Screenshots

These comparison GIFs are useful for gauging the impressive potential quality improvements Clarity can provide.

### Raw
![Raw](https://raw.githubusercontent.com/kmark/Clarity/master/images/RawComparison.gif)  
Obtained by extracting the raw contact photo JPEG directly from the internal contacts database.

### Android Wear
![Android Wear](https://raw.githubusercontent.com/kmark/Clarity/master/images/WearComparison.gif)  
A Google Hangouts incoming SMS notification. Taken with a Moto 360 on Android 5.0.2.

### Google Hangouts
![Google Hangouts](https://raw.githubusercontent.com/kmark/Clarity/master/images/HangoutsComparison.gif)

### Google Messages
![Google Messages](https://raw.githubusercontent.com/kmark/Clarity/master/images/MessagesComparison.gif)

## Features
* Simple lightweight Xposed modification
* Powerful database processor for forcibly updating contact thumbnails
* Should work on nearly any AOSP-based ROM like CyanogenMod
* Free and open-source software. No nags. No data collection. No secrets.

## Known Issues
* Does not appear to work with Google-synced contact photos.
* May not work with all devices and ROMs. See the ROM Compatibility section.

## Installation
1. Make certain you have Xposed installed before continuing.
2. Install the app by a) searching for it in the Xposed Installer or b) manually through the APK provided on [GitHub](https://github.com/kmark/Clarity/releases) or the [Xposed website](http://repo.xposed.info/module/com.versobit.kmark.clarity).
3. Configure the app in its settings panel. It can be accessed through the module section of the Xposed Installer or by the launcher shortcut.
4. Enable the module in the Xposed Installer.
5. Reboot.
6. Done!
7. The Xposed module will only update contact photos that are modified or added to the database after it is turned on. The database processor will attempt to update the contact photos in place and right away. To avoid using the database processor your contact photos must be manually refreshed. How/if this can be done and how easily is dependent on how you sync your contacts. For instance, if you use HaxSync you can update all your photos by going to Settings app -> Accounts -> HaxSync -> Advanced Settings -> Contact Settings -> Check Force redownload. Hit back twice. Select your account name at the top. Click Contacts to uncheck it. Recheck it and wait for sync to finish.


## ROM Compatibility
Clarity should work with CyanogenMod and other closely AOSP-based ROMs. Other ROMs like the stock ones found on most popular Android devices may not work with Clarity. If you have a device or ROM that Clarity works or does not work with (and is not already on the below list) please tell me!

**LG G2**

* CyanogenMod 12

**LG G3**

* SkyDragon (Lollipop)

**Motorola Droid X2**

* CyanogenMod 10

**Nexus 6**

* Temasek

**OnePlus One**

* BlissPop 2.2
* Temasek 5.0.2

**Samsung Galaxy Note II N7100**

* CyanogenModX 5.0.2

** Samsung Galaxy Note 4**

* CyanogenMod 12 (3/24 nightly)
* Stock (doesn't work? try this)

**Samsung Galaxy S4**

* CyanogenMod 11
* CyanogenMod 12

**Xperia J**
* Xperia Revolution ROM

## ROM Incompatibility
**Moto X (2013)**

* Stock (Android 4.4)

## Restoring Your Contacts Database
> Something went horribly wrong! How do I restore from one of the backups?

Ouch! Clarity stores backups on your "external storage." Depending on your device and ROM this could be an actual SD card or internal. If your device has two check both. It will be in a directory named *Clarity*. In the Clarity backup directory there will be sub-directories with dates. Select the one before everything went to hell. You'll find up to three files: contacts2.db contacts2.db-journal, and dbprocessor.log The .db file(s) are the actual contacts databases. The dbprocessor.log is your log file and has lots of great information for us to use. Now that we have located everything we need let's get restoring.

### Method #1 - By hand
If your device still boots and you can navigate around (even if things are crashing every few seconds) try this. If you have a custom recovery that supports adb then this will work as well if you boot into it.

1. Grab the `adb` command line tool. For Windows [check here](http://forum.xda-developers.com/showthread.php?t=2317790). For Linux and Mac [see here](https://code.google.com/p/adb-fastboot-install/).
2. Now put your device in debug mode. Go to the settings app. Tap *About phone*. Keep tapping *Build number* until it says you are a developer. Go back and select *Developer options*. Turn the development options "on" if needed. Under *Debugging* turn on *Android debugging*.
3. Now connect your device to your computer. It should say something about debugging being enabled in the status bar. Make certain your phone is unlocked. If it says something about allowing the computer debug access to your phone please accept it.
4. Open up a command line or terminal. On Linux I'm going to assume you know what to do. On Mac it's the Terminal app. On Windows it's `C:\Windows\system32\cmd.exe`
5. Type `adb version` and hit enter. If your get something about it being not found then adb isn't installed in your PATH or you're not in the directory in which adb was extracted. You can navigate to that directory with the `cd` command. So to navigate to the `system32` folder on Windows, for instance, I'd type `cd C:\Windows\system32` and hit enter.
6. Type `adb shell` and hit enter. This will dump us into a shell instance directly on your device.
7. Type `id` and hit enter. If the first bit is not `uid=0` then unlock your device and type `su` and hit enter. If your device prompts for superuser access please grant it.
8. Navigate to the directory in which your backups are stored on your device. For me this would be something like `cd /storage/emulated/legacy/Clarity/2015-Something`
9. You should now see the backup files if you type `ls` and hit enter. Great. Now to move these into place.
10. Open your `dbprocessor.log` file on your device through a file manager or move it onto your computer to read its contents. You can read it through the command line if you wish but it's much more convenient to be able to copy and paste if needed.
11. Note the Contacts UID and Contacts directory. Mine is `10006` and `/data/data/com.android.providers.contacts` respectively.
12. Execute `cp contacts2.db /your/contacts/directory/databases/contacts2.db` and hit enter. For me this full command is `cp contacts2.db /data/data/com.android.providers.contacts/databases/contacts2.db`
13. If you have a `contacts2.db-journal` file do the same except with that file. For me this is: `cp contacts2.db-journal /data/data/com.android.providers.contacts/databases/contacts2.db-journal`
14. Now to correct the permissions. Type `chown +UID:+UID /your/contacts/directory/databases/contacts2.db*` and hit enter. For me this is `chown +10006:+10006 /data/data/com.android.providers.contacts/databases/contacts2.db*`
15. Reboot immediately by typing `reboot` and hitting enter.
16. All done. Your contacts database has been restored.

### Method #2 - Recovery
Took a backup with your custom recovery before processing? Just restore from the backup to get your device back to normal.


## Licensing
Copyright &copy; 2015 Kevin Mark. Clarity is licensed under the GNU General Public License, Version 3, which can
be found in `LICENSE.md`

Clarity uses a number of open-source tools and libraries. Check the "License" link from within the app or open `app/src/main/assets/license.html`
