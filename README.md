# Mobile app/game for Android

## Parts
There are two parts - physical module and the Android app in this repository.

### Physical module is very easy, contains:
- Bluetooth Module
- GPS Module
- other things to make the above meaningfully work

In the following pictures, the module is mounted on a frisbee:

![Covered parts](https://github.com/l-korous/maiaga-android/blob/master/device-covered.jpg)

![Uncovered parts](https://github.com/l-korous/maiaga-android/blob/master/device-uncovered.jpg)

### How the application works

There is no button for "I am throwing", the app detects (based on motion of the module) that there is no throw attempt:

![No throw in progress](https://github.com/l-korous/maiaga-android/blob/master/no_throw.png)

Then you throw the module, and the app starts collecting data

![No throw in progress](https://github.com/l-korous/maiaga-android/blob/master/in_throw.gif)

When the module stops moving, the app processes the data and shows result

![No throw in progress](https://github.com/l-korous/maiaga-android/blob/master/after_throw.gif)

... and shows data such as distance, max altitude and speed.