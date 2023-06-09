<?php
/* 用于查询特定id的信息，返回名字和对应图像
    返回值：code，msg,name，img
    code 200 正常。400出错，即找不到
*/
function sql_find_name($id)
{
	$name = '';
	$img  = '';
	$code = 400;
	$msg = "识别失败！";
	$data = array($code,$msg,$name,$img);
	$table = "face";
	$link = @mysqli_connect("127.0.0.1","fory","passwrod",'datainfo');
if (!$link){
	$data[1]=mysqli_connect_error();
	}
$sql_find = sprintf("select * from %s where Id=%s",$table,$id);
if ($results = mysqli_query($link,$sql_find)){
	while ($result = mysqli_fetch_assoc($results)){
		$data = array(200,"识别成功！", $result['name'],$result['data']);
	}
}
else{
	$data[1]=mysqli_error($link);
}
mysqli_close($link);
return $data;
}


function SQL_Record_Access($id,$time,$open_mode,$content,$result)
{
    $table = "Acess_Record";
    $link = @mysqli_connect("127.0.0.1","fory","passwrod",'datainfo');
    if (!$link) {return;}
    if($open_mode == 1)
    {	// id time way content img result
    		$open_mode = "人脸识别";
		$sql_insert = sprintf('insert into %s values(null,"%s","%s","%s","%s")',$table,$id,$time,$open_mode,$result);
    	}else if($open_mode == 2){
    		// id time way content img result
    		$open_mode = "RFID认证";
    		$sql_insert = sprintf('insert into %s values(null,"%s","%s","%s","%s")',$table,$id,$time,$open_mode,$result);
    	}else if($open_mode == 3){
    		// id time way content img result
    		$open_mode = "密码认证";
    		$sql_insert = sprintf('insert into %s values(null,"%s","%s","%s","%s")',$table,$id,$time,$open_mode,$result);
    	}else{
    		// id time way content img result
    		$sql_insert = sprintf('insert into %s values(null,"%s","%s","%s","%s")',$table,$id,$time,$open_mode,$result);
    		}
   mysqli_query($link,$sql_insert);
   // 选出最新的，即上面添加的，取SN在data表来专门存数据，防止卡
	$sql_sn=sprintf('select SN from %s where SN = (select max(SN) from %s)',$table,$table);
	if ($results = mysqli_query($link,$sql_sn)){
	while ($result = mysqli_fetch_assoc($results)){
		$sql_insert_data = sprintf('insert into data values("%s","%s")',$result['SN'],$content);
		 mysqli_query($link,$sql_insert_data);
	}
}
   mysqli_close($link);
}


function sql_notice_get($warning)
{
    $link = @mysqli_connect("127.0.0.1","fory","passwrod",'datainfo');
    $sql_find_notice=sprintf('select * from notice;');
    $result = mysqli_query($link,$sql_find_notice);
    $json ="code:400,context:[],warning:Flase";
    //组合notice成列表，即数组
    $data =array();
    // 用以存每个公告
    class Context{
		public $title;
		public $data;
		public $date;
		}
	while ($row= mysqli_fetch_assoc($result)){
		$context = new Context();
		$context->title = $row["title"];
		$context->data = $row["data"];
		$context->date = $row["date"];
		$data[]=$context;
		}
	class Notice{
		public $code;
		public $context;
		public $warning;
	}
	$notic = new Notice();
	$notic->code = 200;
	$notic->context = $data;
	$notic->warning = $warning;

	$json = json_encode($notic,JSON_UNESCAPED_UNICODE);//把数据转换为JSON数据.
	echo $json;
    mysqli_close($link);
}
?>
