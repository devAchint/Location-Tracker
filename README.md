# Location Tracker

This Location Tracker app is built using Kotlin and integrates with the Google Maps API to display maps and track the user's current location. It also utilizes the Geofencing API to set a defined radius. When the user exits this radius, the app triggers an alert through a notification and a popup. Upon re-entering the radius, the app sends another popup notification.
The app continues to work in the background using a service, even if the app is closed, ensuring uninterrupted geofencing alerts.

## Features

1. Integrates Google Maps API to display maps and the user's current location.
2. Implements geofencing to define a radius for location-based alerts.
3. Uses a foreground service to send continuous location updates.
4. Utilizes a broadcast receiver to trigger geofencing alerts.
5. Built with Kotlin and uses Data Binding for efficient UI updates.
6. Uses polylines to draw and display the user's movements on the map.



## Screenshot

|                                                                                                                         |                                                                                                               |                                                                                                                |
|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| ![initial](https://github.com/user-attachments/assets/7c452db1-d32a-4fb9-8432-8e720f025566)                             | ![start](https://github.com/user-attachments/assets/fb4469da-4fc2-480d-a7d8-e857c93ad817)                     | ![left](https://github.com/user-attachments/assets/17106471-1d3f-423a-a263-92c533c513bc)                       |
| ![notificaiton](https://github.com/user-attachments/assets/4ed7c12e-97cb-4f56-a2a4-867080a59695)                        | ![enter](https://github.com/user-attachments/assets/7ab125e1-a9e1-4554-ad72-ce6617de99fc)                     |                                                                                                                |




## Package Structure

- **[`service`](app/src/main/java/com/achint/locationtracker/service)**: Contains the `TrackingService`, responsible for tracking user location in the background.
- **[`broadcast`](app/src/main/java/com/achint/locationtracker/broadcast)**: Includes the `BroadcastReceiver` for handling Geofencing alerts.
- **[`ui`](app/src/main/java/com/achint/locationtracker/ui)**: Contains the `MainActivity`, responsible for managing the UI.
- **[`utils`](app/src/main/java/com/achint/locationtracker/utils)**: Contains utility classes.
  - `Constants`: Stores constant values used throughout the app.
  - `PermissionManager`: Handles permission requests and checks.


## Build With

[Kotlin](https://kotlinlang.org/):
As the programming language.

[Google Maps](https://developers.google.com/maps/documentation/android-sdk/overview) :
For implementing maps.

[GeoFencing](https://developer.android.com/develop/sensors-and-location/location/geofencing) :
For radius.

