<?php
  class Person {
    // database connection and table name
    private $conn;
    private $table_name = 'auth_data';

    // properties
    public $email;
    public $auth_code;
    public $refresh_token;
    public $access_token;
    public $expires_in;
    public $scope;
    public $token_type;
    public $created;

    // constructor
    public function __construct($db) {
      $this->conn = $db;
    }

    // insert new user into database
    public function create() {
      // query to insert record
      $query = "INSERT INTO
                  " . $this->table_name . "
                SET
                  email=:email, auth_code=:auth_code, refresh_token=:refresh_token, access_token=:access_token,
                  expires_in=:expires_in, scope=:scope, token_type=:token_type, created=:created";

      // prepare query: sanitize input and bind values
      $stmt = $this->conn->prepare($query);

      $this->email=htmlspecialchars(strip_tags($this->email));

      $stmt->bindParam(':email', $this->email);
      $stmt->bindParam(':auth_code', $this->auth_code);
      $stmt->bindParam(':refresh_token', $this->refresh_token);
      $stmt->bindParam(':access_token', $this->access_token);
      $stmt->bindParam(':expires_in', $this->expires_in);
      $stmt->bindParam(':scope', $this->scope);
      $stmt->bindParam(':token_type', $this->token_type);
      $stmt->bindParam(':created', $this->created);

      // execute
      if ($stmt->execute()) {
        return true;
      }
      return false;
    }

    // check if person exists in database
    public function getPerson() {
      // query to read single record
      $query = "SELECT email, auth_code, refresh_token, access_token, expires_in, scope, token_type, created
                FROM
                  " . $this->table_name . "
                WHERE
                  email=:email";

      $stmt = $this->conn->prepare($query);
      $stmt->bindParam(':email', $this->email);
      $stmt->execute();

      return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    // "semi-static" function to check if array of people exist in database and, if so, return their rows
    public function getPeople($arr) {
      // query to read single record
      $query = "SELECT email, auth_code, refresh_token, access_token, expires_in, scope, token_type, created
                FROM
                  " . $this->table_name . "
                WHERE
                  email=:email";

      // gather all rows for users in $arr
      $people = array(true);
      $unregistered = array(false);

      foreach ($arr as $email) {
        $stmt = $this->conn->prepare($query); // CHECK IF THIS CAN BE MOVED
        $stmt->bindParam(':email', $email);
        $stmt->execute();

        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        // if empty row, then unregistered user in $arr
        if (empty($row)) {
          $unregistered[] = $email;
        }

        $people[] = $row;
      }

      // if some unregistered, return them
      if (count($unregistered) > 1) {
        return $unregistered;
      }
      return $people;
    }

    // stores new access token data
    public function updateAccessToken() {
      // query to update record
      $query = "UPDATE
                  " . $this->table_name . "
                SET
                  access_token=:access_token, expires_in=:expires_in, created=:created
                where
                  email=:email";

      // prepare query: bind values
      $stmt = $this->conn->prepare($query);

      $stmt->bindParam(':email', $this->email);
      $stmt->bindParam(':access_token', $this->access_token);
      $stmt->bindParam(':expires_in', $this->expires_in);
      $stmt->bindParam(':created', $this->created);

      // execute
      if ($stmt->execute()) {
        return true;
      }
      return false;
    }

    // checks if person exists in database
    public function exists() {
      // query to read single record
      $query = "SELECT email
                FROM
                  " . $this->table_name . "
                WHERE
                  email=:email";
      $stmt = $this->conn->prepare($query);
      $stmt->bindParam(':email', $this->email);
      $stmt->execute();

      $row = $stmt->fetch(PDO::FETCH_ASSOC);

      // if empty row, then unregistered user
      if (empty($row)) {
        return false;
      }
      return true;
    }
  }
?>
