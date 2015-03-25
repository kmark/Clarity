# Clarity
![Clarity](https://raw.githubusercontent.com/kmark/Clarity/master/app/src/main/res/mipmap-xxhdpi/ic_launcher.png)

***Android contact thumbnails you don't mind looking at.***

> **XDA-Developers:**  [forum.xda-developers.com](http://forum.xda-developers.com/)  
> **Xposed Repository:** [repo.xposed.info/module/com.versobit.kmark.clarity](http://repo.xposed.info/module/com.versobit.kmark.clarity)

Clarity is an Xposed module and root Android application for increasing the size and quality of contact thumbnails in the Android contacts database. It's comprised of two components. An Xposed modification and a root-required Database Processor.

### Xposed Mod

Android stores two versions of contact images. The *contact photo* and the *contact thumbnail*. While both are downscaled appropriately from the original image, the two are very different in size. Contact photos clock in at around 720×720 pixels or so depending on the original image. Contact thumbnails are a measly 96×96 pixels. Contact photos are used in places where contact imagery is going to be distinctively large like the KitKat/Lollipop Phone app. Or when you receive an incoming call. Thumbnails are used for notifications, messaging applications, and other roles which the full contact photo is unnecessarily big. Unfortunately, as screen PPIs have skyrocketed since the 96×96 limit was put in place, contact thumbnails are now too small for their original purpose. Regardless, most applications continue to use contact thumbnails.

Clarity forces Android to use a user-defined size (defaulting to 256×256) when adding contact thumbnails to the database. While these new images are larger and look much better they will load slightly slower and increase memory requirements. For newer devices this should not be a problem. For reference, I have not witnessed any side effects on my Galaxy S4 using the default 256×256 setting. Since the size can be adjusted you can optimize it for your device.

In the name of simplicity, Clarity does not change the method Android uses to downscale contact thumbnails, just the final dimensions. However since Clarity allows you to increase the size of the thumbnail and consequently decrease the magnitude of the downscale, there will be noticeably fewer artifacts in your new thumbnails.

The Xposed module does not magically increase the quality of thumbnails already added in the database. It only affects newly added/updated contacts and images. To upgrade your entire contacts database either use the Database Processor detailed below or re-import the contacts. Re-importing may require you to deconnect/desync the account the contacts are associated with and then reconnect/resync the account. Some sync applications may have a force refresh option and that should work nicely. For instance, HaxSync has a *Force redownload* option under its contact settings.

### Database Processor

In addition to the Xposed-based modification described above, Clarity includes a *Database Processor*. This powerful feature forcibly updates every contact thumbnail in your database. It rips out the internal contacts database file, finds all your contacts with photos and puts in new thumbnails. It'll then replace the old database with the new one. It is highly recommended to immediately reboot after processing to avoid Android going berserk. It will if you don't. :)

**While powerful, the Database Processor is also extremely dangerous.** I am *not* responsible for any damage to your device as a result of using Clarity. It performs several operations as root and modifies core Android files that were never meant to be touched by anything other than Android itself. I highly recommend a full recovery-based backup of your device alongside the built-in backup feature. *Please* use the dry-run feature first (hell, multiple times even) to ensure the operation will succeed for your ROM. It is possible a successful dry-run could still fail when doing the real-deal so, as I mentioned before, backup your stuff. The built-in contact backups will be saved to a *Clarity* folder on your "primary" external storage. What primary means depends on your ROM and device. It could be your actual external SD card or it could be an internal storage location. Regardless, the current backup location will be noted, assuming backups are enabled, in the processor's log. As an added bonus, if the backup feature is on it will also save the log file alongside your backups!

The Database Processor has been tested on CyanogenMod 10 (based on Android 4.1.2), CyanogenMod 11 (based on Android 4.4.4), and CyanogenMod 12 (based on Android 5.0.2). It *should* work on any AOSP-based ROM from Ice Cream Sandwich (4.0) to Lollipop MR1 (5.1). For stock ROMs and anything created by carriers I have no idea. If it works for you please report back! If it doesn't, well, report that too.

Clarity is developed in my spare time and will always remain free and open-source software. If you find this application useful please feel free to [donate](http://forum.xda-developers.com/donatetome.php?u=4195957).

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

## Licensing
Copyright &copy; 2015 Kevin Mark. Clarity is licensed under the GNU General Public License, Version 3, which can
be found in `LICENSE.md`

Clarity uses a number of open-source tools and libraries. Check the "License" link from within the app or open `app/src/main/assets/license.html`
