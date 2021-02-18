# Image Cropper
Crop image library for Android intended to be used as independent component.

## Features
* Image cropping
* Customizable overlay appearance
* Large image support

## Requirements
Android API 21+

## Usage
Include dependency:
```groovy
//TODO
```

Add view to layout:
```xml
<com.rosberry.android.imagecropper.CropView android:id="@+id/cropView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

Set the image:
```kotlin
cropView.setImageAsset(assetFileName)
```
or
```kotlin
cropView.setImageResource(R.raw.resource_name)
```
or
```kotlin
cropView.setImageUri(imageUri)
```
**Important!** Crop frame must be created in order to calculate initial scale of the image,
so make sure to load the image after view was measured.

Get image region as bitmap:
```kotlin
val bitmap = cropView.crop()
```
**Important!** While resulting bitmap will be sampled to prevent OOM exception when necessary,
it still can appear too large for immediate use especially when working with large sources.

View appearance can be customized in layout xml or updated in runtime.
Available parameters listed in the table below:
| attribute      | attribute format    | property type  | default value  |
|----------------|---------------------|----------------|----------------|
| frameColor     | reference \| color  | Int            | #FFF           |
| frameMargin    | dimension           | Float          | 16dp           |
| frameShape     | enum { oval, rect } | FrameShape     | rect           |
| frameRatio     | string              | Float          | 1:1            |
| frameThickness | dimension           | Float          | 1dp            |
| gridColor      | reference \| color  | Int            | #FFF           |
| gridEnabled    | boolean             | Boolean        | false          |
| gridThickness  | dimension           | Float          | 1dp            |
| gridRows       | integer             | Int            | 3              |
| overlayColor   | reference \| color  | Int            | #CC000000      |
| scaleFactor    | float               | _Unsupported_  | 4              |

## Authors
Vitaly Fedorov, vitaly.fedorov@rosberry.com

## About

<img src="https://github.com/rosberry/Foundation/blob/master/Assets/full_logo.png?raw=true" height="100" />

This project is owned and maintained by [Rosberry](http://rosberry.com). We build mobile apps for users worldwide 🌏.

Check out our [open source projects](https://github.com/rosberry), read [our blog](https://medium.com/@Rosberry) or give us a high-five on 🐦 [@rosberryapps](http://twitter.com/RosberryApps).

## License

Product Name is available under the MIT license. See the LICENSE file for more info.
