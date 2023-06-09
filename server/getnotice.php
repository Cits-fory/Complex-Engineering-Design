<?php
require 'sql_control.php';
$f_open = fopen("warming.txt","r");
$warming = trim(fgets($f_open));
fclose($f_open);
sql_notice_get($warming);
?>


