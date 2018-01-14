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

3. This SDK has two app versions - Zype demo app and template app
   - To build Zype demo app select `zypeDebug` build variant for `app` and `ZypeAPI` modules

     <a href="https://drive.google.com/uc?export=view&id=1wz_eFKHcljpUFYuwfIKSCSCVhcKFGDPG"><img src="https://drive.google.com/uc?export=view&id=1wz_eFKHcljpUFYuwfIKSCSCVhcKFGDPG" style="width: auto; height: auto" title="Click for the larger version." /></a>

   - To build custom app based on the template version follow instructions in the [RECIPE.md](./RECIPE.md)
4. Wait for Gradle to finish building. It may take up to 30 minutes.

5. Run the app on a connected Fire TV device or in TV emulator

## Supported Features

- Populates your app with content from enhanced playlists
- Video Search
- Live Streaming videos
- Resume watch functionality
- Pre-roll ads
- Midroll ads

## Unsupported Features

- Video Favorites 
- Dynamic theme colors
- Closed Caption Support

## Monetizations Supported

- Native SVOD via In App Purchases
- Universal SVOD via login

## Supported Devices

Please only select the following devices when submitting your app to Amazon.

- Fire TV (2014)
- Fire TV (2015)
- Fire TV (Gen 3)
- Fire TV Stick
- Fire TV Stick with Alexa Voice Remote
- Amazon Fire TV Edition

Non-Amazon Android devices and Amazon Fire phones and tablets are not supported and should be deselected. 

## Creating new Fire TV app based of Zype Template

### Customizing UI resources

**App name**

In the ```app\res\values\strings.xml``` file update following string resources:

- ```app_name```
- ```app_name_short```

**Company logo**

Image dimensions should be approximately 356 x 108 px and have a transparent background.
Put your logo file to ```app\res\drawable``` folder.

In the ```app\res\values\custom.xml``` file update following elements with your logo drawable resource id:

- ```splash_logo```
- ```company_logo```

**App icon**

In ```app\res\mipmap``` folders (```-mdpi```, ```-hdpi```, ```-xhdpi```, ```-xxhdpi```) update ```ic_launcher``` file.

You can use icon generatir in Android Studio to produce icons with required dimensions from 512x512 source icon image.   

**Action buttons and other widgets**

To use your brand color for highlighting buttons and widgets update ```accent``` color resource in the ```app\res\values\custom.xml``` file.  

### Building app with the template variant

Note: 
- Build `app` and `ZypeApi` as `templateDebug` or `templateRelease` in the Build Variants section
- Update `ZypeApi/build.gradle` to use `templateDebug` as defaultConfig instead of `demoDebug`

Update the following resources in each file:

In ```Application/ZypeAPI/src/template/java/com/zype/fire/api/ZypeSettings.java```
- ```<APP_KEY>```
- ```<CLIENT_ID>```
- ```<CLIENT_SECRET>```
- ```<ROOT_PLAYLIST_ID>```

Also, set subscription details to `true/false` as you see fit.

In ```Application/app/build.gradle```
- ```<APPLICATION_ID>```

In ```Application/app/src/template/res/values/custom.xml```
- ```<BRAND_COLOR>```

In ```Application/app/src/template/res/values-en/strings.xml```
- ```<APP_NAME>```

In ```Application/app/src/template/res/values/strings.xml```
- ```<APP_NAME>```
- ```<APP_NAME_SHORT>```

## Built With

* [Zype API](http://dev.zype.com/api_docs/intro/) - Zype API docs

## App Architecture

Modules customized to work with Zype Platform:

- **DataLoader**

New custom data loader ```ZypeDataDownloader``` is added. It is loading playlist and video data using Zype API and put this data into feed, which is processed further by Fire App Builder. See "App Configuration" section for how ZypeDatadownloader is connected to Fire App Builder.

- **ContentBrowser**

This core module is modified to support Zype enchanced playlists and custom data feed.

- **ContentModel**

New custom feed translators ```ZypeContentTranslator``` and ```ZypeContentContainerTranslator``` are added to map specific Zype 
video and playlist objects to Fire App Builder content model.

- **TVUIComponent**

```CardPresenter``` is modified to support thumbnail images for categories (playlists).

```ContentBrowseFragment``` is modified to support navigation from home screen to nested playlists.

```ZypePlaylistContentBrowseActivity``` 
```ZypePlaylistContentBrowseFragment``` are added to display content of nested playlistsis.

- **App Configuration**

```DataLoadManagerConfig.json``` is modified to support custom Zype data downloader

```ZypeDownloaderConfig.json``` is added for using with ZypeDataDownloader

```ZypeCategoriesRecipe.json```
```ZypeContentsRecipe.json``` is added to configure parsing data from custom Zype feed

```Navigator.json``` is modified to support Zype customizations

## Contributing

Please submit pull requests to us.

## Versioning

For the versions available, see the [tags on this repository](https://github.com/zype/zype-firebuilder/tags). 

## Authors

* **Evgeny Cherkasov** - *Initial work* - [ech89899](https://github.com/ech89899)

See also the list of [contributors](https://github.com/zype/zype-firebuilder/graphs/contributors) who participated in this project.

## License

This project is licensed under the License - see the [LICENSE](LICENSE.md) file for details
