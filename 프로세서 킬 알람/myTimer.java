package timeProject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;


public class myTimer {
	public static void main(String[] args) throws IOException {
		/*
		int sec = 4;
		int min = 0;
		int hour = 0;
		
		alarm a = new alarm();
		a.start();
		a.createAlarm(hour, min, sec);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		a.Stop_Player();
		*/
	}
}

class alarm extends Thread{
	public Date nowTime = new Date();
	private long timeTemp;
	private long nowTimeInProgram;
	private PauseAblePlayer player;
	ArrayList<Long> alarmList = new ArrayList<Long>();
	
	private final Object keyStone_player = new Object();
	private boolean playlogic, serverLogic;
	
	public alarm() {
		try {
			timeTemp = timeCheck();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(timeTemp > 0) {
			serverLogic = true;
			nowTimeInProgram = timeTemp;
		}else {
			serverLogic = false;
			nowTimeInProgram = System.currentTimeMillis();
		}
		//setDaemon(false);
	}
	
	//서버에서 시간 받아오는 기능
	public long timeCheck() throws IOException {
		URL url = new URL("http://time.google.com/"); 
		URLConnection conn = url.openConnection(); 
		if (conn instanceof HttpURLConnection) { 
		    HttpURLConnection httpConn = (HttpURLConnection)conn; 
		    httpConn.setRequestMethod("HEAD"); 
		} 
		long dateTime = conn.getHeaderFieldDate("Date", 0); 
		if (dateTime > 0) { 
			return dateTime;
		} 
		return -1;
	}
	
	private void checkAlarm() {
		for(int i=0;i<alarmList.size();i++) {
			if(alarmList.get(i) <= nowTimeInProgram) {
				synchronized(keyStone_player) {
					alarmList.remove(i);
					TrrigePlayer();
				}
			}
		}
	}
	
	private void TrrigePlayer() {
		try {
            FileInputStream input = new FileInputStream("./pa.mp3"); 
            player = new PauseAblePlayer(input, false);
            player.play();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
	}
	
	public void Stop_Player(){
		try{
			if(player == null) {
				System.out.println("wrong access");
				return;
			}
			player.stop();	
		}catch(Exception e) {
			System.out.println(e);
		}
	}
	
	public void exitAlarm() {
		interrupt();
	}
	
	public void createAlarm(int hour, int min, int sec) {
		long temp = nowTimeInProgram;
		temp += (hour*3600000) + (min*60000) + (sec*1000);
		alarmList.add(temp);
	}
	
	public long nowTimeLong() {
		return nowTimeInProgram;
	}
	
	@Override
	public void run() {
		byte count = 0;
		
		//==============초단위 클럭========================
		Thread ka = new Thread() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					nowTimeInProgram += 1000;
				}
					
			}
		};
		ka.setDaemon(true);
		ka.start();
		
		
		//================알람 시간 설정 및 기능들========================
		if(serverLogic) {
			//웹 활성화 시,
			while(true) {
				try {
					Thread.sleep(1000);
					if(count > 60) {
						if((timeTemp = timeCheck()) > 0) {
							nowTimeInProgram = timeTemp;
						}
						count = 0;
					}
					count++;
					checkAlarm();
					nowTime.setTime(nowTimeInProgram);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch(IOException e) {
					
				}
			}
		}
		else {
			while(true) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				nowTimeInProgram = System.currentTimeMillis();
				nowTime.setTime(nowTimeInProgram);
			}
		}
	}
	

	class WindowProcessKill{
		//only Window Code
		public ArrayList processList() {
			ArrayList processes = new ArrayList();
	        try{
	            String line;
	            Process p = Runtime.getRuntime().exec("tasklist.exe /nh");
	            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            while((line = input.readLine()) != null){
	                if (!line.trim().equals("")){
	                    processes.add(line.substring(0, line.indexOf(" ")));
	                }
	            }
	        }catch (Exception e){
	
	            e.printStackTrace();
	
	        }
	        return processes;
		}
		
		public void processkill(String taskName) throws IOException {
			Runtime.getRuntime().exec("taskkill /F /IM " + taskName);
		}
	}
}

