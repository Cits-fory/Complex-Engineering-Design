#include "stm32f10x.h"   
#include "pwm.h"
#include "./usart/bsp_usart.h"	
#include "./lcd/bsp_ili9341_lcd.h"
#include "./flash/bsp_spi_flash.h"
#include "./SysTick/bsp_SysTick.h"
#include "rc522_config.h"
#include "rc522_function.h"
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include "delay.h"
#include "./usart/bsp_usart_blt.h"
#include "./hc05/bsp_hc05.h"
#include "beep.h"

extern ReceiveData DEBUG_USART_ReceiveData;
extern ReceiveData BLT_USART_ReceiveData;

int main()
{	  
 
  /* 延时函数初始化 */
  CPU_TS_TmrInit();
	
  /* 调试串口初始化 USART1 配置模式为 115200 8-N-1 接收中断 */
	USART_Config();
	
  /* HC05蓝牙模块初始化：GPIO 和 USART3 配置模式为 38400 8-N-1 接收中断 */
	if(HC05_Init() == 0)
	{
		HC05_INFO("HC05模块检测正常。");
	}
	else
	{
		HC05_ERROR("HC05模块检测不正常，请检查模块与开发板的连接，然后复位开发板重新测试。");
		while(1);
	}
	
	HC05_Send_CMD("AT+RESET\r\n",1);	//复位指令发送完成之后，需要一定时间HC05才会接受下一条指令
	HC05_Send_CMD("AT+ORGL\r\n",1);
	
		/*初始化SPP规范*/
	HC05_Send_CMD("AT+INIT\r\n",1);
	HC05_Send_CMD("AT+CLASS=0\r\n",1);
	HC05_Send_CMD("AT+INQM=1,9,48\r\n",1);  
	HC05_Send_CMD("AT+INQ\r\n",1);
	
	 /*滴答时钟初始化*/
  SysTick_Init ();
  
	/* USART config */
	USART_Config(); 
	
  /*RC522模块所需外设的初始化配置*/
  RC522_Init (); 
 
	PcdReset ();
	
  BEEP_Init();//蜂鸣器初始化
	
	pwm_init();	 //PWM初始化
	
	while(1)
	{
		IC_test (); 
	}
}