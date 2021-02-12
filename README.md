Don't know what Zype is? Check this [overview](http://www.zype.com/).

# Zype Fire App Builder Template

This SDK allows you to set up an eye-catching, easy to use Amazon Fire video streaming app integrated with the Zype platform with minimal coding and configuration. The app is built on top of Amazon Fire App builder with java framework using Zype API. With minimal setup you can have your Fire app up and running.
An example of fully functional app that is using a Zype sandbox account

## Fire App Builder

Fire App Builder is a java framework that allows developers to quickly build immersive, java based Android media applications for Fire TV, without writing code.  Fire App Builder accomplishes this by using a plug and play java framework with easy configuration files.  Developers simply specify the format of their media feed in a json file and add resources for logos and colors to create a rich media TV experience quickly.  Fire App Builder supports multiple modules for Analytics, Authentication and Advertising that you can enable for your app.

Full Documentation for Fire App Builder is located [here](https://developer.amazon.com/public/solutions/devices/fire-tv/docs/fire-app-builder-overview).


## Prerequisites

- Android Studio

## Installing

1. Clone the repo. Answer 'No' when Android Studio asked to create new project.

2. Open Application folder as existing project in Android Studio

3. This SDK has two app variants - Zype demo app and template app. To build Zype demo app:

   - Select `zypeDebug` build variant for `app`, `PurchaseInterface` and `ZypeAPI` modules

     <a href="https://drive.google.com/uc?export=view&id=1wz_eFKHcljpUFYuwfIKSCSCVhcKFGDPG"><img src="https://drive.google.com/uc?export=view&id=1wz_eFKHcljpUFYuwfIKSCSCVhcKFGDPG" style="width: auto; height: auto" title="Click for the larger version." /></a>

   - Set `zypeDebug` value to `defaultPublishConfig` parameters in `PurchaseInterface/build.gradle` and `Application/ZypeAPI/build.gradle` files.

***Note:*** To build custom app based on the template see [this section](#template).

4. Wait for Gradle to finish building. It may take up to 30 minutes.

5. Run the app on a connected Fire TV device or in TV emulator

## Supported Features

- Populates your app with content from enhanced playlists
- Video Search
- Live Streaming videos
- Resume watch functionality
- Pre-roll ads
- Midroll ads
- Video Favorites
- Closed Caption Support

## Unsupported Features

- Dynamic theme colors

## Monetizations Supported

- Native SVOD via In App Purchases
- Universal SVOD via login and device linking
- Native TVOD via In App Purchases
- Universal TVOD via login and device linking

## Supported Devices

Please only select the following devices when submitting your app to Amazon.

- Fire TV (2014)
- Fire TV (2015)
- Fire TV (Gen 3)
- Fire TV Stick
- Fire TV Stick with Alexa Voice Remote
- Amazon Fire TV Edition

Non-Amazon Android devices and Amazon Fire phones and tablets are not supported and should be deselected. 

## Creating Custom App Based on the Template<a name="template"></a>

1. Select `templateDebug` (or `templateRelease`) build variant for `app`, `PurchaseInterface`  and `ZypeAPI` modules

2. In `PurchaseInterface/build.gradle` and `Application/ZypeAPI/build.gradle` files set `defaultPublishConfig` parameter to `templateRelease` value.

3. Replace following placeholders with actual values:

  **Application/ZypeAPI/src/main/java/com/zype/fire/api/ZypeSettings.java**
```
    public static final String APP_KEY = "";
    public static final String CLIENT_ID = "";
    public static final String ROOT_PLAYLIST_ID = "";
```

Also replace features and monetization options placeholders with `true` or `false` values.

**Application/app/build.gradle**
- ```<APPLICATION_ID>``` - used to identify your app on the device and in the marketplace. Must be unique and usually is following `com.yourdomain.aftv` pattern.

**Application/app/src/template/res/values/strings.xml**
- ```<APP_NAME>```
- ```<APP_NAME_SHORT>```

**Application/app/src/template/res/values-en/strings.xml**
**Application/app/src/template/res/values-de/strings.xml**
**Application/app/src/template/res/values-ja/strings.xml**
- ```<APP_NAME>```

**Application/app/src/template/res/values/custom.xml**
- ```<BRAND_COLOR>``` - used for highlighting buttons and widgets

4. Update following resources:

**Company logo**

*Application/app/src/template/res/drawable/logo_company.png*

Image dimensions should be approximately 356 x 108 px and have a transparent background.

**App icon**

*Application/app/src/template/res/mipmap-mdpi/ic_launcher.png*
*Application/app/src/template/res/mipmap-hdpi/ic_launcher.png*
*Application/app/src/template/res/mipmap-xhdpi/ic_launcher.png*
*Application/app/src/template/res/mipmap-xxhdpi/ic_launcher.png*

You can use icon generator in Android Studio to produce icons with required dimensions from 512x512 source icon image.

5. Setup UI options

**App theme**

The app supports both dark and light themes.  
The default appearance is dark, where the screen background is black/dark grey, and text elements are white/light grey.
The Light theme will display all screens with white background and black/grey text elements.

To enable light theme:  
  - Set `LIGHT_THEME` feature flag to `true` in the *Application/ZypeAPI/src/main/java/com/zype/fire/api/ZypeSettings.java* file
  - Update the following values in the *Application/app/src/template/res/values/custom.xml* file:

| ***Attribute name***                 | ***Light theme***      | ***Dark theme***      |
|:-------------------------------------|:-----------------|:-----------------|
|`background`                          |#FFFFFF           |#22282E           |
|`spinner_color`                       |#FF000000         |#FFFFFFFF         |
|`primary_text`                        |#E6000000         |#E6FFFFFF         |
|`secondary_text`                      |#99000000         |#E6FFFFFF         |
|`tertiary_text`                       |#99000000         |#99FFFFFF         |
|`search_edit_text_bg_color`           |#4D000000         |#4DFFFFFF         |
|`search_edit_text_bg_color_unfocused` |#1A000000         |#1AFFFFFF         |



**Left menu**

Left menu will appear when the user press `Menu` button on the device retomte control. It contains the same actions like Settings row at the bottom of the main screen.
This option is turned off by default. To turn it on make the method `displayLeftMenu()` in the `ZypeConfiguration.java` return `true` value.

**Watched bar**

The watched bar is displayed on the video thumbnail and shows the time that user watched this video.
This option is turned on by default. To turn it off make the method `displayWatchedBarOnVideoThumbnails()` in the `ZypeConfiguration.java` return `false` value.

6. Analytics

**Segment analytics**

To enable Segment Analytics:
- In the ***settings.gradle*** file uncomment lines referencing to `SegmentAnalyticsComponent`.
- In the ***app/build.gradle*** file uncomment the following row:   
```    
    compile project(':SegmentAnalyticsComponent')
```
- Rebuild the project

***Note:** You can find detail instructions on adding and removing Fire App Builder components [here](https://developer.amazon.com/docs/fire-app-builder/load-a-component.html#loadcomponent)* 

Setting up Segment Analytics:
- In the `SegmentAnalyticsComponent` module open the ***res/values/values.xml*** file and set your Segment write key in this line:
```
    <string name="segment_analytics_write_key">Your WRITE_KEY here</string>
```

7. Rebuild the project

## Built With

* [Zype API](http://dev.zype.com/api_docs/intro/) - Zype API docs

## Contributing

Please submit pull requests to us.

## Versioning

For the versions available, see the [tags on this repository](https://github.com/zype/zype-firebuilder/tags). 

## Authors

* **Evgeny Cherkasov** - *Initial work* - [ech89899](https://github.com/ech89899)

See also the list of [contributors](https://github.com/zype/zype-firebuilder/graphs/contributors) who participated in this project.

## License

This project is licensed under the License - see the [LICENSE](LICENSE.md) file for details
