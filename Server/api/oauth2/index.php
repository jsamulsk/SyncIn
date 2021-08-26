<?php
  require_once dirname(__DIR__, 3).'/lib/vendor/autoload.php';
  require_once dirname(__DIR__, 1).'/config/database.php';
  require_once dirname(__DIR__, 1).'/objects/person.php';

  // begin session and record email from initial GET
  session_start();
  if (isset($_GET['email'])) {
    $_SESSION['email'] = $_GET['email'];
  }

  // if email was sent in initial GET, proceed
  // else, bad request
  if (isset($_SESSION['email'])) {
    // create google client with necessary paramenters
    $client = new Google_Client();
    $client->setAuthConfig(dirname(__DIR__, 3).'/lib/client_secrets.json'); // load my API client ID
    $client->addScope(Google_Service_Calendar::CALENDAR); // request access to calendar

    // if access_token set, save auth data to database
    // else, get one
    if (isset($_SESSION['access_arr']) && $_SESSION['access_arr']) {
      // initialize database and person objects
      $database = new Database();
      $db = $database->getConnection();
      $person = new Person($db);

      // set parameters
      $person->email = $_SESSION['email'];
      $person->auth_code = $_SESSION['auth_code'];
      $person->refresh_token = $_SESSION['access_arr']['refresh_token'];
      $person->access_token = $_SESSION['access_arr']['access_token'];
      $person->expires_in = $_SESSION['access_arr']['expires_in'];
      $person->scope = $_SESSION['access_arr']['scope'];
      $person->token_type = $_SESSION['access_arr']['token_type'];
      $person->created = $_SESSION['access_arr']['created'];

      // tell user whether create worked or not
      try {
        if ($person->create()) {
          // 200 created
          http_response_code(200);

          $redirect_uri = 'https://' . $_SERVER['HTTP_HOST'] . '/api/oauth2/success/';
          header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
        }
        else {
          // 503 unavailable (issue with database)
          http_response_code(503);
          echo json_encode(array("message" => "Unable to authenticate. Cannot connect to database"));
        }
      }
      catch (PDOException $exception) {
        // 403 forbidden (user already exists)
        http_response_code(403);
        
        $redirect_uri = 'https://' . $_SERVER['HTTP_HOST'] . '/api/oauth2/success/';
        header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
      }
    }
    else {
      $redirect_uri = 'https://' . $_SERVER['HTTP_HOST'] . '/api/oauth2/callback.php';
      header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
    }
  }
  else {
    // 400 bad request (does not include email in request)
    http_response_code(400);
    echo json_encode(array("message" => "Unable to authenticate. Incomplete request"));
  }
?>
