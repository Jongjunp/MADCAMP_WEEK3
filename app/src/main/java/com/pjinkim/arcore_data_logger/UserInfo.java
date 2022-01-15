package com.pjinkim.arcore_data_logger;

public class UserInfo {
    public  static UserInfo instance = new UserInfo();
    float x,y,z;
    int q_x,q_y,q_z,q_w;

    public void setAbsoluteLoc(float a, float b, float c)
    {

    }

    public void setrelateLoc(float a, float b, float c)
    {
        x=a;
        y=b;
        z=c;

        return;
    }
    public void setrelateRot(int a, int b, int c, int d)
    {
        x=a;
        y=b;
        z=c;

        return;
    }
    public String printLoc()
    {
        return String.format("x =%2.2f,  y =%2.2f,  z =%2.2f",x,y,z);
    }
}
