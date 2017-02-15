package com.lunchareas.divertio;


import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;

public class PlayMusicService extends Service {

    public static final String PLAYMUSIC_RESULT = "REQUEST_PROCESSED";
    public static final String PLAYMUSIC_POSITION = "POSITION";
    public static final String PLAYMUSIC_DURATION = "DURATION";

    private Bundle intentCmd;
    private static MediaPlayer mp = null;

    // set up broadcaster to activity to update progress bar
    private LocalBroadcastManager musicUpdater;
    private Thread musicUpdaterThread;

    @Override
    public int onStartCommand(Intent workIntent, int flags, int startId) {
        if (workIntent != null) {
            intentCmd = workIntent.getExtras();
        }

        // create the thread to update progress bar
        musicUpdater = LocalBroadcastManager.getInstance(this);
        musicUpdaterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int songPosition;
                int songDuration;
                do {
                    // change duration if track changes
                    songPosition = mp.getCurrentPosition();
                    songDuration = mp.getDuration();
                    //System.out.println("Song service position: " + songPosition + "\nSong service duration: " + songDuration);

                    // create and send intent with position and duration
                    Intent songIntent = new Intent(PLAYMUSIC_RESULT);
                    songIntent.putExtra(PLAYMUSIC_POSITION, songPosition);
                    songIntent.putExtra(PLAYMUSIC_DURATION, songDuration);
                    musicUpdater.sendBroadcast(songIntent);
                    musicUpdater.sendBroadcast(songIntent);
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (mp != null && mp.getCurrentPosition() <= songDuration);
            }
        });

        if (intentCmd != null) {
            if (intentCmd.containsKey("CREATE")) {
                System.out.println("Create Key: " + intentCmd.getString("CREATE"));
                initMusicPlayer(intentCmd.getString("CREATE"));
                mp.start();
                musicUpdaterThread.start();
            } else if (intentCmd.containsKey("START") && mp != null) {
                mp.start();
                musicUpdaterThread.start();
            } else if (intentCmd.containsKey("PAUSE") && mp != null) {
                mp.pause();
            } else if (intentCmd.containsKey("CHANGE") && mp != null) {
                int newPosition = intentCmd.getInt("CHANGE");
                mp.seekTo(newPosition);
            } else {
                System.err.println("ERROR: Command sent to PlayMusicService not found.");
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onDestroy() {
        System.out.println("MUSICPLAYER: PlayMusicService destroyed...");
        mp.release();
        mp = null;
        musicUpdaterThread.interrupt();
    }

    public void initMusicPlayer(String path) {
        if (path != null) {
            mp = MediaPlayer.create(this, Uri.parse(path));
            if (mp != null) {
                mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }
    }

    public Thread getMusicUpdaterThread() {
        return musicUpdaterThread;
    }

    public void setMusicUpdaterThread (Thread t) {
        musicUpdaterThread = t;
    }

    /*
    @Override
    protected void onHandleIntent(Intent workIntent) {
        String dataString = workIntent.getDataString();
        Integer songId = new Integer(dataString);

        songPlayer = MediaPlayer.create(this, songId);
        songPlayer.setLooping(true);
        songPlayer.start();
    }
    */
}
