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

void DATA_FRAME(void);
void Recevie_Data(void);

extern ReceiveData DEBUG_USART_ReceiveData;
extern ReceiveData BLT_USART_ReceiveData;
extern uint16_t lcdid;

int i;
char cStr [ 30 ];
char Data_frame[21]="2222A";//数据帧

char recevie[4];
char Data[5];


uint8_t KeyValue[]={0xFF ,0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
char ID[]="13265195";

char RFID[]="001";
char FACE[]="010";
char PWD[]="100";

char open[]="1111";
char close[]="0000";

void IC_test ( void )
{
  uint8_t ucArray_ID [ 4 ];    /*先后存放IC卡的类型和UID(IC卡序列号)*/                                                                                         
	uint8_t ucStatusReturn;      /*返回状态*/
  
  while ( 1 )
  {    
		  /* 处理蓝牙串口接收到的蓝牙数据 */
  if(BLT_USART_ReceiveData.receive_data_flag == 1)
  {
    BLT_USART_ReceiveData.uart_buff[BLT_USART_ReceiveData.datanum] = 0;
		Recevie_Data();
		if(strstr((char *)Data,"1111"))
    {
      TIM_SetCompare1(TIM3, 195);//0度
		  delay(50000000);
		  TIM_SetCompare1(TIM3, 185);//90度
		  sprintf ( cStr, "00001111" );
			DATA_FRAME();
			HC05_SendString(Data_frame);
			
    } 
    //清零蓝牙串口数据缓存
    BLT_USART_ReceiveData.receive_data_flag = 0;		//接收数据标志清零
    BLT_USART_ReceiveData.datanum = 0;  
  }
	
    /*寻卡*/
		if ( ( ucStatusReturn = PcdRequest ( PICC_REQALL, ucArray_ID ) ) != MI_OK )
    {
			
    }

    if ( ucStatusReturn == MI_OK  )
    {
      /*防冲撞（当有多张卡进入读写器操作范围时，防冲突机制会从其中选择一张进行操作）*/
      if ( PcdAnticoll ( ucArray_ID ) == MI_OK )                                                                   
      {
        PcdSelect(ucArray_ID);			
        PcdAuthState( PICC_AUTHENT1A, 0x11, KeyValue, ucArray_ID );//校验密码		
        sprintf ( cStr, "%02X%02X%02X%02X",ucArray_ID [0], ucArray_ID [1], ucArray_ID [2],ucArray_ID [3] );				
				
				DATA_FRAME();
				HC05_SendString(Data_frame);
        if(!strcmp(cStr,ID))
				{
					TIM_SetCompare1(TIM3, 195);//0度
		      delay(50000000);
		      TIM_SetCompare1(TIM3, 185);//90度
				}
				else
				{
				  GPIO_SetBits(GPIOC,GPIO_Pin_0);
	        delay(5000000);
	        GPIO_ResetBits(GPIOC,GPIO_Pin_0);//低电平不响
				  delay(40000000);
				}
			  PcdHalt();
      }
    }   
  }	
}

void DATA_FRAME(void)
{
	for(i=0;i<20;i++)
	{
		if(i>=5 && i<=7)
		{
			if(strstr((char *)recevie,"010"))
			{
				Data_frame[i]=FACE[i-5];
			}
			
			if(strstr((char *)recevie,"100"))
			{
				Data_frame[i]=PWD[i-5];
			}
			
			if(!strstr((char *)cStr,"00001111"))
			{
				Data_frame[i]=RFID[i-5];
			}
			
		}
		
		if(i>7&&i<16)
		{
			Data_frame[i]=cStr[i-8];
		}
		
		if(i>=16 && i<21)
		{
			if(!strcmp(cStr,"13265195") || !strcmp(cStr,"00001111"))
			{
				Data_frame[i]=open[i-16];
			}
			else
			{
				Data_frame[i]=close[i-16];
			}
		}
		if(i==21)
		{
			Data_frame[i]='\0';
		}
	}
}


void Recevie_Data(void)
{
	for(i=0;i<3;i++)
	{
		recevie[i]=BLT_USART_ReceiveData.uart_buff[i];
	}
	if(strstr((char *)recevie,"010") || strstr((char *)recevie,"100"))	
		{
			for(i=0;i<4;i++)
			{
				Data[i]=BLT_USART_ReceiveData.uart_buff[i+3];
		  }
	  }
		
		else
		{
		  for(i=0;i<4;i++)
			{
        Data[i]='0';
			}
		}
}