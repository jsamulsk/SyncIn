<?php
  header("Access-Control-Allow-Origin: *");
  header("Access-Control-Allow-Headers: access");
  header("Access-Control-Allow-Methods: GET");
  header("Access-Control-Allow-Credentials: true");
  header('Content-Type: application/json');

  require_once dirname(__DIR__, 1).'/config/database.php';
  require_once dirname(__DIR__, 1).'/objects/person.php';

  // initialize database and person objects
  $database = new Database();
  $db = $database->getConnection();
  $person = new Person($db);

  $person->email = isset($_GET['email']) ? $_GET['email'] : exit('Unable to check user. Incomplete request');

  if ($person->exists()) {
    // 200 OK
    http_response_code(200);
    echo json_encode(array('response' => 'true', 'message' => 'User exists'));
  }
  else {
    // 200 OK
    http_response_code(200);
    echo json_encode(array('response' => 'false', 'message' => 'User not in database'));
  }
?>
