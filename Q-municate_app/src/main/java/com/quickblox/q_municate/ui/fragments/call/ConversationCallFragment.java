package com.quickblox.q_municate.ui.fragments.call;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.Order;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.q_municate.R;
import com.quickblox.q_municate.ui.activities.call.CallActivity;
import com.quickblox.q_municate.utils.ToastUtils;
import com.quickblox.q_municate.utils.image.ImageLoaderUtils;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.models.StartConversationReason;
import com.quickblox.q_municate_core.qb.commands.push.QBSendPushCommand;
import com.quickblox.q_municate_core.qb.helpers.QBFriendListHelper;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate.utils.helpers.SystemPermissionHelper;
import com.quickblox.q_municate_user_service.model.QMUser;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.AppRTCAudioManager;
import com.quickblox.videochat.webrtc.BaseSession;
import com.quickblox.videochat.webrtc.QBMediaStreamManager;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.view.QBRTCSurfaceView;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglRenderer;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * QuickBlox team
 */
public class ConversationCallFragment extends Fragment implements Serializable, QBRTCClientVideoTracksCallbacks<QBRTCSession>,
        QBRTCSessionConnectionCallbacks, CallActivity.QBRTCSessionUserCallback {

    private static final long TOGGLE_CAMERA_DELAY = 1000;
    private static final long LOCAL_TRACk_INITIALIZE_DELAY = 500;

    private String TAG = ConversationCallFragment.class.getSimpleName();
    private ArrayList<QBUser> opponents;
    private QBRTCTypes.QBConferenceType qbConferenceType;
    private StartConversationReason startConversationReason;

    private ToggleButton cameraToggle;
    private ToggleButton micToggleVideoCall;
    private ImageButton handUpVideoCall;
    private boolean isVideoCall = false;
    private boolean isAudioEnabled = true;
    private List<QBUser> allUsers = new ArrayList<>();
    private String callerName;
    private boolean isMessageProcessed;
    private QBRTCSurfaceView remoteVideoView;
    private QBRTCSurfaceView localVideoView;
    private CameraState cameraState = CameraState.NONE;
    private boolean isPeerToPeerCall;
    private QBRTCVideoTrack localVideoTrack;
    private QBRTCVideoTrack remoteVideoTrack;
    private Handler mainHandler;
    private ImageView avatarImageview;
    private TextView callingToTextView;
    private FrameLayout avatarAndNameView;
    private boolean isFullScreen;
    private View elementSetVideoButtons;
    private boolean isFrontCameraSelected = true;
    private AppRTCAudioManager audioManager;
    private SystemPermissionHelper systemPermissionHelper;
    private boolean isAllViewsInitialized = true;

    //
    int screenHeight, screenWidth;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;
    ImageReader mImageReader;
    private Timer timer;
    private TimerTask timerTask;
    public static final String EMOTION_API_KEY = "2871e8188acc40df98759c07a876d426";
    public static final String FACE_API_KEY = "fa0142197b854f09a89525915c5ab7a1";
    Bitmap mBitmap;
    private EmotionServiceClient client;
    ImageView emotionImage;
    RelativeLayout layout;
    ArrayList<ImageView> imageViews;



    public static ConversationCallFragment newInstance(List<QBUser> opponents, String callerName,
            QBRTCTypes.QBConferenceType qbConferenceType,
            StartConversationReason reason,
            String sessionId) {

        ConversationCallFragment fragment = new ConversationCallFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(QBServiceConsts.EXTRA_CONFERENCE_TYPE, qbConferenceType);
        bundle.putString(QBServiceConsts.EXTRA_CALLER_NAME, callerName);
        bundle.putSerializable(QBServiceConsts.EXTRA_OPPONENTS, (Serializable) opponents);

        bundle.putSerializable(QBServiceConsts.EXTRA_START_CONVERSATION_REASON_TYPE, reason);
        if (sessionId != null) {
            bundle.putString(QBServiceConsts.EXTRA_SESSION_ID, sessionId);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                Toast.makeText(getActivity(), "User cancelled", Toast.LENGTH_SHORT).show();
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            Log.i(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        tearDownMediaProjection();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void takeScreenShot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
        try {
            Image image = mImageReader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * screenWidth;
                int bitmapWidth = screenWidth + rowPadding / pixelStride;

                if (mBitmap != null) {
                    mBitmap.recycle();
                }

                mBitmap = Bitmap.createBitmap(bitmapWidth,
                        screenHeight, Bitmap.Config.ARGB_8888);

                mBitmap.copyPixelsFromBuffer(buffer);

                image.close();
            }
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }

        try {
            new doRequest(false).execute();
            Log.d(TAG, "onPostExecute: start regconize");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startScreenCapture() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mMediaProjection != null) {
            setUpVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
        } else {
            Log.i(TAG, "Requesting confirmation");
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUpVirtualDisplay() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size=new Point();
        display.getSize(size);

        screenWidth = size.x;
        screenHeight = size.y;
        mImageReader = ImageReader.newInstance(screenWidth,screenHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror", screenWidth, screenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);

        takeScreenShot();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation, container, false);
        Log.d(TAG, "Fragment. Thread id: " + Thread.currentThread().getId());

        if (getArguments() != null) {
            opponents = (ArrayList<QBUser>) getArguments().getSerializable(QBServiceConsts.EXTRA_OPPONENTS);
            qbConferenceType = (QBRTCTypes.QBConferenceType) getArguments().getSerializable(QBServiceConsts.EXTRA_CONFERENCE_TYPE);
            startConversationReason = (StartConversationReason) getArguments().getSerializable(QBServiceConsts.EXTRA_START_CONVERSATION_REASON_TYPE);
            callerName = getArguments().getString(QBServiceConsts.EXTRA_CALLER_NAME);

            isPeerToPeerCall = opponents.size() == 1;
            isVideoCall = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO.equals(qbConferenceType);
            Log.d(TAG, "CALLER_NAME: " + callerName);
            Log.d(TAG, "opponents: " + opponents.toString());
        }

        systemPermissionHelper = new SystemPermissionHelper(getActivity());

        initViews(view);
        ((CallActivity)getActivity()).initActionBar();
        ((CallActivity)getActivity()).setCallActionBarTitle(StartConversationReason.INCOME_CALL_FOR_ACCEPTION
                .equals(startConversationReason) ? callerName : opponents.get(0).getFullName());
        initButtonsListener();
        initSessionListener();
        setUpUiByCallType();

        displayOpponentAvatar();
        initAudioManager();
        mainHandler = new FragmentLifeCycleHandler();

        return view;

    }

    private void initSessionListener() {
        ((CallActivity) getActivity()).addVideoTrackCallbacksListener(this);
    }

    private void setUpUiByCallType() {
        if (!isVideoCall) {
            remoteVideoView.setVisibility(View.INVISIBLE);
            cameraToggle.setVisibility(View.GONE);
            cameraToggle.setEnabled(false);
            cameraToggle.setChecked(false);
        }

        correctButtonsVisibilityByGrantedPermissions();
    }

    private void correctButtonsVisibilityByGrantedPermissions (){
        if (!systemPermissionHelper.isAllPermissionsGrantedForCallByType(qbConferenceType)){
            if(!systemPermissionHelper.isMicrophonePermissionGranted()){
                micToggleVideoCall.setChecked(false);
                micToggleVideoCall.setEnabled(false);
            }

            if(!systemPermissionHelper.isCameraPermissionGranted()){
                cameraToggle.setChecked(false);
                cameraToggle.setEnabled(false);
            }
        }
    }

    private void displayOpponentAvatar() {
        QMUser opponent = ((CallActivity) getActivity()).getOpponentAsUserFromDB(opponents.get(0).getId());
        if (StartConversationReason.INCOME_CALL_FOR_ACCEPTION.equals(startConversationReason) && !isVideoCall){
            avatarAndNameView.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage(opponent.getAvatar(), avatarImageview, ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS);
        } else if (StartConversationReason.OUTCOME_CALL_MADE.equals(startConversationReason)) {
            avatarAndNameView.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage(opponent.getAvatar(), avatarImageview, ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS);
            callingToTextView.setText(getString(R.string.calling_to, opponent.getFullName()));
        }
    }


    private void actionsByConnectedToUser(){
        actionButtonsEnabled(true);
        ((CallActivity) getActivity()).startTimer();
        getActivity().invalidateOptionsMenu();
        hideAvatarIfNeed();
    }

    private void hideAvatarIfNeed() {
        callingToTextView.setVisibility(View.INVISIBLE);

        if (isVideoCall) {
            avatarAndNameView.setVisibility(View.GONE);
        }
    }

    public void actionButtonsEnabled(boolean enability) {
        if (!isAllViewsInitialized){
            return;
        }

        if (isVideoCall){
            cameraToggle.setEnabled(enability);
        }
        micToggleVideoCall.setEnabled(enability);

        // inactivate toggle buttons
        if (isVideoCall) {
            cameraToggle.setActivated(enability);
        }
        micToggleVideoCall.setActivated(enability);

        correctButtonsVisibilityByGrantedPermissions();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onStart() {
        super.onStart();
        QBRTCSession session = ((CallActivity) getActivity()).getCurrentSession();
        if (!isMessageProcessed) {
            if (startConversationReason == StartConversationReason.INCOME_CALL_FOR_ACCEPTION) {
                session.acceptCall(session.getUserInfo());
            } else {
                sendPushAboutCall();
                session.startCall(session.getUserInfo());
            }
            isMessageProcessed = true;
        }
        ((CallActivity) getActivity()).addTCClientConnectionCallback(this);
        ((CallActivity) getActivity()).addRTCSessionUserCallback(this);
    }

    private void sendPushAboutCall() {
        if (isPeerToPeerCall) {
            QBFriendListHelper qbFriendListHelper = new QBFriendListHelper(getActivity());
            QBUser qbUser = opponents.get(0);

            if (!qbFriendListHelper.isUserOnline(qbUser.getId())) {
                String callMsg = getString(R.string.dlg_offline_call,
                        AppSession.getSession().getUser().getFullName());
                QBSendPushCommand.start(getActivity(), callMsg, qbUser.getId());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() from " + TAG);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }

        client = new EmotionServiceRestClient(EMOTION_API_KEY);
        setHasOptionsMenu(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mMediaProjectionManager = (MediaProjectionManager)
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void initViews(View view) {
        remoteVideoView = (QBRTCSurfaceView) view.findViewById(R.id.remote_video_view);

        localVideoView = (QBRTCSurfaceView) view.findViewById(R.id.local_video_view);
        localVideoView.setZOrderMediaOverlay(true);

        cameraToggle = (ToggleButton) view.findViewById(R.id.cameraToggle);

        micToggleVideoCall = (ToggleButton) view.findViewById(R.id.micToggleVideoCall);

        handUpVideoCall = (ImageButton) view.findViewById(R.id.handUpVideoCall);

        avatarAndNameView = (FrameLayout) view.findViewById(R.id.avatar_and_name);

        avatarAndNameView.addView(getActivity().getLayoutInflater().inflate(isVideoCall ?
                        R.layout.view_avatar_and_name_horizontal : R.layout.view_avatar_and_name_vertical,
                avatarAndNameView, false));

        elementSetVideoButtons = view.findViewById(R.id.element_set_video_buttons);

        avatarImageview = (ImageView) avatarAndNameView.findViewById(R.id.avatar_imageview);

        callingToTextView = (TextView) avatarAndNameView.findViewById(R.id.calling_to_text_view);

        isAllViewsInitialized = true;

        emotionImage = view.findViewById(R.id.emotion_image);
        emotionImage.setVisibility(View.INVISIBLE);

        layout = view.findViewById(R.id.fragmentOpponents);

        actionButtonsEnabled(false);
        imageViews = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();

        // If user changed camera state few times and last state was CameraState.ENABLED_FROM_USER
        // than we turn on cam, else we nothing change
        if (cameraState != CameraState.DISABLED_FROM_USER
                && isVideoCall) {
            toggleCamera(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPause() {
        // If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
        // than we turn off cam
        if (cameraState != CameraState.DISABLED_FROM_USER && isVideoCall) {
            toggleCamera(false);
        }

        stopScreenCapture();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((CallActivity) getActivity()).removeRTCClientConnectionCallback(this);
        ((CallActivity) getActivity()).removeRTCSessionUserCallback();
    }

    private void initButtonsListener() {
        cameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cameraState = isChecked ? CameraState.ENABLED_FROM_USER : CameraState.DISABLED_FROM_USER;
                toggleCamera(isChecked);
            }
        });

        micToggleVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CallActivity) getActivity()).getCurrentSession() != null) {
                    if (isAudioEnabled) {
                        Log.d(TAG, "Mic is off!");
                        ((CallActivity) getActivity()).getCurrentSession().getMediaStreamManager().setAudioEnabled(false);
                        isAudioEnabled = false;
                    } else {
                        Log.d(TAG, "Mic is on!");
                        ((CallActivity) getActivity()).getCurrentSession().getMediaStreamManager().setAudioEnabled(true);
                        isAudioEnabled = true;
                    }
                }
            }
        });

        handUpVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtonsEnabled(false);
                handUpVideoCall.setEnabled(false);
                Log.d(TAG, "Call is stopped");

                ((CallActivity) getActivity()).hangUpCurrentSession();
                handUpVideoCall.setEnabled(false);
                handUpVideoCall.setActivated(false);

            }
        });

        remoteVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullScreen();
            }
        });
    }

    void toggleFullScreen(){
        if(isFullScreen){
            setLocalVideoViewVisible(false);
            showSystemUI();
            ((CallActivity)getActivity()).showCallActionBar();
            elementSetVideoButtons.setVisibility(View.VISIBLE);
            isFullScreen = false;
        } else {
            setLocalVideoViewVisible(true);
            hideSystemUI();
            ((CallActivity) getActivity()).hideCallActionBar();
            elementSetVideoButtons.setVisibility(View.INVISIBLE);
            isFullScreen = true;
        }
    }

    private void setLocalVideoViewVisible(boolean visible){
        if (remoteVideoView != null && localVideoTrack != null){
            if (visible) {
                if (localVideoTrack.getRenderer() == null){
                    fillVideoView(localVideoView, localVideoTrack, true);
                }

                localVideoView.setVisibility(View.VISIBLE);
                Log.d(TAG, "fullscreen enabled");
            } else {
                localVideoTrack.cleanUp();
                localVideoView.setVisibility(View.INVISIBLE);
                Log.d(TAG, "fullscreen disabled");
            }
        }
    }

    private void hideSystemUI() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    private void showSystemUI() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        QBRTCSession currentSession = ((CallActivity) getActivity()).getCurrentSession();
        if (currentSession == null) {
            return super.onOptionsItemSelected(item);
        }

        final QBMediaStreamManager mediaStreamManager = currentSession.getMediaStreamManager();
        if (mediaStreamManager == null) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.switch_camera_toggle:
                mediaStreamManager.switchCameraInput(new CameraVideoCapturer.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(boolean b) {
                        isFrontCameraSelected = b;
                        toggleCameraInternal();
                    }
                    @Override
                    public void onCameraSwitchError(String s) {

                    }
                });
                break;
            case R.id.switch_speaker_toggle:
                toggleAudioOutput();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        getActivity().invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    private void toggleAudioOutput() {
        if (!audioManager.getSelectedAudioDevice().equals(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)) {
            audioManager.setAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        } else {
            if(audioManager.getAudioDevices().contains(AppRTCAudioManager.AudioDevice.WIRED_HEADSET)){
                audioManager.setAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET);
            } else {
                audioManager.setAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
            }
        }
    }

    private void initAudioManager() {
        audioManager = AppRTCAudioManager.create(getActivity(), new AppRTCAudioManager.OnAudioManagerStateListener() {
            @Override
            public void onAudioChangedState(AppRTCAudioManager.AudioDevice audioDevice) {
                ToastUtils.shortToast("Audio device switched to  " + audioDevice);
            }
        });
        audioManager.init();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSpeakerToggle = menu.findItem(R.id.switch_speaker_toggle);
        if (itemSpeakerToggle != null){
            updateSpeakerToggleIcon(itemSpeakerToggle);
        }

        MenuItem itemCameraToggle = menu.findItem(R.id.switch_camera_toggle);
        if (itemCameraToggle != null){
            updateCameraToggleIcon(itemCameraToggle);
        }
    }

    private void updateSpeakerToggleIcon(MenuItem itemSpeakerToggle) {
        boolean speakerEnabled = audioManager.getSelectedAudioDevice().equals(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        itemSpeakerToggle.setIcon(speakerEnabled ? R.drawable.ic_phonelink_ring : R.drawable.ic_speaker_phone);
    }

    private void updateCameraToggleIcon(MenuItem itemCameraToggle) {
        if (((CallActivity) getActivity()).getCurrentSession() != null) {
            if (isVideoCall) {
                if (isVideoEnabled()) {
                    itemCameraToggle.setIcon(isFrontCameraSelected ? R.drawable.ic_camera_front_white : R.drawable.ic_camera_rear_white);
                }

                itemCameraToggle.setVisible(isVideoEnabled());
                itemCameraToggle.setEnabled(isVideoEnabled());
            } else {
                itemCameraToggle.setEnabled(false);
                itemCameraToggle.setVisible(false);
            }
        }
    }

    private boolean isVideoEnabled(){
        QBRTCSession currentSession = ((CallActivity) getActivity()).getCurrentSession();
        QBMediaStreamManager mediaStreamManager;
        if (currentSession != null) {
            mediaStreamManager = currentSession.getMediaStreamManager();
        } else {
            return false;
        }

        if (mediaStreamManager != null){
            return mediaStreamManager.isVideoEnabled();
        } else {
            return false;
        }
    }

    private void toggleCameraInternal(){
        getActivity().invalidateOptionsMenu();
        updateVideoView(remoteVideoTrack == null ? remoteVideoView : localVideoView, isFrontCameraSelected);
    }

    private void toggleCamera(boolean isNeedEnableCam) {

        QBRTCSession currentSession = ((CallActivity) getActivity()).getCurrentSession();
        if (currentSession != null && currentSession.getMediaStreamManager() != null){
            currentSession.getMediaStreamManager().setVideoEnabled(isNeedEnableCam);
            getActivity().invalidateOptionsMenu();
        }
    }


    @Override
    public void onLocalVideoTrackReceive(QBRTCSession qbrtcSession, final QBRTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive() run");
        localVideoTrack = videoTrack;

        if (remoteVideoView != null && remoteVideoTrack == null) {
            fillVideoView(remoteVideoView, videoTrack, true);
        }
        if(getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRemoteVideoTrackReceive(QBRTCSession session, QBRTCVideoTrack videoTrack, Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);
        remoteVideoTrack = videoTrack;

        if (remoteVideoView != null) {
            if (localVideoTrack != null){
                localVideoTrack.cleanUp();
            }
            fillVideoView(remoteVideoView, videoTrack, false);
            updateVideoView(remoteVideoView, false);
        }
        startScreenCapture(); // TODO
    }

    private void fillVideoView(QBRTCSurfaceView videoView, QBRTCVideoTrack videoTrack, boolean localRenderer) {
        videoTrack.cleanUp();
        videoTrack.addRenderer(new VideoRenderer(videoView));

        if (localRenderer) {
            updateVideoView(videoView, isFrontCameraSelected);
        }
        Log.d(TAG, (localRenderer ? "local" : "remote") + " Track is rendering");
    }

    protected void updateVideoView(QBRTCSurfaceView surfaceViewRenderer, boolean mirror) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }

    protected void updateVideoView(QBRTCSurfaceView surfaceViewRenderer, boolean mirror, RendererCommon.ScalingType scalingType) {
        Log.i(TAG, "updateVideoView mirror:" + mirror + ", scalintType = " + scalingType);
        surfaceViewRenderer.setScalingType(scalingType);
        surfaceViewRenderer.setMirror(mirror);
        surfaceViewRenderer.requestLayout();
    }

    @Override
    public void onStartConnectToUser(QBRTCSession qbrtcSession, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.checking));
    }

    @Override
    public void onStateChanged(QBRTCSession qbrtcSession, BaseSession.QBRTCSessionState qbrtcSessionState) {

    }

    @Override
    public void onConnectedToUser(QBRTCSession qbrtcSession,final Integer userId) {
        actionsByConnectedToUser();
    }

    @Override
    public void onConnectionClosedForUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.closed));
    }

    @Override
    public void onDisconnectedFromUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.disconnected));
    }

    @Override
    public void onDisconnectedTimeoutFromUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.time_out));
    }

    @Override
    public void onConnectionFailedWithUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.failed));
    }

    @Override
    public void onUserNotAnswer(QBRTCSession session, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.call_no_answer));
    }

    @Override
    public void onCallRejectByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
//        setStatusForOpponent(userId, getString(R.string.call_rejected));
    }

    @Override
    public void onCallAcceptByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
//        setStatusForOpponent(userId, getString(R.string.call_accepted));
    }

    @Override
    public void onReceiveHangUpFromUser(QBRTCSession session, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.call_hung_up));
    }


    private enum CameraState {
        NONE,
        DISABLED_FROM_USER,
        ENABLED_FROM_USER
    }

    class FragmentLifeCycleHandler extends Handler{

        @Override
        public void dispatchMessage(Message msg) {
            if (isAdded() && getActivity() != null) {
                super.dispatchMessage(msg);
            } else {
                Log.d(TAG, "Fragment under destroying");
            }
        }
    }

    class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        public DividerItemDecoration(@NonNull Context context, @DimenRes int dimensionDivider) {
            this.space = context.getResources().getDimensionPixelSize(dimensionDivider);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(space, space, space, space);
        }
    }

    // Emotion Detect

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = FACE_API_KEY;
        FaceServiceRestClient faceClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0" ,faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.client.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            for (ImageView imageView : imageViews) {
                layout.removeViewInLayout(imageView);
            }
            imageViews.clear();
            if (result != null && !result.isEmpty()) {
                for (RecognizeResult recognizeResult : result) {
                    List<Map.Entry<String, Double>> rankedList;
                    rankedList = recognizeResult.scores.ToRankedList(Order.DESCENDING);
                    Log.d(TAG, "onPostExecute: Reconize OK");
                    if (getContext() != null) {
                        Log.d(TAG, rankedList.get(0).getKey() + Double.toString(rankedList.get(0).getValue()));
                        ImageView emotionImage = new ImageView(getContext());
                        emotionImage.setZ(1);
                        emotionImage.setVisibility(View.VISIBLE);
                        emotionImage.setX(recognizeResult.faceRectangle.left - 70);
                        emotionImage.setY(recognizeResult.faceRectangle.top - 70);
                        imageViews.add(emotionImage);
                        layout.addView(emotionImage);

                        String emotion = rankedList.get(0).getKey();
                        Double emotionValue = rankedList.get(0).getValue();

                        if (emotion.equalsIgnoreCase("NEUTRAL") && emotionValue <= 0.7) {
                            emotion = rankedList.get(1).getKey();
                        }

                        switch (emotion) {
                            case "ANGER" :
                                emotionImage.setImageResource(R.drawable.angry);
                                break;
                            case "CONTEMPT" :
                                emotionImage.setImageResource(R.drawable.smirking);
                                break;
                            case "DISGUST" :
                                emotionImage.setImageResource(R.drawable.disgusted);
                                break;
                            case "FEAR" :
                                emotionImage.setImageResource(R.drawable.scared);
                                break;
                            case "HAPPINESS" :
                                emotionImage.setImageResource(R.drawable.happy);
                                break;
                            case "SADNESS" :
                                emotionImage.setImageResource(R.drawable.crying);
                                break;
                            case "SURPRISE" :
                                emotionImage.setImageResource(R.drawable.surprised);
                                break;
                            default:
                                emotionImage.setImageResource(R.drawable.neutral);
                                break;
                        }
                    }
                }
            }
            else {
                if (getContext() != null) {
                    for (ImageView imageView : imageViews) {
                        layout.removeViewInLayout(imageView);
                        imageViews.clear();
                    }
                }
            }

            takeScreenShot();
        }
    }
}