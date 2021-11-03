# Garmin Health SDK Sample App

To use the Garmin Health Sample App place the unaltered license file provided by Garmin Health in the 'license'
directory of the app.  Running the 'loadLicense' task of the app project with this file in place will integrate
the license and build the app with your application id.  If this procedure is unsuccessful then you can manually
integrate your license into the app using the following procedure:

1. In the 'build.gradle' file, set the 'id' variable at the top of the file to your application id.
2. In the 'AndroidManifest.xml' file, set the 'package' element of the 'manifest' tag to your application id.
3. In the 'src/main/res/values/strings.xml' file, replace the placeholder text for the license string with your own license string.
4. Create a new package in the src/main/java directory for your full application id (i.e. com.sample.partner.app).
5. Move all src files to this new directory, allowing Android Studio to refactor references dynamically.
6. Using global find-replace, replace all references "import com.garmin.garminhealth.R;" with "import <YOUR APP ID>.R;"

In either case, once these steps or the build task have completed, you can build and run the sample app.