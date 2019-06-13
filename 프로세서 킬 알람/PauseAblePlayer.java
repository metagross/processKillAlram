package timeProject;

import java.io.InputStream;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.Player;

//수입 후 우덜화 stackOverflow
public class PauseAblePlayer {
	String toStr[] = {"NOTSTARTED", "PLAYING", "PAUSED", "FINISHED"};
    private final static int NOTSTARTED = 0;
    private final static int PLAYING = 1;
    private final static int PAUSED = 2;
    private final static int FINISHED = 3;

    // the player actually doing all the work
    private final Player player;

    // locking object used to communicate with player thread
    private final Object playerLock = new Object();

    // status variable what player thread is doing/supposed to do
    private int playerStatus = NOTSTARTED;
    private boolean daemonLogic;
    
    public PauseAblePlayer(final InputStream inputStream, boolean daemonLogic) throws JavaLayerException {
        this.player = new Player(inputStream);
        this.daemonLogic = daemonLogic;
    }

    public PauseAblePlayer(final InputStream inputStream, final AudioDevice audioDevice, boolean daemonLogic) throws JavaLayerException {
        this.player = new Player(inputStream, audioDevice);
    }

    /**
     * Starts playback (resumes if paused)
     */
    public void play() throws JavaLayerException {
        synchronized (playerLock) {
            switch (playerStatus) {
                case NOTSTARTED:
                    final Runnable r = new Runnable() {
                        public void run() {
                            playInternal();
                        }
                    };
                    final Thread t = new Thread(r);
                    t.setPriority(Thread.MAX_PRIORITY);
                    if(daemonLogic) {
                    	t.setDaemon(true);
                    }
                    playerStatus = PLAYING;
                    t.start();
                    break;
                case PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Pauses playback. Returns true if new state is PAUSED.
     */
    public boolean pause() {
        synchronized (playerLock) {
            if (playerStatus == PLAYING) {
                playerStatus = PAUSED;
            }
            return playerStatus == PAUSED;
        }
    }

    /**
     * Resumes playback. Returns true if the new state is PLAYING.
     */
    public boolean resume() {
        synchronized (playerLock) {
            if (playerStatus == PAUSED) {
                playerStatus = PLAYING;
                playerLock.notifyAll();
            }
            return playerStatus == PLAYING;
        }
    }

    /**
     * Stops playback. If not playing, does nothing
     */
    public void stop() {
        synchronized (playerLock) {
            playerStatus = FINISHED;
            playerLock.notifyAll();
        }
    }

    private void playInternal() {
        while (playerStatus != FINISHED) {
            try {
                if (!player.play(1)) {
                    break;
                }
            } catch (final JavaLayerException e) {
                break;
            }
            // check if paused or terminated
            synchronized (playerLock) {
                while (playerStatus == PAUSED) {
                    try {
                        playerLock.wait();
                    } catch (final InterruptedException e) {
                        // terminate player
                        break;
                    }
                }
            }
        }
        close();
    }
    
    public void setDaemonLogic(boolean logic) {
    	synchronized (playerLock) {
    		daemonLogic = logic;
    	}
    }
    
    public int getPlayerStatus() {
    	return playerStatus;
    }
    
    public void close() {
        synchronized (playerLock) {
            playerStatus = FINISHED;
        }
        try {
            player.close();
        } catch (final Exception e) {
            // ignore, we are terminating anyway
        }
    }
    
    public String toString() {
    	return toStr[playerStatus];
    }

    // demo how to use
    /*
    public static void main(String[] argv) {
        try {
            FileInputStream input = new FileInputStream("myfile.mp3"); 
            PausablePlayer player = new PausablePlayer(input);

            // start playing
            player.play();

            // after 5 secs, pause
            Thread.sleep(5000);
            player.pause();     

            // after 5 secs, resume
            Thread.sleep(5000);
            player.resume();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }*/

}