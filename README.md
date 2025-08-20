# FitnessTracker

FitnessTracker is a multi-module fitness tracking app for phones and Wear OS devices you can learn to build in the Android Essentials course bundle ([Get it here](https://pl-coding.com/android-essentials-bundle?utm_source=github&utm_medium=readme&utm_campaign=readme_link&utm_id=essentials)).

![Run Feature](https://pl-coding.com/wp-content/uploads/2024/04/run-feature.png)
<table>
  <tr>
    <td>
      <img src="https://pl-coding.com/wp-content/uploads/2024/04/auth-feature.png" alt="Auth Feature" width="500"/>
    </td>
    <td>
      <img src="https://pl-coding.com/wp-content/uploads/2024/04/phone-watch-mockup.png" alt="Phone Watch Mockup" width="300"/>
    </td>
  </tr>
</table>

## What's covered?

In this course bundle, you will learn these concepts/technologies:
- Project planning
- Software architecture theory
- Multi-module architecture
- Gradle for large-scale projects (version catalogs & convention plugins)
- Authentication systems (OAuth / token refresh)
- Offline-first development
- Dynamic feature modules
- Google Maps SDK
- Jetpack Compose in multi-module projects
- Wear OS development (Health services API, data sync, UI building)

## How do you run the project?

In order to run the project on your phone, you'll need to first clone it and then add two API keys for:
1. ... the FitnessTracker API (access granted after course purchase)
2. ... Google Maps (needs to be got from Google Cloud Console - instructions in the course)

### Setting up API Keys

1. **Copy the template file:**
   ```bash
   cp local.properties.template local.properties
   ```

2. **Edit `local.properties` and add your API keys:**
   ```properties
   # Android SDK location (update this path to match your system)
   sdk.dir=/path/to/your/Android/sdk
   
   # API Key for the FitnessTracker application
   API_KEY=YOUR_ACTUAL_API_KEY_HERE
   
   # Google Maps API Key
   MAPS_API_KEY=YOUR_ACTUAL_MAPS_API_KEY_HERE
   ```

3. **Replace the placeholder values:**
   - `YOUR_ACTUAL_API_KEY_HERE` → Your FitnessTracker API key (from course purchase)
   - `YOUR_ACTUAL_MAPS_API_KEY_HERE` → Your Google Maps API key (from Google Cloud Console)
   - Update the `sdk.dir` path to match your Android SDK location

Afterwards, build the project and you're ready to use it.

**Note:** The `local.properties` file is automatically ignored by git for security reasons, so your API keys won't be committed to version control.
