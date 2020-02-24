package com.procoderforu.lyricsify;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;


import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.ContentApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.ListItem;
import com.spotify.protocol.types.PlaybackSpeed;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Repeat;


import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String REDIRECT_URI = "https://procoderforu.com/callback";
    static String trackName = "", artistName = "";


    private static String CLIENT_ID = "f40f4e1e8e4943bc85885b75dabb43bb";

    Button mConnectButton, mConnectAuthorizeButton, mDisconnectButton;
    Button mPlayerStateButton;
    ImageView mCoverArtImageView;

    AppCompatImageButton mToggleShuffleButton;
    AppCompatImageButton mPlayPauseButton;
    AppCompatImageButton mToggleRepeatButton;
    AppCompatSeekBar mSeekBar;

    AppCompatTextView mHeadTrack, mLyricsTV;

    List<View> mViews;
    TrackProgressBar mTrackProgressBar;

    public static SpotifyAppRemote mSpotifyAppRemote;
    public final ErrorCallback mErrorCallback = this::logError;

    Subscription<PlayerState> mPlayerStateSubscription;
    Subscription<PlayerContext> mPlayerContextSubscription;
    Subscription<Capabilities> mCapabilitiesSubscription;


    private final Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback =
            new Subscription.EventCallback<PlayerContext>() {
                @Override
                public void onEvent(PlayerContext playerContext) {

                }
            };

    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback =
            new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {

                    Drawable drawable =
                            ResourcesCompat.getDrawable(
                                    getResources(), R.drawable.mediaservice_shuffle, getTheme());
                    if (!playerState.playbackOptions.isShuffling) {
                        mToggleShuffleButton.setImageDrawable(drawable);
                        DrawableCompat.setTint(mToggleShuffleButton.getDrawable(), Color.WHITE);
                    } else {
                        mToggleShuffleButton.setImageDrawable(drawable);
                        DrawableCompat.setTint(
                                mToggleShuffleButton.getDrawable(),
                                getResources().getColor(R.color.cat_medium_green));
                    }

                    if (playerState.playbackOptions.repeatMode == Repeat.ALL) {
                        mToggleRepeatButton.setImageResource(R.drawable.mediaservice_repeat_all);
                        DrawableCompat.setTint(
                                mToggleRepeatButton.getDrawable(),
                                getResources().getColor(R.color.cat_medium_green));
                    } else if (playerState.playbackOptions.repeatMode == Repeat.ONE) {
                        mToggleRepeatButton.setImageResource(R.drawable.mediaservice_repeat_one);
                        DrawableCompat.setTint(
                                mToggleRepeatButton.getDrawable(),
                                getResources().getColor(R.color.cat_medium_green));
                    } else {
                        mToggleRepeatButton.setImageResource(R.drawable.mediaservice_repeat_off);
                        DrawableCompat.setTint(mToggleRepeatButton.getDrawable(), Color.WHITE);
                    }

                    trackName = playerState.track.name;
                    artistName = playerState.track.artist.name;


                    mHeadTrack.setText(trackName);

                    String ch = "%20";
                    //String url = "https://api.lyrics.ovh/v1/"+artistName.toLowerCase().replace(" ", ch)+"/"+trackName.toLowerCase().replace(" ", ch);
                    String url = "https://private-anon-ef40f651ea-lyricsovh.apiary-proxy.com/v1/" + artistName.toLowerCase().replace(" ", ch) + "/" + trackName.toLowerCase().replace(" ", ch);
                    //String url = "https://api.lyrics.ovh/v1/"+artistName.toLowerCase()+"/"+trackName.toLowerCase();


                    new getLyrics().execute(url);

                    // setLyrics(trackName, artistName);
                    mPlayerStateButton.setText(
                            String.format(
                                    Locale.US, "%s\n%s", playerState.track.name, playerState.track.artist.name));
                    mPlayerStateButton.setTag(playerState);

                    // Update progressbar
                    if (playerState.playbackSpeed > 0) {
                        mTrackProgressBar.unpause();
                    } else {
                        mTrackProgressBar.pause();
                    }

                    // Invalidate play / pause
                    if (playerState.isPaused) {
                        mPlayPauseButton.setImageResource(R.drawable.btn_play);
                    } else {
                        mPlayPauseButton.setImageResource(R.drawable.btn_pause);
                    }


                    // Invalidate seekbar length and position
                    mSeekBar.setMax((int) playerState.track.duration);
                    mTrackProgressBar.setDuration(playerState.track.duration);
                    mTrackProgressBar.update(playerState.playbackPosition);

                    mSeekBar.setEnabled(true);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mConnectButton = findViewById(R.id.connect_button);
        mConnectAuthorizeButton = findViewById(R.id.connect_authorize_button);
        mDisconnectButton = findViewById(R.id.disconnect_button);

        mLyricsTV = findViewById(R.id.lyricsTV);
        mHeadTrack = findViewById(R.id.headTrack);

        mCoverArtImageView = findViewById(R.id.image);

        mPlayerStateButton = findViewById(R.id.current_track_label);

        mToggleRepeatButton = findViewById(R.id.toggle_repeat_button);
        mToggleShuffleButton = findViewById(R.id.toggle_shuffle_button);
        mPlayPauseButton = findViewById(R.id.play_pause_button);

        mSeekBar = findViewById(R.id.seek_to);
        mSeekBar.setEnabled(false);
        mSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        mSeekBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        mTrackProgressBar = new TrackProgressBar(mSeekBar);

        mViews =
                Arrays.asList(
                        findViewById(R.id.disconnect_button),

                        mPlayPauseButton,
                        findViewById(R.id.seek_forward_button),
                        findViewById(R.id.seek_back_button),
                        findViewById(R.id.skip_prev_button),
                        findViewById(R.id.skip_next_button),
                        mToggleRepeatButton,
                        mToggleShuffleButton,

                        mSeekBar);

        SpotifyAppRemote.setDebugMode(true);

        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDisconnected();
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConnecting();
                connect(true);
            }
        });


    }

    private void onDisconnected() {
        for (View view : mViews) {
            view.setEnabled(false);
        }
        mConnectButton.setEnabled(true);
        mConnectButton.setText("CONNECT");
        mConnectAuthorizeButton.setEnabled(true);
        mConnectAuthorizeButton.setText("CONNECT AND AUTHORIZE");

        mPlayerStateButton.setText("No Track \n Playing");
        mToggleRepeatButton.clearColorFilter();
        mToggleRepeatButton.setImageResource(R.drawable.btn_repeat);
        mToggleShuffleButton.clearColorFilter();
        mToggleShuffleButton.setImageResource(R.drawable.btn_shuffle);


    }

    private void logMessage(String msg) {
        logMessage(msg, Toast.LENGTH_SHORT);
    }

    private void logMessage(String msg, int duration) {
        Toast.makeText(this, msg, duration).show();
        Log.d("random shit", msg);
    }

    public void getPlayerState() {

        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateButton.setVisibility(View.VISIBLE);


        mPlayerStateSubscription =
                (Subscription<PlayerState>)
                        mSpotifyAppRemote
                                .getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(mPlayerStateEventCallback)
                                .setLifecycleCallback(
                                        new Subscription.LifecycleCallback() {
                                            @Override
                                            public void onStart() {
                                                logMessage("Event: start");
                                            }

                                            @Override
                                            public void onStop() {
                                                logMessage("Event: end");
                                            }
                                        })
                                .setErrorCallback(
                                        throwable -> {
                                            mPlayerStateButton.setVisibility(View.INVISIBLE);

                                            logError(throwable);
                                        });
    }

    private void onConnected() {
        for (View input : mViews) {
            input.setEnabled(true);
        }
        mConnectButton.setEnabled(false);
        mConnectButton.setText("Connected");
        mConnectAuthorizeButton.setEnabled(false);
        mConnectAuthorizeButton.setText("Connected");
        getPlayerState();

    }


    private void connect(boolean showAuthView) {

        SpotifyAppRemote.disconnect(mSpotifyAppRemote);

        SpotifyAppRemote.connect(
                getApplication(),
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(showAuthView)
                        .build(),
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        MainActivity.this.onConnected();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        logError(error);
                        MainActivity.this.onDisconnected();
                    }
                });
    }

    public void onToggleShuffleButtonClicked(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .toggleShuffle()
                .setResultCallback(
                        empty -> logMessage(getString(R.string.command_feedback, "toggle shuffle")))
                .setErrorCallback(mErrorCallback);
    }

    public void onToggleRepeatButtonClicked(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .toggleRepeat()
                .setResultCallback(
                        empty -> logMessage(getString(R.string.command_feedback, "toggle repeat")))
                .setErrorCallback(mErrorCallback);
    }

    public void onSkipPreviousButtonClicked(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .skipPrevious()
                .setResultCallback(
                        empty -> logMessage(getString(R.string.command_feedback, "skip previous")))
                .setErrorCallback(mErrorCallback);
    }

    public void onPlayPauseButtonClicked(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .getPlayerState()
                .setResultCallback(
                        playerState -> {
                            if (playerState.isPaused) {
                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .resume()
                                        .setResultCallback(
                                                empty -> logMessage(getString(R.string.command_feedback, "play")))
                                        .setErrorCallback(mErrorCallback);
                            } else {
                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .pause()
                                        .setResultCallback(
                                                empty -> logMessage(getString(R.string.command_feedback, "pause")))
                                        .setErrorCallback(mErrorCallback);
                            }
                        });
    }

    public void onSkipNextButtonClicked(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .skipNext()
                .setResultCallback(data -> logMessage(getString(R.string.command_feedback, "skip next")))
                .setErrorCallback(mErrorCallback);
    }

    public void onSeekBack(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .seekToRelativePosition(-15000)
                .setResultCallback(data -> logMessage(getString(R.string.command_feedback, "seek back")))
                .setErrorCallback(mErrorCallback);
    }

    public void onSeekForward(View view) {
        mSpotifyAppRemote
                .getPlayerApi()
                .seekToRelativePosition(15000)
                .setResultCallback(data -> logMessage(getString(R.string.command_feedback, "seek fwd")))
                .setErrorCallback(mErrorCallback);
    }

    private void onConnecting() {
        mConnectButton.setEnabled(false);
        mConnectButton.setText("Connecting");
        mConnectAuthorizeButton.setEnabled(false);
        mConnectAuthorizeButton.setText("Connecting");
    }

    private void logError(Throwable throwable) {
        Toast.makeText(this, "An error has occurred.", Toast.LENGTH_SHORT).show();
        Log.e(MainActivity.class.getSimpleName(), "", throwable);
    }


    public class getLyrics extends AsyncTask<String, Integer, String> {


        @Override
        protected String doInBackground(String... params) {

            InputStream inputStream = null;

            HttpClient client = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(params[0]);
            httpGet.addHeader("Content-type", "application/json");

            String lyrics = "no lyrics found";


            try {


                HttpResponse response = client.execute(httpGet);
                inputStream = response.getEntity().getContent();

                // convert inputstream to string
                String tempJson = "";
                if (inputStream != null) {
                    tempJson = convertInputStreamToString(inputStream);
                    Log.i("App", "Data received:" + lyrics);

                }
                JSONObject jObject = new JSONObject(tempJson);

                lyrics = jObject.getString("lyrics");


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return lyrics;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mLyricsTV.setText(s);
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
                result += line;

            inputStream.close();
            return result;

        }


    }

    private class TrackProgressBar {

        private static final int LOOP_DURATION = 500;
        private final SeekBar mSeekBar;
        private final Handler mHandler;

        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mSpotifyAppRemote
                                .getPlayerApi()
                                .seekTo(seekBar.getProgress())
                                .setErrorCallback(mErrorCallback);
                    }
                };

        private final Runnable mSeekRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        int progress = mSeekBar.getProgress();
                        mSeekBar.setProgress(progress + LOOP_DURATION);
                        mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
                    }
                };

        private TrackProgressBar(SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
        }

        private void setDuration(long duration) {
            mSeekBar.setMax((int) duration);
        }

        private void update(long progress) {
            mSeekBar.setProgress((int) progress);
        }

        private void pause() {
            mHandler.removeCallbacks(mSeekRunnable);
        }

        private void unpause() {
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }
    }


}





