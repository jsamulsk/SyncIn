# SyncIn
Android mobile app for scheduling. Implemented with native Java and LAMP backend.
(Does not inlcude entire app, only source and build files. Only PHP API included for server, can run on any LAMP server.)

The app is designed to find free time among multiple Google calendars. It uses a back-end server to support the app and make the necessary Google API calls.

The Android folder contains the source code, both classes and activities. There are three activities and one helper class:

<ul>
  <li>
    Login Activity:
    <ul>
      <li>Handles logging the user into the app by communicating with the server</li>
      <li>If account does not already exist, opens custom tab to work through Google login and authentication</li>
    </ul>
  </li>
  <li>
    Main Activity:
    <ul>
      <li>Allows user to choose which Google emails/calendars to check for free time against</li>
      <li>Provides UI for choosing search parameters</li>
    </ul>
  </li>
  <li>
    Schedule Activity:
    <ul>
      <li>Displays free time found across all selected calendars within requested search parameters</li>
      <li>Allows user to choose one and schedule the event on all selected calendars</li>
    </ul>
  </li>
  <li>
    HTTP Request Class:
    <ul>
      <li>Helper class for making HTTP requests to the server</li>
    </ul>
  </li>
</ul>

The Server folder contains the source code for the PHP API. There are two helper classes and four pages:

<ul>
  <li>
    Database Class:
    <ul>
      <li>Object for setting up connection to send requests to the database</li>
    </ul>
  </li>
  <li>
    Person Class:
    <ul>
      <li>Object for managing a person's information</li>
      <li>Contains functions for making specific requests to the database (i.e., adding a person, updating their access token, etc.)</li>
    </ul>
  </li>
  <li>
    Exists Page:
    <ul>
      <li>Checks whether or not a given list of people are registered in the database</li>
    </ul>
  </li>
  <li>
    OAuth2 Page:
    <ul>
      <li>Handles authentication with Google</li>
      <li>Works user through the authentication process, with callback and success webpage</li>
    </ul>
  </li>
  <li>
    Find Page:
    <ul>
      <li>Makes request to Google to get calendar data within a time frame for a group of users</li>
      <li>Sends list of events back to app for processing</li>
    </ul>
  </li>
  <li>
    Schedule Page:
    <ul>
      <li>Makes request to Google to schedulde a calendar event for a group of users</li>
    </ul>
  </li>
</ul>
