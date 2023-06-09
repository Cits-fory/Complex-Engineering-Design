<?php
namespace vendor;
require 'sql_control.php';

class EncryptionTool{
    public static function enAES($originTxt, $key): string{
        return base64_encode(openssl_encrypt($originTxt, 'AES-128-ECB',$key, OPENSSL_RAW_DATA));
    }
    public static function deAES($originTxt, $key): string{
        $data = base64_decode($originTxt);
        return openssl_decrypt($data,'AES-128-ECB',$key, OPENSSL_RAW_DATA);
    }
}
$score_limit = 70;
$key_num ="2020748713735726";
$raw_post_data = file_get_contents('php://input');
# 接收到raw-解密到arr-转json到data
$arr = EncryptionTool::deAES($raw_post_data, $key_num);
$data = json_decode($arr,true);
$result = "False";
$face_data = "NULL";
// 先设定失败为前提
$re = '{"code":400,"msg":"请检查原数据格式！","name":"","num":"","face_data":"","result":0}';
$content = $raw_post_data;$time=date('Y-m-d H:i:s');$id=$_SERVER["REMOTE_ADDR"];$open_mode="未知";
try{
	if($data['code']==200){
	$open_mode = $data['open_mode'];
	$time = date('Y-m-d H:i:s', $data['time']/1000);
	switch($open_mode){
	    case 1:
	        $content=$data['face_data'];$re=face_recognize($content); break;
	    case 2:
	        $content=$data['rfid_id'];$re=rfid_recognize($content);break;
	    case 3:
	        $content=$data['password'];$re=password_recognize($content);break;
		}
	}
}
finally{
	$re = EncryptionTool::enAES($re, $key_num);
	echo $re;
	// echo $id.$time.$open_mode.$content.$result;
	SQL_Record_Access($id,$time,$open_mode,$content,$result);
	
	
}
// 百度API获取access_token
// https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=&client_secret=
function face_recognize($image) {
	global $open_mode;
	global $result;
	global $id;
	global $score_limit;
    $POSTFIELDS = sprintf('{"group_id_list":"Access_control","image":"%s","image_type":"BASE64"}',$image );
        $curl = curl_init();
        curl_setopt_array($curl, array(
            CURLOPT_URL => "https://aip.baidubce.com/rest/2.0/face/v3/search?access_token=",
            CURLOPT_TIMEOUT => 30,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST => 'POST',
            CURLOPT_POSTFIELDS =>$POSTFIELDS,
            CURLOPT_HTTPHEADER => array('Content-Type: application/json'),
        ));
        $response = curl_exec($curl); // 源文
        curl_close($curl);
        $data = json_decode($response,true);
        if($data['error_code'] == 0) // 识别成功
        {
		$result1 = $data['result'];
		$user_list = $result1['user_list'];
		$score = $user_list[0]['score'];
		$user_id = $user_list[0]['user_id'];
          if($score>$score_limit){     // 阈值判断
          	$temp=sql_find_name($user_id);
            if($temp[0] == 200){   // 数据库查询名字学号照片
            	$re = sprintf('{"code":200,"msg":"人脸验证通过！","name":"%s","num":"%s","face_data":"%s","result":1}',$temp[2],$user_id,$temp[3]);
            	$result = "True";
            	$id = $temp[2];
            }
          }else{
            	$result = "Flase";
            	$re = sprintf('{"code":401,"msg":"数据格式通过但照片未在数据库中！","name":"","num":"","face_data":"","result":0}');
            }
        }else{
        	$re = sprintf('{"code":%s,"msg":"%s","name":"","num":"","face_data":"","result":0}',$data['error_code'],$data['error_msg']);
        	}
       return $re;
}

function rfid_recognize($rid) {
	global $result;
	global $id;
	// 001D0BB89100000 -> D0BB8910 0000
	$id = substr($rid, 3, 8);  // 去除开头的001
	$j = substr($rid, 11, 15); // 取末尾的0000/1111
	$result = "Flase"; 
	if($j=="1111"){
		$re = sprintf('{"code":200,"msg":"RFID验证成功！","name":"","num":"","rfid_id":"%s","result":1}',$id);
		$result = "True";
	}else{
		$re = sprintf('{"code":200,"msg":"RFID验证失败！","name":"","num":"","rfid_id":"%s","result":0}',$id);
	}
	return $re;
}

function password_recognize($pwd) {
	global $result;
	global $id;
	$id = $pwd;
	$link = @mysqli_connect("127.0.0.1","fory","password",'datainfo');
	$sql_find_pwd=sprintf('select * from password where pwd=%s limit 1;',$pwd);
	if ($results = mysqli_query($link,$sql_find_pwd)){
		if (mysqli_num_rows($results)){
			$re = sprintf('{"code":200,"msg":"密码验证成功！","name":"","num":"","password":"%s","result":1}',$pwd);
			$result = "True"; 
		}
		else {
			$re = sprintf('{"code":401,"msg":"密码验证失败！","name":"","num":"","password":"%s","result":0}',$pwd);
		}
	}
	mysqli_close($link);
	return $re;
}


?>

