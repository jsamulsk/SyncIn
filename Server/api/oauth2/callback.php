<?php
  require_once dirname(__DIR__, 3).'/lib/vendor/autoload.php';

  // begin session
  session_start();

  // create google client with necessary paramenters
  $client = new Google_Client();
  $client->setApplicationName('Sync In');
  $client->setAuthConfig(dirname(__DIR__, 3).'/lib/client_secrets.json'); // load my API client ID
  $client->setRedirectUri('https://' . $_SERVER['HTTP_HOST'] . '/api/oauth2/callback.php'); // where to send response
  $client->addScope(Google_Service_Calendar::CALENDAR); // request access to calendar
  $client->setAccessType('offline'); // get token for offline refresh
  $client->setLoginHint($_SESSION['email']); // tell google which account to log into

  // if code not set, get one
  // else, request tokens
  if (!isset($_GET['code'])) {
    $auth_url = $client->createAuthUrl();
    header('Location: ' . filter_var($auth_url, FILTER_SANITIZE_URL));
  }
  else {
    $_SESSION['auth_code'] = $_GET['code'];

    $client->authenticate($_GET['code']);
    $_SESSION['access_arr'] = $client->getAccessToken();

    $redirect_uri = 'https://' . $_SERVER['HTTP_HOST'] . '/api/oauth2/index.php'; //CHECK LATER
    header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
  }
?>
