package com.rinno.simaski.spotifyapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    @BindView(R.id.tvEscuchas)
    TextView tvEscuchas;
    @BindView(R.id.ivAlbumCover)
    ImageView ivAlbumCover;
    @BindView(R.id.seekBar)
    SeekBar seekBar;
    @BindView(R.id.ivPlay)
    ImageView ivPlay;
    @BindView(R.id.ivPause)
    ImageView ivPause;
    @BindView(R.id.ivNext)
    ImageView ivNext;
    @BindView(R.id.ivPrevious)
    ImageView ivPrevious;
    private BluetoothAdapter bAdapter;

    private static final String CLIENT_ID = "a87d072fa5a04db299815d5bf258071a";
    private static final String REDIRECT_URI = "http://rinno.cl/callback/";
    AuthenticationRequest.Builder builder;
    AuthenticationRequest request;

    private Player mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private Metadata mMetadata;


    private static final int REQUEST_CODE = 1337;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final String TEST_SONG_URI = "spotify:track:6HFbq7cewJ7rPiffV0ciil";
    private static final String TEST_PLAYLIST_URI = "spotify:user:entel.manquehue:playlist:77OjbfH7qQKvjVcYkU3hAM";

    private int tiempo;


    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.e("TAG", "OK!");
        }

        @Override
        public void onError(Error error) {
            Log.e("TAG", "ERROR:" + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //seekBar.setProgress(0);
        //seekBar.setMax(1000);
        registrarEventosBluetooth();
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bAdapter.isEnabled()) {
            // Lanzamos el Intent que mostrara la interfaz de activacion del
            // Bluetooth. La respuesta de este Intent se manejara en el metodo
            // onActivityResult
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    onAuthenticationComplete(response);
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.e("TAG", "Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.e("TAG", "Auth result: " + response.getType());
            }
        }
    }

    private void onAuthenticationComplete(AuthenticationResponse authResponse) {
        if (mPlayer == null) {
            Config playerConfig = new Config(getApplicationContext(), authResponse.getAccessToken(), CLIENT_ID);

            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    //logStatus("-- Player initialized --");
                    //mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(DemoActivity.this));
                    mPlayer.addNotificationCallback(MainActivity.this);
                    mPlayer.addConnectionStateCallback(MainActivity.this);
                    // Trigger UI refresh
                    //updateView();
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("TAG", "Error in initialization: " + error.getMessage());
                }
            });
        } else {
            mPlayer.login(authResponse.getAccessToken());
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }


    @Override
    public void onPlaybackEvent(PlayerEvent event) {

        //Log.e("TAG", "Event: " + event);
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        //Log.i("TAG", "Player state: " + mCurrentPlaybackState);
        //Log.i("TAG", "Metadata: " + mMetadata);
        switch (event) {
            case kSpPlaybackNotifyMetadataChanged:
                updateView();
                break;
            default:
                break;
        }
    }

    private void updateView() {
        //final String durationStr = String.format(" (%dms)", mMetadata.currentTrack.durationMs);
        if (mMetadata != null) {
            findViewById(R.id.ivNext).setEnabled(mMetadata.nextTrack != null);
            findViewById(R.id.ivPrevious).setEnabled(mMetadata.prevTrack != null);
            findViewById(R.id.ivPause).setEnabled(mMetadata.currentTrack != null);
        }

        tiempo = (int) mMetadata.currentTrack.durationMs;

        tvEscuchas.setText(mMetadata.currentTrack.name + " - " + mMetadata.currentTrack.artistName);

        Glide.with(getApplicationContext()).load(mMetadata.currentTrack.albumCoverWebUrl)
                .thumbnail(0.5f)
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivAlbumCover);
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        //spotify:user:digsterchile:playlist:4i9ilOIkmFL4bn1Xag1G5n
        //spotify:track:58IL315gMSTD37DOZPJ2hf
        //mPlayer.playUri(null, "spotify:track:6HFbq7cewJ7rPiffV0ciil", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed");
    }


    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }


    // Instanciamos un BroadcastReceiver que se encargara de detectar si el estado
    // del Bluetooth del dispositivo ha cambiado mediante su handler onReceive
    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // Filtramos por la accion. Nos interesa detectar BluetoothAdapter.ACTION_STATE_CHANGED
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Solicitamos la informacion extra del intent etiquetada como BluetoothAdapter.EXTRA_STATE
                // El segundo parametro indicara el valor por defecto que se obtendra si el dato extra no existe
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (estado) {
                    // Apagado
                    case BluetoothAdapter.STATE_OFF: {
                        //((Button)findViewById(R.id.btnConectar)).setText(R.string.ActivarBluetooth);
                        break;
                    }

                    // Encendido
                    case BluetoothAdapter.STATE_ON: {
                        // Cambiamos el texto del boton
                        //((Button)findViewById(R.id.btnConectar)).setText(R.string.DesactivarBluetooth);

                        // Lanzamos un Intent de solicitud de visibilidad Bluetooth, al que añadimos un par
                        // clave-valor que indicara la duracion de este estado, en este caso 120 segundos
                        /*Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                        startActivity(discoverableIntent);*/


                        break;
                    }
                    default:
                        break;
                }

            }

            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
                switch (estado) {
                    case BluetoothAdapter.STATE_DISCONNECTED: {
                        mPlayer.pause(mOperationCallback);
                        //btnPause.setVisibility(View.GONE);
                        //btnResume.setVisibility(View.VISIBLE);
                        break;
                    }
                    case BluetoothAdapter.STATE_CONNECTED: {

                        break;
                    }
                    default:
                        break;
                }
            }

        }
    };

    private void registrarEventosBluetooth() {
        // Registramos el BroadcastReceiver que instanciamos previamente para
        // detectar los distintos eventos que queremos recibir
        IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(bReceiver, filtro);
        IntentFilter filtro2 = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        this.registerReceiver(bReceiver, filtro2);
    }


    /*@OnClick({R.id.btnPlay, R.id.btnPause, R.id.btnResume, R.id.btnNext, R.id.btnPrevious})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnPlay:

                break;
            case R.id.btnPause:

                break;
            case R.id.btnResume:
                mPlayer.resume(mOperationCallback);
                btnPause.setVisibility(View.VISIBLE);
                btnResume.setVisibility(View.GONE);
                break;
            case R.id.btnNext:
                mPlayer.skipToNext(mOperationCallback);
                break;
            case R.id.btnPrevious:
                mPlayer.skipToPrevious(mOperationCallback);
                break;
        }
    }*/

    @OnClick({R.id.ivPlay, R.id.ivPause, R.id.ivNext, R.id.ivPrevious})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivPlay:
                mPlayer.playUri(mOperationCallback, TEST_PLAYLIST_URI, 0, 0);
                ivPlay.setVisibility(View.GONE);
                ivPause.setVisibility(View.VISIBLE);
                Log.e("TIEMPO", "State : " + mCurrentPlaybackState);
                Log.e("TIEMPO", " : " + mCurrentPlaybackState);
                break;
            case R.id.ivPause:
                Log.e("TAG", "Player state: " + mCurrentPlaybackState);
                mPlayer.pause(mOperationCallback);
                ivPause.setVisibility(View.GONE);
                ivPlay.setVisibility(View.VISIBLE);
                break;
            case R.id.ivNext:
                mPlayer.skipToNext(mOperationCallback);
                break;
            case R.id.ivPrevious:
                mPlayer.skipToPrevious(mOperationCallback);
                break;
        }
    }
}