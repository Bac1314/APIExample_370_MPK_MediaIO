package io.agora.api.example.examples.advanced;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

//import io.agora.RtcChannelPublishHelper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import io.agora.api.component.Constant;
import io.agora.api.example.MainApplication;
import io.agora.api.example.R;
import io.agora.api.example.annotation.Example;
import io.agora.api.example.common.BaseFragment;
import io.agora.api.example.examples.advanced.customvideo.AgoraVideoSource;
import io.agora.api.example.utils.CommonUtil;
import io.agora.mediaplayer.AgoraMediaPlayerKit;
import io.agora.mediaplayer.AudioFrameObserver;
import io.agora.mediaplayer.Constants;
import io.agora.mediaplayer.MediaPlayerObserver;
import io.agora.mediaplayer.VideoFrameObserver;
import io.agora.mediaplayer.data.AudioFrame;
import io.agora.mediaplayer.data.VideoFrame;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.mediaio.AgoraDefaultSource;
import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.utils.*;

import static io.agora.api.example.common.model.Examples.ADVANCED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED;
import static io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_PLAYING;
import static io.agora.mediaplayer.Constants.PLAYER_RENDER_MODE_FIT;
import static io.agora.rtc.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x360;

@Example(
        index = 16,
        group = ADVANCED,
        name = R.string.item_mediaplayerkit,
        actionId = R.id.action_mainFragment_to_mediaplayerkit,
        tipsId = R.string.mediaplayerkit
)
public class MediaPlayerKit extends BaseFragment implements View.OnClickListener {

    private static final String TAG = MediaPlayerKit.class.getSimpleName();

    private Button join, open, play, stop, pause, publish, unpublish;
    private EditText et_channel, et_url;
    private RtcEngine engine;
    private int myUid;
    private FrameLayout fl_local, fl_remote;

    private AgoraMediaPlayerKit agoraMediaPlayerKit;
    private boolean joined = false;
    private SeekBar progressBar, volumeBar;
    private long playerDuration = 0;

    private static final String SAMPLE_MOVIE_URL = "https://bacsbucket.s3.ap-northeast-1.amazonaws.com/unsync_test.mp4";

    //
    private AgoraVideoSource mSource;

//    RtcChannelPublishHelper rtcChannelPublishHelper = RtcChannelPublishHelper.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_player_kit, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Check if the context is valid
        Context context = getContext();
        if (context == null) {
            return;
        }
        try {
            /**Creates an RtcEngine instance.
             * @param context The context of Android Activity
             * @param appId The App ID issued to you by Agora. See <a href="https://docs.agora.io/en/Agora%20Platform/token#get-an-app-id">
             *              How to get the App ID</a>
             * @param handler IRtcEngineEventHandler is an abstract class providing default implementation.
             *                The SDK uses this class to report to the app on SDK runtime events.*/
            engine = RtcEngine.create(context.getApplicationContext(), getString(R.string.agora_app_id), iRtcEngineEventHandler);
        } catch (Exception e) {
            e.printStackTrace();
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        join = view.findViewById(R.id.btn_join);
        open = view.findViewById(R.id.open);
        play = view.findViewById(R.id.play);
        stop = view.findViewById(R.id.stop);
        pause = view.findViewById(R.id.pause);
        publish = view.findViewById(R.id.publish);
        unpublish = view.findViewById(R.id.unpublish);
        progressBar = view.findViewById(R.id.ctrl_progress_bar);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });
        volumeBar = view.findViewById(R.id.ctrl_volume_bar);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                agoraMediaPlayerKit.adjustPlayoutVolume(i);
//                rtcChannelPublishHelper.adjustPublishSignalVolume(i,i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        et_channel = view.findViewById(R.id.et_channel);
        et_url = view.findViewById(R.id.link);
        et_url.setText(SAMPLE_MOVIE_URL);
        view.findViewById(R.id.btn_join).setOnClickListener(this);
        view.findViewById(R.id.open).setOnClickListener(this);
        view.findViewById(R.id.play).setOnClickListener(this);
        view.findViewById(R.id.stop).setOnClickListener(this);
        view.findViewById(R.id.pause).setOnClickListener(this);
        view.findViewById(R.id.publish).setOnClickListener(this);
        view.findViewById(R.id.unpublish).setOnClickListener(this);
        fl_local = view.findViewById(R.id.fl_local);
        fl_remote = view.findViewById(R.id.fl_remote);
        agoraMediaPlayerKit = new AgoraMediaPlayerKit(this.getActivity());
        agoraMediaPlayerKit.registerPlayerObserver(new MediaPlayerObserver() {
            @Override
            public void onPlayerStateChanged(Constants.MediaPlayerState state, Constants.MediaPlayerError error) {
                Log.e(TAG, String.format("onPlayerStateChanged State is " + state + " Error is " + error));
                if (state.equals(PLAYER_STATE_OPEN_COMPLETED)) {
                    play.setEnabled(true);
                    stop.setEnabled(true);
                    pause.setEnabled(true);
                    publish.setEnabled(true);
                    unpublish.setEnabled(true);
                }
            }


            @Override
            public void onPositionChanged(final long position) {
                if (playerDuration > 0) {
                    final int result = (int) ((float) position / (float) playerDuration * 100);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(Long.valueOf(result).intValue());
                        }
                    });
                }
            }


            @Override
            public void onMetaData(Constants.MediaPlayerMetadataType mediaPlayerMetadataType, byte[] bytes) {

            }

            @Override
            public void onPlayBufferUpdated(long l) {

            }

            @Override
            public void onPreloadEvent(String s, Constants.MediaPlayerPreloadEvent mediaPlayerPreloadEvent) {

            }

            @Override
            public void onPlayerEvent(Constants.MediaPlayerEvent eventCode) {
//                LogUtil.i("agoraMediaPlayerKit1 onEvent:" + eventCode);
            }

        });
        agoraMediaPlayerKit.registerVideoFrameObserver(new VideoFrameObserver() {
            @Override
            public void onFrame(VideoFrame videoFrame) {
//                Log.e(TAG, String.format("agoraMediaPlayerKit1 video onFrame :" + videoFrame));
                sendMPKFrame(videoFrame);
            }
        });
        agoraMediaPlayerKit.registerAudioFrameObserver(new AudioFrameObserver() {
            @Override
            public void onFrame(AudioFrame audioFrame) {
//                LogUtil.i("agoraMediaPlayerKit1 audio onFrame :" + audioFrame);
            }
        });
    }

    // BAC'S CODe //
    private void sendMPKFrame(VideoFrame videoFrame){
        if (mSource.getConsumer() == null) return;

        mSource.getConsumer().consumeByteBufferFrame(videoFrame.buffer, videoFrame.format, videoFrame.width, videoFrame.height, videoFrame.rotation, videoFrame.timestamp);
    }
    // END//

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_join) {
            if (!joined) {
                CommonUtil.hideInputBoard(getActivity(), et_channel);
                // call when join button hit
                String channelId = et_channel.getText().toString();
                // Check permission
                if (AndPermission.hasPermissions(this, Permission.Group.STORAGE, Permission.Group.MICROPHONE, Permission.Group.CAMERA)) {
                    joinChannel(channelId);
                    return;
                }
                // Request permission
                AndPermission.with(this).runtime().permission(
                        Permission.Group.STORAGE,
                        Permission.Group.MICROPHONE,
                        Permission.Group.CAMERA
                ).onGranted(permissions ->
                {
                    // Permissions Granted
                    joinChannel(channelId);
                }).start();
            } else {
                joined = false;

                engine.leaveChannel();
                join.setText(getString(R.string.join));
                agoraMediaPlayerKit.stop();
                agoraMediaPlayerKit.destroy();
                open.setEnabled(false);
                play.setEnabled(false);
                stop.setEnabled(false);
                pause.setEnabled(false);
                publish.setEnabled(false);
                unpublish.setEnabled(false);
            }
        } else if (v.getId() == R.id.open) {
//            engine.setVideoSource(null);
//            engine.setVideoSource(mSource);

            agoraMediaPlayerKit.open("/sdcard/Download/AudioVideoSyncTest.mp4", 0);
            progressBar.setVisibility(View.VISIBLE);
            volumeBar.setVisibility(View.VISIBLE);
            volumeBar.setProgress(100);

        } else if (v.getId() == R.id.play) {
            agoraMediaPlayerKit.play();
            playerDuration = agoraMediaPlayerKit.getDuration();
        } else if (v.getId() == R.id.stop) {
            agoraMediaPlayerKit.stop();
        } else if (v.getId() == R.id.pause) {
            agoraMediaPlayerKit.pause();
        } else if (v.getId() == R.id.publish) {
//            rtcChannelPublishHelper.publishAudio();
//            rtcChannelPublishHelper.publishVideo();
        } else if (v.getId() == R.id.unpublish) {
//            rtcChannelPublishHelper.unpublishAudio();
//            rtcChannelPublishHelper.unpublishVideo();
        }
    }

    private void joinChannel(String channelId) {
        // Check if the context is valid
        Context context = getContext();
        if (context == null) {
            return;
        }

        engine.setDefaultAudioRoutetoSpeakerphone(true);

        /** Sets the channel profile of the Agora RtcEngine.
         CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile.
         Use this profile in one-on-one calls or group calls, where all users can talk freely.
         CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast
         channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams;
         an audience can only receive streams.*/
        engine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        /**In the demo, the default is to enter as the anchor.*/
        engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
        // Enable video module
        engine.enableVideo();
        // Setup video encoding configs
        engine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                ((MainApplication)getActivity().getApplication()).getGlobalSettings().getVideoEncodingDimensionObject(),
                VideoEncoderConfiguration.FRAME_RATE.valueOf(((MainApplication)getActivity().getApplication()).getGlobalSettings().getVideoEncodingFrameRate()),
                STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.valueOf(((MainApplication)getActivity().getApplication()).getGlobalSettings().getVideoEncodingOrientation())
        ));

        //BAC'S CODE
        mSource = new AgoraVideoSource();
        engine.setVideoSource(mSource);

        //

        SurfaceView surfaceView = new SurfaceView(this.getActivity());
        surfaceView.setZOrderMediaOverlay(false);
        if (fl_local.getChildCount() > 0) {
            fl_local.removeAllViews();
        }
        fl_local.addView(surfaceView);

        // attach player to agora rtc kit, so that the media stream can be published
//        rtcChannelPublishHelper.attachPlayerToRtc(agoraMediaPlayerKit, engine);

        // set media local play view
        agoraMediaPlayerKit.setView(surfaceView);
        agoraMediaPlayerKit.setRenderMode(PLAYER_RENDER_MODE_FIT);

        /**Please configure accessToken in the string_config file.
         * A temporary token generated in Console. A temporary token is valid for 24 hours. For details, see
         *      https://docs.agora.io/en/Agora%20Platform/token?platform=All%20Platforms#get-a-temporary-token
         * A token generated at the server. This applies to scenarios with high-security requirements. For details, see
         *      https://docs.agora.io/en/cloud-recording/token_server_java?platform=Java*/
        String accessToken = getString(R.string.agora_access_token);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "<#YOUR ACCESS TOKEN#>")) {
            accessToken = null;
        }
        /** Allows a user to join a channel.
         if you do not specify the uid, we will generate the uid for you*/

        ChannelMediaOptions option = new ChannelMediaOptions();
        option.autoSubscribeAudio = true;
        option.autoSubscribeVideo = true;
        int res = engine.joinChannel(accessToken, channelId, "Extra Optional Data", 0, option);
        if (res != 0) {
            // Usually happens with invalid parameters
            // Error code description can be found at:
            // en: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            // cn: https://docs.agora.io/cn/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            showAlert(RtcEngine.getErrorDescription(Math.abs(res)));
            return;
        }
        // Prevent repeated entry
        join.setEnabled(false);
    }

    /**
     * IRtcEngineEventHandler is an abstract class providing default implementation.
     * The SDK uses this class to report to the app on SDK runtime events.
     */
    private final IRtcEngineEventHandler iRtcEngineEventHandler = new IRtcEngineEventHandler() {
        /**Reports a warning during SDK runtime.
         * Warning code: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_warn_code.html*/
        @Override
        public void onWarning(int warn) {
            Log.w(TAG, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
        }

        /**Reports an error during SDK runtime.
         * Error code: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html*/
        @Override
        public void onError(int err) {
            Log.e(TAG, String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
            showAlert(String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
        }

        /**Occurs when a user leaves the channel.
         * @param stats With this callback, the application retrieves the channel information,
         *              such as the call duration and statistics.*/
        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            Log.i(TAG, String.format("local user %d leaveChannel!", myUid));
            showLongToast(String.format("local user %d leaveChannel!", myUid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.i(TAG, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            showLongToast(String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            myUid = uid;
            joined = true;
            handler.post(() -> {
                join.setEnabled(true);
                join.setText(getString(R.string.leave));
                open.setEnabled(true);
            });
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            Log.i(TAG, "onRemoteAudioStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            Log.i(TAG, "onRemoteVideoStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.i(TAG, "onUserJoined->" + uid);
            showLongToast(String.format("user %d joined!", uid));
            /**Check if the context is correct*/
            Context context = getContext();
            if (context == null) {
                return;
            }
            handler.post(() ->
            {
                /**Display remote video stream*/
                SurfaceView surfaceView = null;
                if (fl_remote.getChildCount() > 0) {
                    fl_remote.removeAllViews();
                }
                // Create render view by RtcEngine
                surfaceView = RtcEngine.CreateRendererView(context);
                surfaceView.setZOrderMediaOverlay(true);
                // Add to the remote container
                fl_remote.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                // Setup remote video to render
                engine.setupRemoteVideo(new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, uid));
            });
        }

        /**Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         * @param uid ID of the user whose audio state changes.
         * @param reason Reason why the user goes offline:
         *   USER_OFFLINE_QUIT(0): The user left the current channel.
         *   USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data
         *              packet was received within a certain period of time. If a user quits the
         *               call and the message is not passed to the SDK (due to an unreliable channel),
         *               the SDK assumes the user dropped offline.
         *   USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from
         *               the host to the audience.*/
        @Override
        public void onUserOffline(int uid, int reason) {
            Log.i(TAG, String.format("user %d offline! reason:%d", uid, reason));
            showLongToast(String.format("user %d offline! reason:%d", uid, reason));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    /**Clear render view
                     Note: The video will stay at its last frame, to completely remove it you will need to
                     remove the SurfaceView from its parent*/
                    engine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_HIDDEN, uid));
                }
            });
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (engine != null) {
            /**leaveChannel and Destroy the RtcEngine instance*/
            if (joined) {
                engine.leaveChannel();
            }
            agoraMediaPlayerKit.destroy();
            handler.post(RtcEngine::destroy);
            engine = null;
        }
    }

}

