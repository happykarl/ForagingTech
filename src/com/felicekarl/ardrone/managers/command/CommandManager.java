/**
   ARDroneForP5
   https://github.com/shigeodayo/ARDroneForP5
   Copyright (C) 2013, Shigeo YOSHIDA.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.felicekarl.ardrone.managers.command;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.felicekarl.ardrone.ARDroneConstants;
import com.felicekarl.ardrone.managers.AbstractManager;

import android.util.Log;

public abstract class CommandManager extends AbstractManager {
	private static final String TAG = CommandManager.class.getSimpleName();
	protected static final String CR = "\r";
	protected static final String SEQ = "$SEQ$";

	private static int seq = 1;

	private FloatBuffer fb;
	private IntBuffer ib;

	private boolean landing = true;
	
	private boolean continuance = false;
	private String command;

	/** speed */
	private float speed = 0.05f; // 0.01f - 1.0f

	protected String VIDEO_CODEC;

	public CommandManager(InetAddress inetaddr) {
		this.inetaddr = inetaddr;

		ByteBuffer bb = ByteBuffer.allocate(4);
		fb = bb.asFloatBuffer();
		ib = bb.asIntBuffer();
	}

	public void setHorizontalCamera() {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"0\"";
		continuance = false;
	}

	public void setVerticalCamera() {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"1\"";
		continuance = false;
	}

	public void setHorizontalCameraWithVertical() {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"2\"";
		continuance = false;
	}

	public void setVerticalCameraWithHorizontal() {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"3\"";
		continuance = false;
	}

	public void toggleCamera() {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"4\"";
		continuance = false;
	}

	public void landing() {
		command = "AT*REF=" + SEQ + ",290717696";
		continuance = false;
		landing = true;
	}

	public void takeOff() {
		command = "AT*REF=" + SEQ + ",290718208";
		continuance = false;
		landing = false;
	}

	public void reset() {
		command = "AT*REF=" + SEQ + ",290717952";
		continuance = true;
		landing = true;
	}

	public void forward() {
		command = "AT*PCMD=" + SEQ + ",1,0," + intOfFloat(-speed) + ",0,0"
				+ "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void forward(int speed) {
		setSpeed(speed);
		forward();
	}

	public void backward() {
		command = "AT*PCMD=" + SEQ + ",1,0," + intOfFloat(speed) + ",0,0"
				+ "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void backward(int speed) {
		setSpeed(speed);
		backward();
	}

	public void spinRight() {
		command = "AT*PCMD=" + SEQ + ",1,0,0,0," + intOfFloat(speed) + "\r"
				+ "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void spinRight(int speed) {
		setSpeed(speed);
		spinRight();
	}

	public void spinLeft() {
		command = "AT*PCMD=" + SEQ + ",1,0,0,0," + intOfFloat(-speed) + "\r"
				+ "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void spinLeft(int speed) {
		setSpeed(speed);
		spinLeft();
	}

	public void up() {
		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(0) + ","
				+ intOfFloat(0) + "," + intOfFloat(speed) + "," + intOfFloat(0)
				+ "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void up(int speed) {
		setSpeed(speed);
		up();
	}

	public void down() {
		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(0) + ","
				+ intOfFloat(0) + "," + intOfFloat(-speed) + ","
				+ intOfFloat(0) + "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void down(int speed) {
		setSpeed(speed);
		down();
	}

	public void goRight() {
		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(speed) + ",0,0,0"
				+ "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void goRight(int speed) {
		setSpeed(speed);
		goRight();
	}

	public void goLeft() {
		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(-speed) + ",0,0,0"
				+ "\r" + "AT*REF=" + SEQ + ",290718208";
		continuance = true;
	}

	public void goLeft(int speed) {
		setSpeed(speed);
		goLeft();
	}

	public void stop() {
		command = "AT*PCMD=" + SEQ + ",1,0,0,0,0";
		continuance = true;
	}

	public void setSpeed(int speed) {
		if (speed > 100)
			speed = 100;
		else if (speed < 1)
			speed = 1;

		this.speed = (float) (speed / 100.0);
	}

	public void enableVideoData() {
		command = "AT*CONFIG=" + SEQ + ",\"general:video_enable\",\"TRUE\""
				+ CR + "AT*FTRIM=" + SEQ;
		continuance = false;
	}
	
	public void disableDemoData() {
		command = "AT*CONFIG=" + SEQ + ",\"general:navdata_demo\",\"FALSE\""
				+ CR + "AT*FTRIM=" + SEQ;
		continuance = false;
	}
	
	public void enableDemoData() {
		command = "AT*CONFIG=" + SEQ + ",\"general:navdata_demo\",\"TRUE\""
				+ CR + "AT*FTRIM=" + SEQ;
		continuance = false;
	}
	
	public void setDefaultDemoOptions() {
		int navdataOptions = navdataOptionMask(ARDroneConstants.NAVDATA_DEMO_TAG) | navdataOptionMask(ARDroneConstants.NAVDATA_MAGNETO_TAG);
		command = "AT*CONFIG=" + SEQ + ",\"general:navdata_options\",\"" + navdataOptions + "\""
				+ CR + "AT*FTRIM=" + SEQ;
		continuance = false;
	}

	public void disableBootStrap() {
		command = "AT*CONFIG_IDS=" + SEQ + ",\"" + ARDroneConstants.SESSION_ID
				+ "\",\"" + ARDroneConstants.PROFILE_ID + "\",\""
				+ ARDroneConstants.APPLICATION_ID + "\"" + CR;
	}

	public void sendControlAck() {
		command = "AT*CTRL=" + SEQ + ",0";
		continuance = false;
	}

	public int getSpeed() {
		return (int) (speed * 100);
	}

	public void disableAutomaticVideoBitrate() {
		command = "AT*CONFIG=" + SEQ + ",\"video:bitrate_control_mode\",\"0\"";
		continuance = false;
	}
	
	public void enableAutomaticVideoBitrate() {
		command = "AT*CONFIG=" + SEQ + ",\"video:bitrate_control_mode\",\"1\"";
		continuance = false;
	}
	
	public void fixVideoBitrate(int rate) {
		command = "AT*CONFIG=" + SEQ + ",\"video:max_bitrate\",\""+ rate + "\"";
		continuance = false;
	}
	
	public void fixVideoFPSrate(int rate) {
		command = "AT*CONFIG=" + SEQ + ",\"video:codec_fps\",\"" + rate + "\"";
		continuance = false;
	}
	
	public void setVideoCodec(String codec) {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_codec\"," + "\"" + codec + "\"";
		continuance = false;
	}
	
	public void setVideoChannel(int channel) {
		command = "AT*CONFIG=" + SEQ + ",\"video:video_channel\",\"" + channel + "\"";
		continuance = false;
	}
	
	

	public void setMaxAltitude(int altitude) {
		command = "AT*CONFIG=" + SEQ + ",\"control:altitude_max\",\""
				+ altitude + "\"";
		continuance = false;
	}

	public void setMinAltitude(int altitude) {
		command = "AT*CONFIG=" + SEQ + ",\"control:altitude_min\",\""
				+ altitude + "\"";
		continuance = false;
	}
	
	public void calibMagnetometer() {
		Log.d(TAG, "Send Calibration Command");
		command ="AT*CALIB=" + SEQ + ",0";
		continuance = false;
	}
	
	public void move3D(int speedX, int speedY, int speedZ, int speedSpin) {
		int maxSpeed = 100;
		if (speedX > maxSpeed)
			speedX = maxSpeed;
		else if (speedX < -maxSpeed)
			speedX = -maxSpeed;
		if (speedY > maxSpeed)
			speedY = maxSpeed;
		else if (speedY < -maxSpeed)
			speedY = -maxSpeed;
		if (speedZ > 100)
			speedZ = 100;
		else if (speedZ < -100)
			speedZ = -100;

		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(-speedY / 100.0f) + ","
				+ intOfFloat(-speedX / 100.0f) + ","
				+ intOfFloat(-speedZ / 100.0f) + ","
				+ intOfFloat(-speedSpin / 100.0f) + "\r" + "AT*REF=" + SEQ
				+ ",290718208";
		continuance = true;
	}
	
	public void move3DNav(int speedX, int speedY, int speedZ, int speedSpin) {
		int maxSpeed = 15;
		if (speedX > maxSpeed)
			speedX = maxSpeed;
		else if (speedX < -maxSpeed)
			speedX = -maxSpeed;
		if (speedY > maxSpeed)
			speedY = maxSpeed;
		else if (speedY < -maxSpeed)
			speedY = -maxSpeed;
		if (speedZ > 100)
			speedZ = 100;
		else if (speedZ < -100)
			speedZ = -100;

		command = "AT*PCMD=" + SEQ + ",1," + intOfFloat(-speedY / 100.0f) + ","
				+ intOfFloat(-speedX / 100.0f) + ","
				+ intOfFloat(-speedZ / 100.0f) + ","
				+ intOfFloat(-speedSpin / 100.0f) + "\r" + "AT*REF=" + SEQ
				+ ",290718208";
		continuance = true;
	}
	// TODO: Mag mode doesn't work
	public void move3D_Mag(int speedX, int speedY, int speedZ, int speedSpin, double psi, double psiAccuracy) {
		if (speedX > 100)
			speedX = 100;
		else if (speedX < -100)
			speedX = -100;
		if (speedY > 100)
			speedY = 100;
		else if (speedY < -100)
			speedY = -100;
		if (speedZ > 100)
			speedZ = 100;
		else if (speedZ < -100)
			speedZ = -100;
		
		if (psi > 1)
			psi = 1;
		else if (psi < -1)
			psi = -1;
		if (psiAccuracy > 1)
			psiAccuracy = 1;
		else if (psiAccuracy < -1)
			psiAccuracy = -1;

		command = "AT*PCMD_MAG=" + SEQ + ",1," + intOfFloat(-speedY / 100.0f) + ","
				+ intOfFloat(-speedX / 100.0f) + ","
				+ intOfFloat(-speedZ / 100.0f) + ","
				+ intOfFloat(-speedSpin / 100.0f) + ","
				+ psi + ","
				+ psiAccuracy + ",\r" + "AT*REF=" + SEQ
				+ ",290718212";
		continuance = true;
	}

	@Override
	public void run() {
		initializeDrone();
		try {
			socket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		while (!Thread.currentThread().isInterrupted()) {
			if (this.command != null) {
				// sendCommand();
				sendCommand(this.command);
				if (!continuance) {
					command = null;
				}
			} else {
				if (landing) {
					sendCommand("AT*PCMD=" + SEQ + ",1,0,0,0,0" + CR
							+ "AT*REF=" + SEQ + ",290717696");
				} else {
					sendCommand("AT*PCMD=" + SEQ + ",1,0,0,0,0" + CR
							+ "AT*REF=" + SEQ + ",290718208");
				}
			}

			try {
				Thread.sleep(20); // < 50ms
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (seq % 5 == 0) { // < 2000ms
				sendCommand("AT*COMWDG=" + SEQ);
			}

		}
	}

	protected abstract void initializeDrone();

	/*
	 * private void initializeDrone() { sendCommand("AT*CONFIG=" + SEQ +
	 * ",\"general:navdata_demo\",\"TRUE\"" + CR + "AT*FTRIM=" + SEQ); // 1
	 * sendCommand("AT*PMODE=" + SEQ + ",2" + CR + "AT*MISC=" + SEQ +
	 * ",2,20,2000,3000" + CR + "AT*FTRIM=" + SEQ + CR + "AT*REF=" + SEQ +
	 * ",290717696"); // 2-5 sendCommand("AT*PCMD=" + SEQ + ",1,0,0,0,0" + CR +
	 * "AT*REF=" + SEQ + ",290717696" + CR + "AT*COMWDG=" + SEQ); // 6-8
	 * sendCommand("AT*PCMD=" + SEQ + ",1,0,0,0,0" + CR + "AT*REF=" + SEQ +
	 * ",290717696" + CR + "AT*COMWDG=" + SEQ); // 6-8 sendCommand("AT*FTRIM=" +
	 * SEQ); //System.out.println("Initialize completed!"); }
	 */

	/*
	 * Thank you Dirk !! 
	 */
	protected synchronized void sendCommand(String command) {
		//Log.d("CMD", command);
		int seqIndex = -1;
		while ((seqIndex = command.indexOf(SEQ)) != -1)
			command = command.substring(0, seqIndex) + (seq++)
					+ command.substring(seqIndex + SEQ.length());

		byte[] buffer = (command + CR).getBytes();
		//Log.d(TAG, command);
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
				inetaddr, ARDroneConstants.PORT);

		try {
			socket.send(packet);
			 //Thread.sleep(20); // < 50ms
		} catch (IOException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} /*catch (InterruptedException e) {
			e.printStackTrace();
		}*/
	}

	private int intOfFloat(float f) {
		fb.put(0, f);
		return ib.get(0);
	}
	
	public static int navdataOptionMask(int c) {
		return 1 << c;
	}
	
	@Override
	public void close() {
		Thread.currentThread().interrupt();
		socket.close();
	}
	
	
}