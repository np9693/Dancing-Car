package com.example.android.dancecar.spotifyservice;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.spotify.android.appremote.api.SpotifyAppRemote;

public class TrackPlayerStateTask extends AsyncTask<Object, Void, Void> {

    @Override
    protected Void doInBackground(Object... params) {
        SpotifyAppRemote spotifyAppRemote = (SpotifyAppRemote) params[0];
        TextView displayText = (TextView)params[1];
        while (!isCancelled())
        {
            spotifyAppRemote.getPlayerApi().getPlayerState()
                    .setResultCallback(playerState -> {
                        // have fun with playerState
                        //Log.d("MainActivity", playerState.track.name + " by "   +   playerState.track.artist.name + " (Playback position: " + playerState.playbackPosition+ ")");
                        long positionMilliseconds = playerState.playbackPosition;
                        long minutes = positionMilliseconds / 60000;
                        long seconds = (positionMilliseconds % 60000) / 1000;
                        displayText.setText(minutes + ":" + seconds);
                    })
                    .setErrorCallback(throwable -> {
                        // =(
                    });
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
        return  null;
    }
}

