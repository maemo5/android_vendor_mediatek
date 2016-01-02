#pragma once


class FlashSimpleDrv2
{
public:
	static FlashSimpleDrv2* getInstance();
	int init(unsigned long sensorDev);
	int setOnOff(int a_isOn);
	int uninit();
	int setPreOn();
	int getPreOnTimeMs(int* ms);;
private:
	int m_fdSTROBE;
	FlashSimpleDrv2();
	virtual ~FlashSimpleDrv2();
	int m_preOnTime;
	int mVer;
	int mSensorDev;

};
