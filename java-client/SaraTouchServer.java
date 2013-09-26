import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.ConnectException;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import PQSDKMultiTouch.*;

/**
 * @author PQLabs
 *
 */
public class SaraTouchServer extends PQMTClient
{
	public static void main(String[] args)
        throws Exception
    {
	    JFrame f = new JFrame("PQ test");

	    f.setSize(1024, 768);

	    //exit application when window is closed.
	    f.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });

		SaraTouchServer testClient = new SaraTouchServer();
		testClient.Init();
		//f.pack();
		f.setVisible(true);
	}

	public SaraTouchServer()
	{
	}


	/**
	 * connect to server and setup client request type
	 * */
	public int Init()
        throws Exception
    {
		int err_code=PQMTClientConstant.PQ_MT_SUCESS;
		try{
			if((err_code=ConnectServer())!=PQMTClientConstant.PQ_MT_SUCESS)
			{
				JOptionPane.showMessageDialog(null, "connect server fail, socket error code:"+err_code);
				return err_code;
			}
		}catch(ConnectException ex){
			JOptionPane.showMessageDialog(null, "Please Run the PQLabs MultiTouch Platform(Server) first.");
			return err_code;
		}

		TouchClientRequest clientRequest = new TouchClientRequest();
        // XXX why oh why is this needed?
		clientRequest.app_id = GetTrialAppID();

		try
        {
            clientRequest.type = PQMTClientConstant.RQST_RAWDATA_ALL | PQMTClientConstant.RQST_GESTURE_ALL;
            if ((err_code=SendRequest(clientRequest)) != PQMTClientConstant.PQ_MT_SUCESS)
            {
                JOptionPane.showMessageDialog(null, "send request  fail,  error code:"+err_code);
                return err_code;
            }
            if ((err_code=GetServerResolution()) != PQMTClientConstant.PQ_MT_SUCESS)
            {
                JOptionPane.showMessageDialog(null, "get server resolution fail,  error code:"+err_code);
                return err_code;
            }
            System.out.println("connected, start receive:"+err_code);
		}
        catch(Exception ex)
        {
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}
		return err_code;
	}

	public int OnTouchFrame(int frame_id, int time_stamp, Vector<TouchPoint> point_list)
    {
        System.out.println("OnTouchFrame(): frame_id = "+frame_id+", time_stap = "+time_stamp);

	    for(TouchPoint point:point_list )
		{
			String message = "Point: id "+point.m_id+", event = "+point.m_point_event+", pos = "+point.m_x+", "+point.m_y+", with size "+point.m_dx+"*"+point.m_dy;
			System.out.println(message);
		}

        return PQMTClientConstant.PQ_MT_SUCESS;
	}

    /*
	public int OnTouchGesture(TouchGesture touch_gesture)
	{
		if (touch_gesture.m_type == PQMTClientConstant.TG_NO_ACTION)
        {
			return PQMTClientConstant.PQ_MT_SUCESS;
		}
//		String message="Gesture "+touch_gesture.m_type + GetGestureName(touch_gesture) +" come with params ";
//		for (int i = 0; i < touch_gesture. m_params.size(); i++) {
//			Double param = touch_gesture.m_params.get(i);
//			message += param + " ";
//		}
//		message += "\n";
//		System.out.println(message);
		switch(touch_gesture.m_type)
		{
		case PQMTClientConstant.TG_DOWN:
			//imageCompent.startMove(touch_gesture.m_params.get(0).intValue(), touch_gesture.m_params.get(1).intValue());
			break;
		case PQMTClientConstant.TG_MOVE:
			//imageCompent.touchmove(touch_gesture.m_params.get(0).intValue(), touch_gesture.m_params.get(1).intValue());
			break;
		case PQMTClientConstant.TG_SPLIT_APART:
			break;
		case PQMTClientConstant.TG_SPLIT_CLOSE:
			break;
		default:
				break;
		}

		return PQMTClientConstant.PQ_MT_SUCESS;
	}
    */
}
