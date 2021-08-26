<?php
  header('Access-Control-Allow-Origin: *');
  header('Content-Type: application/json; charset=UTF-8');
  header('Access-Control-Allow-Methods: POST');
  header('Access-Control-Max-Age: 3600');
  header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

  require_once dirname(__DIR__, 3).'/lib/vendor/autoload.php';
  require_once dirname(__DIR__, 1).'/config/database.php';
  require_once dirname(__DIR__, 1).'/objects/person.php';

  // initialize database and person objects
  $database = new Database();
  $db = $database->getConnection();
  $person = new Person($db);

  // get posted data
  $data = json_decode(file_get_contents('php://input'));

  // confirm request validity
  if (
    !empty($data->owner) &&
    !empty($data->start) &&
    !empty($data->end)
  ) {
    $person->email = $data->owner;
    $row = $person->getPerson();

    // create proper client
    $client = new Google_Client();
    $client->setScopes(Google_Service_Calendar::CALENDAR);
    $client->setAuthConfig(dirname(__DIR__, 3).'/lib/client_secrets.json');
    $client->setAccessType('offline');

    // fetch access token details from database
    $token = json_encode(array('refresh_token' => $row['refresh_token'],
                                'access_token' => $row['access_token'],
                                'expires_in' => $row['expires_in'],
                                'scope' => $row['scope'],
                                'token_type' => $row['token_type'],
                                'created' => $row['created']
                              ));
    $client->setAccessToken($token);

    // check if access token needs to be refreshed and do so
    if ($client->isAccessTokenExpired()) {
      $client->fetchAccessTokenWithRefreshToken($client->getRefreshToken());

      $person->email = $row['email'];
      $person->access_token = $client->getAccessToken()['access_token'];
      $person->expires_in = $client->getAccessToken()['expires_in'];
      $person->created = $client->getAccessToken()['created'];
      $person->updateAccessToken();
    }

    // prepare service
    $service = new Google_Service_Calendar($client);

    // create event for request
    $start = new DateTime(date('c', $data->start));
    $end = new DateTime(date('c', $data->end));

    // convert srat and end times to google format
    $googleStart = new Google_Service_Calendar_eventDateTime();
    $googleStart->setDateTime($start->format(\DateTime::RFC3339));
    $googleEnd = new Google_Service_Calendar_eventDateTime();
    $googleEnd->setDateTime($end->format(\DateTime::RFC3339));

    // specify event params
    $eventParams = array(
      'summary' => 'Sync In Meeting',
      'start' => $googleStart,
      'end' => $googleEnd
    );

    // check if should send invites
    if (!empty($data->sendInvites)) {
      $eventParams['sendUpdates'] = 'all';
    }

    // check if attendees are given
    if (!empty($data->others)) {
      $attendees = array();
      foreach ($data->others as $other) {
        $attendees[] = array('email' => $other);
      }
      if (!empty($attendees)) {
        $eventParams['attendees'] = $attendees;
      }
    }

    // prepare event
    $event = new Google_Service_Calendar_Event($eventParams);

    // make request
    $calendarId = 'primary';
    $event = $service->events->insert($calendarId, $event);
    printf('Event created: %s\n', $event->htmlLink);
  }
  else {
    // 400 bad request
    http_response_code(400);
    echo json_encode(array("message" => "Unable to schedule meeting. Incomplete data"));
  }
?>
