class TuioTouchHistory
{
    public TuioTouchHistory(double t, int id, float x, float y, float vx, float vy)
    {
        timestamp = t;
        this.id = id;
        last_x = x;
        last_y = y;
        last_xvel = vx;
        last_yvel = vy;
    }

    public int  id;
    public double timestamp;
    public float last_x, last_y;
    public float last_xvel, last_yvel;
};
