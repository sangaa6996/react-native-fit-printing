# react-native-fit-printing

## Getting started

`$ npm install react-native-fit-printing --save`

### Mostly automatic installation

`$ react-native link react-native-fit-printing`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fit-printing` and add `FitPrinting.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libFitPrinting.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.fitprinting.FitPrintingPackage;` to the imports at the top of the file
  - Add `new FitPrintingPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-fit-printing'
  	project(':react-native-fit-printing').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-fit-printing/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-fit-printing')
  	```


## Usage
```javascript
import FitPrinting from 'react-native-fit-printing';

// TODO: What to do with the module?
FitPrinting;
```
