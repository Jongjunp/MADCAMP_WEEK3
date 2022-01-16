package com.pjinkim.arcore_data_logger;

public class UserInfo {
    public  static UserInfo instance = new UserInfo();
    public float x,y,z;
    public float q_x,q_y,q_z,q_w;

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
    public void setrelateRot(float a, float b, float c, float d)
    {
        q_x = a;
        q_y = b;
        q_z = c;
        q_w = d;

        return;
    }
    public String printLoc()
    {
        return String.format("x =%2.2f,  y =%2.2f,  z =%2.2f",x,y,z);
    }
}
