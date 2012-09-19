<?php

require_once('AWSSDKforPHP/sdk.class.php');
/**
 * +--------------------------------+
 * |    DynamoDB table structure    |
 * +-------------+------------------+
 * + title       |HashKey           |
 * + frame_no    |RangeKey          |
 * + frame       |text              |
 * + line_xxx    |text              |
 * +-------------+------------------+
 */
define('TABLE_NAME','player');

$p = new Player("player");

// TextをDynamoDBに登録 第1引数はテキストファイル群のあるディレクトリ、 第2引数は動画タイトル  第3引数にtrueでbatch_write
// テキストをDynamoDBに入れる時はこの下の行のコメントを外す
// $p->upload("/Users/ec2-user/dynamo/txt", "DynamoDB", true);


// 再生する時は 下の行のコメントを外す
//$p->play("DynamoDB", true);



class Player {

  private $table;
  private $dynamo;
  private $sleep;

  public function __construct($table) {
    $this->dynamo = new AmazonDynamoDB();
    $this->dynamo->set_region(AmazonDynamoDB::REGION_APAC_NE1);
    $this->sleep = 1;
    $this->table = $table;
  }

  public function play($title, $frame_rendering) {
    $frame = 0;
    do {
      $response = $this->dynamo->get_item(array(
          'TableName' => $this->table,
          'Key' => $this->dynamo->attributes(array(
              'HashKeyElement' => $title,
              'RangeKeyElement' => $frame++,
          )),
              ));
      if (!is_null($response->body)
              && !is_null($response->body->Item)
              && !is_null($response->body->Item->frame)
              && !is_null($response->body->Item->frame->{AmazonDynamoDB::TYPE_STRING})) {
        if ($frame_rendering) {
          echo($response->body->Item->frame->{AmazonDynamoDB::TYPE_STRING});
        } else {
          foreach ($resposne->body->Item->_toArray() as $key => $item) {
            if (preg_match("/^line/", $key)) {
              echo $item->{AmazonDynamoDB::TYPE_STRING} . "\n";
            }
          }
        }
      }
    } while ($response->isOK() && $frame < 1650);
  }

  public function upload($dir, $title = null, $batchput = false) {
    if (!is_dir($dir)) {
      throw new Exception("ディレクトリを指定しる");
    }
    if (is_null($title)) {
      $d = explode(DIRECTORY_SEPARATOR, dirname($dir));
      $title = array_pop($d);
    }
    $directory = dir($dir);
    $frame_no = 0;
    $items = array();

    for ($tl = 0; $tl < 1660; $tl++) {
      $sec = (int) ($tl / 10);
      $msec = $tl % 10;
      $file = "out_" . sprintf('%03d', $sec) . "_" . $msec . ".jpg.txt";
      if ($file == "." || $file == "..") {
        continue;
      }
      echo $file . "\n";
      $frame_no = preg_replace("/\D/", "", $file) * 1;
      $frame = file_get_contents($dir . DIRECTORY_SEPARATOR . $file);

      $attr = array(
          'title' => $title,
          'frame_no' => ++$frame_no
      );
      $lines = explode("\n", $frame);
      $rl = array();
      foreach ($lines as $index => $line) {
//        $attr['line_' . sprintf('%03d', $index)] = $line;
      }
      $attr['frame'] = $frame;

      if (!$batchput) {
        $res = $this->dynamo->put_item(array(
            'TableName' => $this->table,
            'Item' => $this->dynamo->attributes($attr
            )
                ));
        if ($res->status != 200) {
          var_dump($res);
        } else {
          echo $frame_no . " is success\n";
          echo "\n\n<br/><br/> " . __FILE__ . " __ " . __LINE__ . "<br/><br/>\n\n";
        }

        $item = array(
            'PutRequest' => array(
                "Item" => $this->dynamo->attributes(
                        $attr
                )
            )
        );
      } else {
        $items[] = $item;
        if (count($items) == 25) {
          $res = $this->dynamo->batch_write_item(array(
              'RequestItems' => array(
                  $this->table => $items
              )
                  ));
          $items = array();
        }
      }
    }
    if ($batchput) {
      $res = $this->dynamo->batch_write_item(array(
          'RequestItems' => array(
              $this->table => $items
          )
              ));
    }
  }

}

