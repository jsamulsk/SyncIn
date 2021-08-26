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
    !empty($data->searchStart) &&
    !empty($data->searchEnd) &&
    !empty($data->people)
  ) {
    $people = $person->getPeople($data->people); // returns array of rows if all valid emails, invalid emails otherwise

    // confirm all emails were valid
    if ($people[0]) {
      array_shift($people);

      $events = array();
      // make calendar requests for each person
      foreach($people as $row) {
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

        // prepare service and get events in time frame for each person
        $service = new Google_Service_Calendar($client);
        $calendarId = 'primary';
        $optParams = array(
          'maxResults' => 10,
          'orderBy' => 'startTime',
          'singleEvents' => true,
          'timeMin' => date('c', $data->searchStart),
          'timeMax' => date('c', $data->searchEnd)
        );
        $results = $service->events->listEvents($calendarId, $optParams);

        foreach($results->getItems() as $event) {
          $start = strtotime($event->start->dateTime);
          $end = strtotime($event->end->dateTime);
          $events[] = array('start' => $start, 'end' => $end);
        }
      }

      // 200 ok
      http_response_code(200);
      echo json_encode(array("response" => true, "body" => $events));
    }
    else {
      array_shift($people);
      // 200 ok (user unregistered)
      http_response_code(200);
      echo json_encode(array("response" => false, "body" => $people));
    }
  }
  else {
    // 400 bad request
    http_response_code(400);
    echo json_encode(array("message" => "Unable to find free time. Incomplete data"));
  }
?>
