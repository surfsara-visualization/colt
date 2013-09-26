import java.io.*;
import java.net.*;
import java.util.*;

class ConnectionHandler implements Runnable
{
    public ConnectionHandler(TouchEventHandler handler, Socket socket, String host, int port)
    {
        this.socket = socket;
        this.host = host;
        this.port = port;
        this.handler = handler;
    }

    public void run()
    {
        // InputStream.read(buf, offs, len)
        InputStream is;
        int     read, left_to_read;
        byte[]  length_buffer = new byte[4];
        byte[]  message_buffer = new byte[2048];
        int     message_length;

        int     num_touches;

        ByteBufferView view = new ByteBufferView(length_buffer, 4);

        byte[]  header_buffer = new byte[8];

        int touch_id;
        int touch_state;
        byte    touch_width, touch_height;
        float   touch_pos_x, touch_pos_y;
        float   touch_vel_x, touch_vel_y;
        float   touch_accel;

        double              timestamp;
        TouchPoint          point;

        TouchPoint[]        points;

        // XXX nicely hardcoded :)
        points = new TouchPoint[16];
        for (int i = 0; i < 16; i++)
            points[i] = new TouchPoint();

        try
        {
            is = socket.getInputStream();

            //readFully(is, header_buffer, 6);

            while (true)
            {
                if (!readFully(is, length_buffer, 4))
                    return;

                view.initialize(length_buffer, 4);
                message_length = view.getInt();
                //System.out.println("message_length: "+message_length);

                if (!readFully(is, message_buffer, message_length))
                    return;

                view.initialize(message_buffer, message_length);
                timestamp = view.getDouble();
                //System.out.println("timestamp "+timestamp);
                num_touches = view.getInt();
                //System.out.println("num touches "+num_touches);

                for (int i = 0; i < num_touches; i++)
                {
                    points[i].id = view.getInt();
                    points[i].state = view.getInt();
                    points[i].tx = view.getFloat();
                    points[i].ty = view.getFloat();
                }

                if (handler != null)
                    handler.OnTouchPoints(timestamp, points, num_touches);
            }

        }
        catch (Exception e)
        {
            System.out.println("Exception: "+e);
            return;
        }
    }

    private boolean readFully(InputStream is, byte[] buffer, int n)
        throws IOException
    {
        int read;
        int offset = 0;

        while (n > 0)
        {
            read = is.read(buffer, offset, n);
            if (read == -1)
            {
                System.out.println("readFully(): read = -1");
                return false;
            }
            n -= read;
            offset += read;
        }

        return true;
    }

    String              host;
    int                 port;
    protected Socket    socket;
    protected TouchEventHandler handler;
};
