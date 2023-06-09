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
 
  /* ��ʱ������ʼ�� */
  CPU_TS_TmrInit();
	
  /* ���Դ��ڳ�ʼ�� USART1 ����ģʽΪ 115200 8-N-1 �����ж� */
	USART_Config();
	
  /* HC05����ģ���ʼ����GPIO �� USART3 ����ģʽΪ 38400 8-N-1 �����ж� */
	if(HC05_Init() == 0)
	{
		HC05_INFO("HC05ģ����������");
	}
	else
	{
		HC05_ERROR("HC05ģ���ⲻ����������ģ���뿪��������ӣ�Ȼ��λ���������²��ԡ�");
		while(1);
	}
	
	HC05_Send_CMD("AT+RESET\r\n",1);	//��λָ������֮����Ҫһ��ʱ��HC05�Ż������һ��ָ��
	HC05_Send_CMD("AT+ORGL\r\n",1);
	
		/*��ʼ��SPP�淶*/
	HC05_Send_CMD("AT+INIT\r\n",1);
	HC05_Send_CMD("AT+CLASS=0\r\n",1);
	HC05_Send_CMD("AT+INQM=1,9,48\r\n",1);  
	HC05_Send_CMD("AT+INQ\r\n",1);
	
	 /*�δ�ʱ�ӳ�ʼ��*/
  SysTick_Init ();
  
	/* USART config */
	USART_Config(); 
	
  /*RC522ģ����������ĳ�ʼ������*/
  RC522_Init (); 
 
	PcdReset ();
	
  BEEP_Init();//��������ʼ��
	
	pwm_init();	 //PWM��ʼ��
	
	while(1)
	{
		IC_test (); 
	}
}