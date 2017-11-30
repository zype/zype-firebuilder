# Zype Amazon Fire TV Recipe

This document outlines step-by-step instructions for creating and publishing an Amazon Fire TV app powered by Zype's Endpoint API service and app production software and SDK template.

## Requirements and Prerequisites

#### Technical Contact
IT or developer support strongly recommended. Completing app submission and publishing requires working with app bundles and IDE.

#### Zype Amazon Fire TV Endpoint License
To create a Zype Amazon Fire TV app you need a paid and current Zype account that includes purchase of a valid license for the Zype Amazon Fire TV endpoint API. Learn more about [Zype's Endpoint API Service](http://www.zype.com/services/endpoint-api/).

#### An Amazon Developer Account
You may sign up for a Amazon Developer account via [Amazon's website](https://developer.amazon.com/appsandservices).

#### Android Studio installed
In order to compile, run, and package an app you need the latest version of Android Studio to be installed on your computer. Android Studio can be downloaded at: [https://developer.android.com/studio/install.html](https://developer.android.com/studio/install.html).

#### ADB installed
You'll need to have ADB (Android Debug Bridge) installed in order to use `adb` commands from the __Terminal/Command Line__. To install them on your computer, follow the [ADB documentation](https://developer.android.com/studio/command-line/adb.html).

## Creating a New App with the SDK Template

#### Generating the bundle

1. In order to generate an AFTV bundle using this SDK, you must first pull the latest source code from Zype's github repository. This can be found at "https://github.com/zype/zype-firebuilder".

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQcFR0UTIxcmhPTU0"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQcFR0UTIxcmhPTU0" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

Select the green __"Clone or download"__ button on the right.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQM01sdkloMnZpXzA"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQM01sdkloMnZpXzA" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

From here, there are two options to copy the files:

a. Click the __"Download ZIP"__ button on the bottom right. Then pick a folder to save the zipped files to.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQNzItbnpmek1qWkE"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQNzItbnpmek1qWkE" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQRTM4aWhvaGZsaWc"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQRTM4aWhvaGZsaWc" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

Once the ZIP file is downloaded, open the file to reveal the contents.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQSXRtUVlOUUxHUGs"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQSXRtUVlOUUxHUGs" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

__OR__

b.  Click the __"Git web URL"__ to highlight it and copy the URL.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQcE05YlJoeFpuYVE"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQcE05YlJoeFpuYVE" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

Open __Terminal/Command Line__ and __"cd"__ into the folder you want to save the files to.

##### Helpful command line tips for Terminal/Command Line

    ```
    ls  ---> shows folders in current directory
    cd Downloads  ---> goes into downloads if available (see ls)
    cd Downloads/myproject  ---> goes into downloads/myproject if available (see ls)
    cd ..  ---> goes back one directory level up
    ```
Clone the files into this folder by using the command __"git clone ***"__ and replace the asterisks with the copied url. Press enter.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQclNzLTlYOHdzcVE"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQclNzLTlYOHdzcVE" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

2. Open Android Studio. Open the Application folder in Android Studio. (File > Open > *application folder* > *Application*)

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQSWhxLVl0WGRUcXM"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQSWhxLVl0WGRUcXM" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

3. Build the APK. (Build > Build APK)

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQYUREcFZDZ29hMEE"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQYUREcFZDZ29hMEE" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

4. You can find the APK by clicking the __"Reveal in Finder"__ link at the bottom of the event log.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQZ3pVNUhpOW05WXc"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQZ3pVNUhpOW05WXc" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

__OR__

Find the directory: *Application folder* > Application > app > build > outputs > apk > *new APK*

#### Running the app

1. Open __Terminal/Command Line__ and enter the command "cd ***" and replace the asterisks with the folder containing the APK. An easy way of doing this is dragging in the folder instead of typing the directory.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQZ0hNazVWSnhnNkU"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQZ0hNazVWSnhnNkU" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

2. Make sure your Amazon Fire TV has the developer mode turned on. You can check this by navigating to: Settings > Device > Developer options > ADB Debugging and making sure the toggle is switched to "on."

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQbjFVZm1TWGRPWUE"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQbjFVZm1TWGRPWUE" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

3. Connect your computer to the AFTV device by typing in __Terminal/Command Line__:

__adb kill-server__
__adb start-server__
__asb connect ***__ (replace the asterisks with the device's IP which can be found by navigating to: Settings > Device > About > Network)

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQbWxLckJYRWxPS0E"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQbWxLckJYRWxPS0E" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

4. To install the app on the AFTV device, type the command __"adb install ***"__ and replace the asterisks with the packaged APK.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQeENOX2l0dzJvbzg"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQeENOX2l0dzJvbzg" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

5. To view your app in your AFTV device, navigate to: Settings > Applications > ManageInstalledApplications and your app should be listed there. You can view the app there and test it.

<a href="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQN0g3UnlVbjlzazA"><img src="https://drive.google.com/uc?export=view&id=0BzMPADAfOuPQN0g3UnlVbjlzazA" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>
