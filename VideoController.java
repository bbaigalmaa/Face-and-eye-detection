package it.polito.teaching.cv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.win32.WinUser.WINDOWINFO;

import it.polito.elite.teaching.cv.utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class VideoController
{
	// the FXML button
	@FXML
	private Button button;
	
	@FXML
	private ImageView currentFrame;
	
	private ScheduledExecutorService timer;
	
	private VideoCapture capture;
	
	private boolean cameraActive;
	
	private Mat logo;
	  String face_cascade_name = "C:/opencv/build/etc/haarcascades/haarcascade_frontalface_alt.xml";
      String eyes_cascade_name = "C:/opencv/build/etc/haarcascades/haarcascade_eye_tree_eyeglasses.xml";
      CascadeClassifier face_cascade = new CascadeClassifier();
      CascadeClassifier eyes_cascade = new CascadeClassifier();
	
	
	public void initialize()
	{
		this.capture = new VideoCapture(0);
		this.cameraActive = false;
	}
	
		@FXML
	protected void startCamera(ActionEvent event)
	{
		
		this.currentFrame.setFitWidth(600);
		
		this.currentFrame.setPreserveRatio(true);
		
		if (!this.cameraActive)
		{
		
			this.capture.open(0);
			
			
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				if(!face_cascade.load(face_cascade_name))
		        {
		            System.out.println("Error loading face cascade");
		        }
		        else
		        {
		            System.out.println("Success loading face cascade");
		        }

		     
		        if(!eyes_cascade.load(eyes_cascade_name))
		        {
		            System.out.println("Error loading eyes cascade");
		        }
		        else
		        {
		            System.out.println("Success loading eyes cascade");
		        }

			
				Runnable frameGrabber = new Runnable() {
	
					@Override
					public void run()
					{
						Mat frame = new Mat();
				        capture.read(frame);
				        Mat frame_gray = new Mat();
				        Imgproc.cvtColor(frame,frame_gray, Imgproc.COLOR_RGBA2GRAY);
				        Imgproc.equalizeHist(frame_gray, frame_gray);


				        MatOfRect faces = new MatOfRect();
				       
				        face_cascade.detectMultiScale(frame_gray, faces, 1.1, 2, 0, new Size(30,30), new Size() );


				        Rect[] facesArray = faces.toArray();

				        for(int i=0; i<facesArray.length; i++)
				        {
				            Point center = new Point(facesArray[i].x + facesArray[i].width * 0.5, facesArray[i].y + facesArray[i].height * 0.5);
				            Imgproc.ellipse(frame, center, new Size(facesArray[i].width * 0.5, facesArray[i].height * 0.5), 0, 0, 360, new Scalar(255, 0, 255), 4, 8, 0);
				           
				             Mat faceROI = frame_gray.submat(facesArray[i]);
				             MatOfRect eyes = new MatOfRect();
				            
				             eyes_cascade.detectMultiScale(faceROI, eyes, 1.1, 2, 0,new Size(30,30), new Size());
				             

				             Rect[] eyesArray = eyes.toArray();
				             Point[] eyesPoint = new Point[2];
				             int[] leftEye = new int[2];
				             int[] rightEye = new int[2];
				             
				             leftEye[0] = eyesArray[0].x;
				             leftEye[1] = eyesArray[0].y;
				             
				             rightEye[0] = eyesArray[1].x;
				             rightEye[1] = eyesArray[1].y;
				             
				             if(leftEye[0] > 150 && rightEye[0] <= 100){
				            	 System.out.println("left eye");
				             }else if(leftEye[0] <= 100 && rightEye[0] >= 120){
				            	 System.out.println("right eye");
				             }
				            
//				             System.out.println(leftEye[0] + " " +  leftEye[1]);
//				            System.out.println(rightEye[0] + " " + rightEye[1]);
				             
				             for (int j = 0; j < eyesArray.length; j++)
				             {
				                Point center1 = new Point(facesArray[i].x + eyesArray[j].x + eyesArray[j].width * 0.5, facesArray[i].y + eyesArray[j].y + eyesArray[j].height * 0.5);
				                
				                eyesPoint[j] = center1;
				                
				                int radius = (int) Math.round((eyesArray[j].width + eyesArray[j].height) * 0.25);
				                Imgproc.circle(frame, center1, radius, new Scalar(255, 0, 0), 4, 8, 0);
				                Imgproc.line(frame, eyesPoint[0], eyesPoint[0],  new Scalar(255, 0, 255));
				             }
				            
				        }
			          
						
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
						
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				
				this.button.setText("Stop Camera");
			}
			else
			{
				
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			
			this.cameraActive = false;
			
			this.button.setText("Start Camera");
			
			
			this.stopAcquisition();
		}
	}
	
	
	private void stopAcquisition()
	{
		if (this.timer != null && !this.timer.isShutdown())
		{
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			this.capture.release();
		}
	}

	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}
