/*
 * Copyright (c) 2013-2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.codeaurora.bluetooth.bttestapp;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothAvrcpRemoteMediaPlayers;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnTouchListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Process;
import android.widget.TextView;
import android.app.Activity;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import java.util.concurrent.TimeUnit;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.codeaurora.bluetooth.bttestapp.R;
import org.codeaurora.bluetooth.bttestapp.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.MediaDescription;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;
import java.util.HashMap;
import java.util.Stack;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.Arrays;
import java.nio.ByteBuffer;
import android.content.res.Resources;

public class AvrcpTestActivity extends MonkeyActivity implements IBluetoothConnectionObserver {

    private final String TAG = "AvrcpTestActivity";
    private boolean ffPressed = false;
    public Context mContext =  null;

    private ActionBar mActionBar = null;
    private PressandHoldHandler mPressandHoldHandler;
    private View appView;
    private Activity mLocalActivity;
    private BluetoothAvrcpFolderAdapter mBluetoothAvrcpFolderAdapter = null;
    private BluetoothAvrcpPlayerAdapter mBluetoothAvrcpPlayerAdapter = null;
    private final ReentrantLock mLock = new ReentrantLock();
    BluetoothAvrcpController mAvrcpController;
    private ListView folderListView;
    private ListView playerListView;
    ProfileService mProfileService = null;
    BluetoothDevice mDevice;
    private class PlayerSettings
    {
        public byte attr_Id;
        public byte attr_val;
        public byte [] supported_values; // check  values before Setting Player Attributes.
    };

    public static final int SEND_PASS_THROUGH_CMD = 1;
    public static final int FETCH_CURRENT_INFO = 2;
    public static final int TEST_SETBROWSED_PLAYER = 3;
    public static final int TEST_CHANGE_PATH = 4;
    public static final int TEST_SETADDRESSED_PLAYER = 5;
    public static final int TEST_FETCH_NPL = 6;
    public static final int TEST_BROWSE_ROOT = 7;
    public static final int TEST_SEARCH = 8;
    public static final int TEST_ADD_TO_NPL = 9;
    public static final int TEST_PLAY_ITEM = 10;
    public static final int REFRESH_CURRENT_FOLDER = 11;
    public static final int UPDATE_NPL_THUMBNAIL = 12;
    public static final int UPDATE_SEARCH_THUMBNAIL = 13;
    public static final int UPDATE_VFS_THUMBNAIL = 14;
    public static final int PTS_GET_ELEMENT_ATTRIBUTE_ID = 0x71;
    public static final int PTS_GET_PLAY_STATUS_ID       = 0x72;
    public static final int PTS_GET_VFS_ATTR_ID    = 0x73;
    public static final int PTS_GET_ITEM_VFS_ID    = 0x51;
    public static final int MAX_SUPPORT_LIST_ENTRY = 200;

    /* connection state with MediaBrowseService implemented by BT-AVRCP app */
    private static final int DISCONNECTED = 0;
    private static final int CONNECTED = 1;
    private static final int SUSPENDED = 2;

    /* Object used to connect to MediaBrowseService of BT-AVRCP app */
    private MediaBrowser mMediaBrowser = null;
    private MediaController mMediaController = null;

    private BluetoothAvrcpRemoteMediaPlayers MediaPlayerList = null;

    /* media players Ids which is set as Current Browsed player*/
    private int mCurrBrowsePlayerID = 0;
    private int mCurrAddrPlayerID = 0;

    /* The mediaId to be used for subscribing for children using the MediaBrowser */
    private String mMediaId = null;
    private String mRootFolderUid = null;
    private int mConnState = DISCONNECTED;

    /* Number of items in current folder */
    private int mCurrFolderNumItems = 0;

    /* store result of getfolderitems with scope="vfs" */
    private List<MediaBrowser.MediaItem> mFolderItems = null;

    /* store result of getfolderitems with scope="nowplaying" */
    private List<MediaSession.QueueItem> mNowPlayingItems = null;

    /* store result of getfolderitems with scope="Search" */
    private List<MediaSession.QueueItem> mSearchItems = null;

    /* notify only if remote registers for notification */
    private Boolean mNotifyUidChange = false;

    /* stores the path trail during changePath */
    private Stack<String> mPathStack = null;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<DisplayItem> itemDisplayList =  null;

    /* handler and looper for mediacontroller callback */
    private HandlerThread mThread;
    private Looper mLooper;
    private Handler mHandler;

    private static final int AVRCP_SCOPE_MEDIA_PLAYERLIST = 0;
    private static final int AVRCP_SCOPE_VFS = 1;
    private static final int AVRCP_SCOPE_SEARCH = 2;
    private static final int AVRCP_SCOPE_NOW_PLAYING = 3;
    private static final int AVRCP_SCOPE_NONE = 4;
    private static final int PLAYER_FEATURE_MASK_SIZE = 16;
    private int mCurrentScope = AVRCP_SCOPE_NONE;
    private static final int FETCH_DONE = 0;
    private static final int FETCH_VFS = 1;
    private static final int FETCH_NPL = 2;
    private static final int FETCH_SEARCH = 3;
    private int pendingFetchCmd = FETCH_DONE;

    private TextView mRepeatStatus;
    private TextView mShuffleStatus;
    private TextView mGenreStatus;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mPlayTime;
    private TextView mScanStatus;
    private TextView mPlayStatus;
    private TextView mEqualizerStatus;
    private TextView mTrackNumber;
    private TextView mTitleName;
    private Button ffButton;
    private Button rwButton;
    private ImageView mCoverArtImageView;
    private ListView mFolderList;

    private String repeatText;
    private String shuffleText;
    private String genreText;
    private String artistText;
    private String albumText;
    private String playText;
    private String scanText;
    private String playStatusText;
    private String equalizerText;
    private String trackNumText;
    private String titleNameText;
    private long   trackLen;
    private Bitmap coverArtBitmap;
    private Button search;
    private Button addtonp;
    private Button playitem;
    private EditText mEdit;

    public class DisplayItem {
        public String textFeild;
        public Bitmap imageFeild;
        public DisplayItem() {
            textFeild = "empty";
            imageFeild = null; //BitmapFactory.decodeResource(res, R.drawable.ic_bt_connected);
        }
        void addTextFeild(String text) {
            textFeild = text;
        }
        void addImageFeild(Bitmap img) {
            imageFeild = img;
        }
    }

    private final BroadcastReceiver mAvrcpControllerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED)) {
                BluetoothDevice device = (BluetoothDevice)
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                onReceiveActionConnectionStateChanged(device, prevState, state,
                                                                    intent.getExtras());
            }
            if (action.equals(BluetoothAvrcpController.ACTION_TRACK_EVENT)) {
                MediaMetadata mMetaData = (MediaMetadata)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_METADATA);

                PlaybackState mState = (PlaybackState)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYBACK);
                if(mMetaData != null)
                    onMetaDataChanged(mMetaData);
                if(mState != null)
                    onPlaybackStateChanged(mState);
            }
            if (action.equals(BluetoothAvrcpController.ACTION_PLAYER_SETTING)) {
                BluetoothAvrcpPlayerSettings plAppSett = (BluetoothAvrcpPlayerSettings)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_PLAYER_SETTING);
                if(plAppSett != null)
                    onPlayerAppSettingChanged(plAppSett);
            }
            if (action.equals(BluetoothAvrcpController.AVAILABLE_MEDIA_PLAYERS_UPDATE)) {
                MediaPlayerList = (BluetoothAvrcpRemoteMediaPlayers)
                        intent.getParcelableExtra(BluetoothAvrcpController.EXTRA_REMOTE_PLAYERS);
                if (MediaPlayerList != null)
                    onMediaPlayerListChanged(MediaPlayerList);
            }
            if (action.equals(BluetoothAvrcpController.BROWSED_PLAYER_CHANGED)) {
                BluetoothAvrcpRemoteMediaPlayers mBrowsedPlayer =
                    (BluetoothAvrcpRemoteMediaPlayers)intent.getParcelableExtra
                                  (BluetoothAvrcpController.EXTRA_REMOTE_PLAYERS);
                if (mBrowsedPlayer != null) {
                    int browsedPlayerId = mBrowsedPlayer.getPlayerIds()[0];
                    Log.d(TAG," Browsed Player Changed playerId = " + browsedPlayerId);
                    if(browsedPlayerId != 0)
                        onBrowsePlayerSelected(browsedPlayerId);
                }
            }
            if (action.equals(BluetoothAvrcpController.ADDRESSED_PLAYER_CHANGED)) {
                BluetoothAvrcpRemoteMediaPlayers mAddressedPlayer =
                        (BluetoothAvrcpRemoteMediaPlayers)intent.getParcelableExtra
                                           (BluetoothAvrcpController.EXTRA_REMOTE_PLAYERS);
                if (mAddressedPlayer != null) {
                    int addressedPlayerId = mAddressedPlayer.getPlayerIds()[0];
                    if(addressedPlayerId != 0)
                        onAddressedPlayerSelected(addressedPlayerId);
                }
            }
            if(action.equals(BluetoothAvrcpController.AVRCP_BROWSE_THUMBNAILS_UPDATE)) {
                Bundle extras = intent.getExtras();
                Bundle data = new Bundle();
                Message msg = new Message();
                long [] mediaIdList = extras.getLongArray
                                             (BluetoothAvrcpController.EXTRA_MEDIA_IDS);
                String [] thumbNailList = extras.getStringArray
                                            (BluetoothAvrcpController.EXTRA_THUMBNAILS);
                Log.d(TAG," Recvd ThumbNail list size = " + mediaIdList.length);
                data.putLongArray("mediaIdList", mediaIdList);
                data.putStringArray("thumbNailList", thumbNailList);
                if (mCurrentScope == AVRCP_SCOPE_NOW_PLAYING) {
                    msg = mPressandHoldHandler.obtainMessage(UPDATE_NPL_THUMBNAIL);
                }
                else if(mCurrentScope == AVRCP_SCOPE_VFS) {
                    msg = mPressandHoldHandler.obtainMessage(UPDATE_VFS_THUMBNAIL);
                }
                else if(mCurrentScope == AVRCP_SCOPE_SEARCH) {
                    msg = mPressandHoldHandler.obtainMessage(UPDATE_SEARCH_THUMBNAIL);
                }
                msg.setData(data);
                mPressandHoldHandler.sendMessage(msg);
            }
        }
        private void onMetaDataChanged(MediaMetadata mMetaData) {
            parseMetaData(mMetaData);
            Log.d(TAG," onMetaDataChanged Title " + titleNameText + " Artist " + artistText +
                  " genre "+ genreText + " album " + albumText + " TrackNum " + trackNumText);
            displayMetaData();
        }
        private void onPlaybackStateChanged(PlaybackState mState) {
            parsePlaybackState(mState);
            Log.d(TAG," onPlaybackStateCHanged playstatus" + playStatusText + " playTime "
                                                + playText);
            displayPlayState();
        }
        private void onPlayerAppSettingChanged(BluetoothAvrcpPlayerSettings mPlAppSett) {
            Log.d(TAG," onPlayerAppSettingChanged ");
            parsePlayerAppSetting(mPlAppSett);
            displayPlayeAppSetting();
        }
        private void onReceiveActionConnectionStateChanged(BluetoothDevice device,
                int prevState, int state, Bundle features) {
            Logger.v(TAG, "onReceiveActionConnectionStateChanged: AVRCP: " +
                    device.getAddress() + " (" +
                    String.valueOf(prevState) + " -> " +
                    String.valueOf(state) + ")");
            if (state ==  BluetoothProfile.STATE_DISCONNECTED) {
                cleanup();
                if (device.equals(mDevice))
                    mDevice = null;
                resetDisplay();
                Toast.makeText(mLocalActivity, "Device " + device + " AVRCP Disconnected",
                                                                   Toast.LENGTH_SHORT).show();
            }
            else if(state == BluetoothProfile.STATE_CONNECTED) {
                mDevice = device;
                if(mAvrcpController!= null) {
                    parseMetaData(mAvrcpController.getMetadata(mDevice));
                    displayMetaData();
                    parsePlaybackState(mAvrcpController.getPlaybackState(mDevice));
                    displayPlayState();
                    parsePlayerAppSetting(mAvrcpController.getPlayerSettings(mDevice));
                    displayPlayeAppSetting();
                    int pixel = SystemProperties.getInt("persist.bt.avrcp.ca.pixel", 500);
                    mAvrcpController.startFetchingAlbumArt("JPEG", pixel, pixel, 2000000);
                    /* call GetRemoteAvailableMediaPlayers just after launch */
                    GetRemoteAvailableMediaPlayers();
                    /* call GetRemoteBrowsedPlayer just after launch */
                    GetRemoteBrowsedPlayers();
                    Log.d(TAG," RC Feat = " + mAvrcpController.getSupportedFeatures(mDevice));
                }
                Toast.makeText(mLocalActivity, "Device " + device + " AVRCP Connected",
                                                                    Toast.LENGTH_SHORT).show();
            }
        }

        private void onMediaPlayerListChanged(BluetoothAvrcpRemoteMediaPlayers mMediaPlayerList) {
            Log.d(TAG," onMediaPlayerListChanged ");
            mCurrentScope = AVRCP_SCOPE_MEDIA_PLAYERLIST;
            displayMediaPlayerList(mMediaPlayerList);
            printMediaPlayerInfo(mMediaPlayerList);
        }

        private void onBrowsePlayerSelected(int browsedPlayerId) {
            Log.d(TAG," onBrowsePlayerSelected id = "+ browsedPlayerId);
            updateNewIds(mCurrAddrPlayerID, browsedPlayerId);
            mPathStack = new Stack<String>();
            mMediaBrowser = new MediaBrowser(mContext, new ComponentName("com.android.bluetooth",
                             "com.android.bluetooth.avrcp.AvrcpControllerBrowseService"),
                    browseMediaConnectionCallback, null);
            mMediaBrowser.connect();
        }

        private void onAddressedPlayerSelected(int addressedPlayerId) {
            Log.d(TAG," onAddressedPlayerSelected ");
            updateNewIds(addressedPlayerId, mCurrBrowsePlayerID);
        }
    };
    private View.OnTouchListener onTouchListenerRW = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent me) {
            v.onTouchEvent(me);
            Log.d(TAG," onTouch for RW " + me.getAction());
            if (me.getAction() == MotionEvent.ACTION_UP){
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != 
                        (mAvrcpController.getConnectionState(mDevice))){
                    if (mPressandHoldHandler != null)
                        mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.
                         obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                         PASS_THRU_CMD_ID_REWIND,BluetoothAvrcpController.KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != 
                        (mAvrcpController.getConnectionState(mDevice))){
                    if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD)))
                        mPressandHoldHandler.sendMessage(mPressandHoldHandler.
                         obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                          PASS_THRU_CMD_ID_REWIND,BluetoothAvrcpController.KEY_STATE_PRESSED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            return true;
        }
    };
    private View.OnTouchListener onTouchListenerFF = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent me) {
            v.onTouchEvent(me);
            Log.d(TAG," onTouch for FF " + me.getAction());
            if (me.getAction() == MotionEvent.ACTION_UP){
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != 
                        (mAvrcpController.getConnectionState(mDevice))){
                        if (mPressandHoldHandler != null)
                            mPressandHoldHandler.sendMessageAtFrontOfQueue(mPressandHoldHandler.
                             obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                                PASS_THRU_CMD_ID_FF,BluetoothAvrcpController.KEY_STATE_RELEASED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            else if (me.getAction() == MotionEvent.ACTION_DOWN) {
                if ((mAvrcpController != null) && mDevice != null &&
                        BluetoothProfile.STATE_DISCONNECTED != 
                             (mAvrcpController.getConnectionState(mDevice))){
                        if ((mPressandHoldHandler != null)&&(!mPressandHoldHandler.
                                                   hasMessages(SEND_PASS_THROUGH_CMD)))
                            mPressandHoldHandler.sendMessage(mPressandHoldHandler.
                                 obtainMessage(SEND_PASS_THROUGH_CMD,BluetoothAvrcpController.
                                    PASS_THRU_CMD_ID_FF,BluetoothAvrcpController.
                                    KEY_STATE_PRESSED));
                    } else {
                        Logger.e(TAG, "passthru command not sent, connection unavailable");
                    }
            }
            return true;
        }
    };
    private final class PressandHoldHandler extends Handler {
        private PressandHoldHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG," Recvd Msg " + msg.what);
            int keyState = msg.arg2;
            int keyCode = msg.arg1;
            switch(msg.what) {
            case SEND_PASS_THROUGH_CMD:
                if (keyState == BluetoothAvrcpController.KEY_STATE_PRESSED) {
                    Message msgsend = mPressandHoldHandler.obtainMessage(SEND_PASS_THROUGH_CMD,
                           keyCode, keyState);
                    mPressandHoldHandler.sendMessageDelayed(msgsend, 1000);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
                                                              getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                else if(keyState == BluetoothAvrcpController.KEY_STATE_RELEASED) {
                    if (mPressandHoldHandler.hasMessages(SEND_PASS_THROUGH_CMD))
                        mPressandHoldHandler.removeMessages(SEND_PASS_THROUGH_CMD);
                    if ((mAvrcpController != null) && mDevice != null &&
                            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
                                                                 getConnectionState(mDevice))){
                            mAvrcpController.sendPassThroughCmd(mDevice, keyCode, keyState);
                    }
                }
                break;
            case FETCH_CURRENT_INFO:
                if(mAvrcpController != null) {
                    int pixel = SystemProperties.getInt("persist.bt.avrcp.ca.pixel", 500);
                    mAvrcpController.startFetchingAlbumArt("JPEG", pixel, pixel, 2000000);
                    List<BluetoothDevice> deviceList = mAvrcpController.getConnectedDevices();
                    if(deviceList.isEmpty()) break;
                    /* Right now we support only one connection */
                    mDevice = deviceList.get(0);
                    parseMetaData(mAvrcpController.getMetadata(mDevice));
                    displayMetaData();
                    parsePlaybackState(mAvrcpController.getPlaybackState(mDevice));
                    displayPlayState();
                    parsePlayerAppSetting(mAvrcpController.getPlayerSettings(mDevice));
                    displayPlayeAppSetting();
                    /* call GetRemoteAvailableMediaPlayers just after launch */
                    GetRemoteAvailableMediaPlayers();
                    /* call GetRemoteBrowsedPlayer just after launch */
                    GetRemoteBrowsedPlayers();
                }
                break;
            case TEST_SETBROWSED_PLAYER:
                if (msg.arg1 == 0) break;
                setBrowsedPlayer(msg.arg1);
                break;
            case TEST_SETADDRESSED_PLAYER:
                if (msg.arg1 == 0) break;
                setAddressedPlayer(msg.arg1);
                break;
            case TEST_CHANGE_PATH:
                if(!isAvrcpMBSConnected()) return;
                if((mConnState != CONNECTED) || (mCurrBrowsePlayerID == 0)) break;
                if (pendingFetchCmd != FETCH_DONE) break;
                Bundle data = msg.getData();
                String folderId = data.getString("folder");
                int direction = data.getInt("direction");
                pendingFetchCmd = FETCH_VFS;
                changePath(folderId, (byte)direction);
                break;
            case TEST_FETCH_NPL:
                if(!isAvrcpMBSConnected()) return;
                if (mConnState != CONNECTED) break;
                if (pendingFetchCmd != FETCH_DONE) break;
                if (mNowPlayingItems != null) {
                    mNowPlayingItems.clear();
                    mNowPlayingItems =  null;
                }
                resetFolderList();
                final String command = BluetoothAvrcpController.
                                            BROWSE_COMMAND_GET_NOW_PLAYING_LIST;
                if (mMediaController != null) {
                    mCurrentScope = AVRCP_SCOPE_NOW_PLAYING;
                    pendingFetchCmd = FETCH_NPL;
                    mMediaController.sendCommand(command, null, null);
                }
                break;
            case TEST_BROWSE_ROOT:
                 if(!isAvrcpMBSConnected()) return;
                 if (pendingFetchCmd != FETCH_DONE) break;
                 pendingFetchCmd = FETCH_VFS;
                 if(mCurrBrowsePlayerID != 0)
                     onBrowseConnect();
                break;
            case TEST_SEARCH:
                if(!isAvrcpMBSConnected()) return;
                if(mCurrBrowsePlayerID == 0) break;
                if (pendingFetchCmd != FETCH_DONE) break;
                if (mSearchItems != null) {
                     mSearchItems.clear(); mSearchItems =  null;
                }
                resetFolderList();
                getSearchList((String)msg.obj);
                break;
            case TEST_ADD_TO_NPL:
                if(mMediaController == null) break;
                Bundle nplData =  new Bundle();
                if (mMediaController != null) {
                    Long id = msg.getData().getLong("id");
                    nplData.putLong(BluetoothAvrcpController.EXTRA_ADD_TO_NOW_PLAYING_LIST,
                            id);
                    mMediaController.sendCommand(mAvrcpController.
                            BROWSE_COMMAND_ADD_TO_NOW_PLAYING_LIST, nplData, null);
                }
                break;
            case TEST_PLAY_ITEM:
                if(mMediaController == null) break;
                if (mMediaController != null) {
                    MediaController.TransportControls controls = mMediaController.
                                                              getTransportControls();
                    long id = msg.getData().getLong("ID");
                    String idStr = String.valueOf(id);
                    if (mCurrentScope == AVRCP_SCOPE_VFS) {
                        controls.playFromMediaId(idStr, null);
                    }
                    else if((mCurrentScope == AVRCP_SCOPE_SEARCH)||
                            (mCurrentScope == AVRCP_SCOPE_NOW_PLAYING)) {
                        controls.skipToQueueItem(id);
                    }
                } else {
                    Log.e(TAG, "mediaController = null");
                }
                break;
            case REFRESH_CURRENT_FOLDER:
                if(mCurrBrowsePlayerID == 0) break;
                if (pendingFetchCmd != FETCH_DONE) break;
                pendingFetchCmd = FETCH_VFS;
                refershCurrentFolder();
                break;
            case UPDATE_NPL_THUMBNAIL:
                Bundle nplThumbData = msg.getData();
                long [] mediaIdList = nplThumbData.getLongArray("mediaIdList");
                String[] thumbNailList = nplThumbData.getStringArray("thumbNailList");
                updateNowPlayingThunbNail(mediaIdList, thumbNailList);
                break;
            case UPDATE_SEARCH_THUMBNAIL:
                Bundle searchThumbData = msg.getData();
                mediaIdList = searchThumbData.getLongArray("mediaIdList");
                thumbNailList = searchThumbData.getStringArray("thumbNailList");
                updateSearchListThunbNail(mediaIdList, thumbNailList);
                break;
            case UPDATE_VFS_THUMBNAIL:
                Bundle vfsThumbData = msg.getData();
                mediaIdList = vfsThumbData.getLongArray("mediaIdList");
                thumbNailList = vfsThumbData.getStringArray("thumbNailList");
                updateVFSListThunbNail(mediaIdList, thumbNailList);
                break;
            }
        }
    }

    /* Browse connection state callback handler */
    private MediaBrowser.ConnectionCallback browseMediaConnectionCallback =
            new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            mConnState = CONNECTED;
            Log.d(TAG, "mediaBrowser CONNECTED to ");
        }

        @Override
        public void onConnectionFailed() {
            mConnState = DISCONNECTED;
            Log.e(TAG, "mediaBrowser Connection failed with ");
        }

        @Override
        public void onConnectionSuspended() {
            mConnState = SUSPENDED;
            Log.e(TAG, "mediaBrowser SUSPENDED connection with ");
        }
    };

    /* This must be called when user chooses a player for list shown on UI after
       GetRemoteAvailableMediaPlayers
     */
    private void setBrowsedPlayer(int selectedId) {
        // checking for error cases
        if (selectedId == '0') {
            Log.w(TAG, " No Available Players to set, ERROR!");
        } else {
            // update current browse player id
            mAvrcpController.SetBrowsedPlayer(selectedId);
            Log.d(TAG, "setBrowsedPlayer for selectedId: " + selectedId);
        }
    }
    private void setAddressedPlayer(int selectedId) {
        // checking for error cases
        if (isMediaPlayerListEmpty() || selectedId == '0') {
            Log.w(TAG, " No Available Players to set, ERROR!");
        } else {
            // update current addressed player id
            mAvrcpController.SetAddressedPlayer(selectedId);
            Log.d(TAG, "SetAddressedPlayer for selectedId: " + selectedId);
        }
    }
    private boolean isMediaPlayerListEmpty() {
        Log.d(TAG, "isMediaPlayerListEmpty ");
        if (MediaPlayerList != null)
            return false;
        else
            return true;
    }
    public void refershCurrentFolder() {
        if ((mCurrentScope == AVRCP_SCOPE_VFS)&&(mMediaController != null)&&
                (mPathStack != null) && (mMediaBrowser != null)) {
            if (!mPathStack.isEmpty()) {
                resetFolderList();
                mMediaBrowser.subscribe(mPathStack.peek(), folderItemsCb);
            }
            else { // scope: VFS and pathStack is empty
                onBrowseConnect();
            }
        }
    }
    public void updateNowPlayingThunbNail(long[] mediaIdList, String[] thumbNailList) {
        if ((mNowPlayingItems != null)&&(mNowPlayingItems.size() > 0)) {
            if (itemDisplayList != null)
                itemDisplayList.clear();
            for (int i = 0; i < mNowPlayingItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                DisplayItem display = new DisplayItem();
                StringBuilder str = new StringBuilder();
                long uid = Long.valueOf(mNowPlayingItems.get(i).
                        getDescription().getMediaId());
                str.append(mNowPlayingItems.get(i).getDescription().getMediaId());
                str.append(" : ");
                str.append(mNowPlayingItems.get(i).getDescription().getTitle());
                display.addTextFeild(str.toString());
                int imageIndex = -1;
                for (int k = 0; k < mediaIdList.length; k++) {
                    if (uid == mediaIdList[k]) {
                        imageIndex = k; break;
                    }
                }
                if (imageIndex >= 0) {
                    Bitmap img = BitmapFactory.decodeFile(thumbNailList[imageIndex]);
                            display.addImageFeild(img);
                }
                itemDisplayList.add(display);
            }
        }
        else {
            return;
        }
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    if ((itemDisplayList != null)&&(!itemDisplayList.isEmpty())) {
                        mBluetoothAvrcpFolderAdapter.clear();
                        mBluetoothAvrcpFolderAdapter.addAll(itemDisplayList);
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    public void updateSearchListThunbNail(long[] mediaIdList, String[] thumbNailList) {
        if ((mSearchItems != null)&&(mSearchItems.size() > 0)) {
            if (itemDisplayList != null)
                itemDisplayList.clear();
            for (int i = 0; i < mSearchItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                DisplayItem display = new DisplayItem();
                StringBuilder str = new StringBuilder();
                long uid = Long.valueOf(mSearchItems.get(i).
                                                getDescription().getMediaId());
                str.append(mSearchItems.get(i).getDescription().getMediaId());
                str.append(" : ");
                str.append(mSearchItems.get(i).getDescription().getTitle());
                display.addTextFeild(str.toString());
                int imageIndex = -1;
                for (int k = 0; k < mediaIdList.length; k++) {
                    if (uid == mediaIdList[k]) {
                        imageIndex = k; break;
                    }
                }
                if (imageIndex >= 0) {
                Bitmap img = BitmapFactory.decodeFile(thumbNailList[imageIndex]);
                        display.addImageFeild(img);
                }
                itemDisplayList.add(display);
            }
        }
        else {
            return;
        }
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    if ((itemDisplayList != null)&&(!itemDisplayList.isEmpty())) {
                        mBluetoothAvrcpFolderAdapter.clear();
                        mBluetoothAvrcpFolderAdapter.addAll(itemDisplayList);
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    public void updateVFSListThunbNail(long[] mediaIdList, String[] thumbNailList) {
        if ((mFolderItems != null)&&(mFolderItems.size() > 0)) {
            if (itemDisplayList != null)
                itemDisplayList.clear();
            for (int i = 0; i < mFolderItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                DisplayItem display = new DisplayItem();
                StringBuilder str = new StringBuilder();
                long uid = Long.valueOf(mFolderItems.get(i).getMediaId());
                str.append(mFolderItems.get(i).getMediaId());
                str.append(" : ");
                Bitmap image = null;
                if(mFolderItems.get(i).isBrowsable()) {
                    str.append("Browsable");
                    image = BitmapFactory.decodeResource(
                            mContext.getResources(), R.drawable.folder);
                } else {
                    str.append("Non-Browsable");
                    int imageIndex = -1;
                    for (int k = 0; k < mediaIdList.length; k++) {
                        if (uid == mediaIdList[k]) {
                            imageIndex = k; break;
                        }
                    }
                    if (imageIndex >= 0) {
                        image = BitmapFactory.decodeFile(thumbNailList[imageIndex]);
                    }
                }
                if (image != null)
                    display.addImageFeild(image);
                str.append(" : ");
                if(mFolderItems.get(i).isPlayable())
                    str.append("Playable");
                else
                    str.append("Non-Playable");

                str.append(" : ");
                str.append(mFolderItems.get(i).getDescription().getTitle());
                display.addTextFeild(str.toString());
                itemDisplayList.add(display);
            }
        }
        else {
            return;
        }
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    if ((itemDisplayList != null)&&(!itemDisplayList.isEmpty())) {
                        mBluetoothAvrcpFolderAdapter.clear();
                        mBluetoothAvrcpFolderAdapter.addAll(itemDisplayList);
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    /* initialize mediacontroller in order to communicate with BT Avrcp apps */
    private void onBrowseConnect() {
        boolean isError = false;
        mMediaId =  null; mRootFolderUid = null;mPathStack.clear();
        try {
            /* get rootfolder uid from Bt Avrcp apps */
            if (mMediaId == null) {
                Log.d(TAG," calling getRoot ");
                mMediaId = mMediaBrowser.getRoot();
                mRootFolderUid = mMediaId;
                mPathStack.push(mMediaId);
            }

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            mMediaController = new MediaController(mContext, mMediaBrowser.getSessionToken());
            if (mMediaController != null) {
                setAvrcpMediaController(mMediaController);
            }
            Log.d(TAG," calling subscribe on root folder ");
            /* get root folder items */
            mCurrentScope = AVRCP_SCOPE_VFS;
            resetFolderList();
            mMediaBrowser.subscribe(mRootFolderUid, folderItemsCb);
        } catch (IllegalArgumentException ex) {
            isError = true;
            Log.e(TAG, "onBrowseConnect : No Session token");
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            isError = true;
            Log.e(TAG, "onBrowseConnect : Null pointer during init");
            ex.printStackTrace();
        }

    }

    public void setAvrcpMediaController(MediaController mediaController) {
        /* create handler for mediaControllercb */
        mThread = new HandlerThread("NowPlayingListChangeHandler");
        mThread.start();
        mLooper = mThread.getLooper();
        mHandler = new Handler(mLooper);
        mMediaController.registerCallback(mediaControllerCb, mHandler);

        /* invalidate and update current now playing list */
        if (mNowPlayingItems != null) {
            mNowPlayingItems.clear();
        }
        mNowPlayingItems = mediaController.getQueue();
    }

    private MediaController.Callback mediaControllerCb = new MediaController.Callback() {

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            Log.d(TAG," onQueueChnaged currScope = " + mCurrentScope);
            if(queue != null) {
                Log.d(TAG," que size = " + queue.size());
            }
            if (mCurrentScope == AVRCP_SCOPE_NOW_PLAYING) {
                if (pendingFetchCmd == FETCH_NPL)
                    pendingFetchCmd = FETCH_DONE;
                displayNowPlayingList();
            } else if (mCurrentScope == AVRCP_SCOPE_SEARCH) {
                if (pendingFetchCmd == FETCH_SEARCH)
                    pendingFetchCmd = FETCH_DONE;
                displaySearchItems();
            }
        }
        @Override
        public void onSessionEvent(String event, Bundle extras) {
            Log.d(TAG," onSessionEvent = " + event + " currentScope = " + mCurrentScope);
            if (event.equals(BluetoothAvrcpController.SESSION_EVENT_REFRESH_LIST)) {
                if ((mCurrentScope == AVRCP_SCOPE_NOW_PLAYING) &&
                        !(mPressandHoldHandler.hasMessages(TEST_FETCH_NPL))) {
                    Message msg = mPressandHoldHandler.obtainMessage(TEST_FETCH_NPL);
                    mPressandHoldHandler.sendMessageDelayed(msg, 100);
                }
                if(mCurrentScope == AVRCP_SCOPE_VFS) {
                    Message msg = mPressandHoldHandler.obtainMessage(REFRESH_CURRENT_FOLDER);
                    mPressandHoldHandler.sendMessageDelayed(msg, 100);
                }
                if(mCurrentScope == AVRCP_SCOPE_SEARCH) {

                }
            }
        }

    };

    /* Note: this should be called from UI button get Now Playing list*/
    public void displayNowPlayingList() {
        Log.d(TAG, "displayNowPlayingList");
        if (mNowPlayingItems == null) {
            if (mMediaController != null) {
                mNowPlayingItems = mMediaController.getQueue();
                if (mNowPlayingItems != null) {
                    Log.d(TAG," size = " + mNowPlayingItems.size());
                    mCurrFolderNumItems = mNowPlayingItems.size();
                    for (int i = 0; i < mNowPlayingItems.size(); i++) {
                        Log.d(TAG," data " + mNowPlayingItems.get(i).toString() + " Que Id = " +
                                mNowPlayingItems.get(i).getQueueId());
                    }
                    mLocalActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLock.lock();
                            try {
                                Log.d(TAG," item Size = " + mNowPlayingItems.size());
                                if (mNowPlayingItems.size() > 0) {
                                    ArrayList<DisplayItem> displayList =
                                            new ArrayList<DisplayItem>();

                                    for (int i = 0; i < mNowPlayingItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                                        DisplayItem display = new DisplayItem();
                                        StringBuilder str = new StringBuilder();
                                        str.append(mNowPlayingItems.get(i).getDescription()
                                                .getMediaId());
                                        str.append(" : ");
                                        str.append(mNowPlayingItems.get(i).getDescription()
                                                .getTitle());
                                        display.addTextFeild(str.toString());
                                        Bitmap img = BitmapFactory.decodeResource(
                                                mContext.getResources(), R.drawable.deafult_media);
                                        display.addImageFeild(img);
                                        displayList.add(display);
                                    }
                                    mBluetoothAvrcpFolderAdapter.clear();
                                    mBluetoothAvrcpFolderAdapter.addAll(displayList);
                                }
                            }
                            finally {
                                mLock.unlock();
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "displayNowPlayingList: Could not fetch now playing list");
                }
            } else {
                Log.e(TAG, "mediaController = null");
            }
        } else {
            Log.d(TAG, "display cached now playing list");
        }
    }

    public void addToNPL() {
        Log.d(TAG," addToNPL curScope = " + mCurrentScope);
        Bundle data =  new Bundle();
        if ((mCurrentScope == AVRCP_SCOPE_VFS) && (mFolderItems != null)) {
            String mediaId = mFolderItems.get(0).getMediaId();
            Log.d(TAG," adding id = " + mediaId);
            data.putLong(
                    BluetoothAvrcpController.EXTRA_ADD_TO_NOW_PLAYING_LIST,Long.valueOf(mediaId));
            mMediaController.sendCommand(
                    mAvrcpController.BROWSE_COMMAND_ADD_TO_NOW_PLAYING_LIST, data, null);
        }
    }

    /* Note: this should be called from UI button get Search list*/
    public void displaySearchItems() {
        Log.d(TAG, "displaySearchItems");
        if (mSearchItems == null) {
            if (mMediaController != null) {
                mSearchItems = mMediaController.getQueue();
                if (mSearchItems != null) {
                    mLocalActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLock.lock();
                            try {
                                Log.d(TAG," item Size = " + mSearchItems.size());
                                if (mSearchItems.size() > 0) {
                                    ArrayList<DisplayItem> displayList =
                                            new ArrayList<DisplayItem>();

                                    for (int i = 0; i < mSearchItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                                        DisplayItem display = new DisplayItem();
                                        StringBuilder str = new StringBuilder();
                                        str.append(mSearchItems.get(i).getDescription().getTitle());
                                        str.append(" : ");
                                        str.append(mSearchItems.get(i).getDescription()
                                                .getMediaId());
                                        display.addTextFeild(str.toString());
                                        Bitmap img = BitmapFactory.decodeResource(
                                                mContext.getResources(), R.drawable.deafult_media);
                                        display.addImageFeild(img);
                                        displayList.add(display);
                                    }
                                    mBluetoothAvrcpFolderAdapter.clear();
                                    mBluetoothAvrcpFolderAdapter.addAll(displayList);
                                }
                            }
                            finally {
                                mLock.unlock();
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "displaySearchItems: Could not fetch search list");
                }
            } else {
                Log.e(TAG, "mediaController = null");
            }
        } else {
            Log.d(TAG, "display cached search list");
        }
    }

    public void getSearchList(String searchId) {
        Log.d(TAG, "getSearchList");
        if (mSearchItems == null) {
            if (mMediaController != null) {
                MediaController.TransportControls controls = mMediaController.getTransportControls();
                final String query = searchId;
                mCurrentScope = AVRCP_SCOPE_SEARCH;
                pendingFetchCmd = FETCH_SEARCH;
                controls.playFromSearch(query, null);
            } else {
                Log.e(TAG, "mediaController = null");
            }
        } else {
            Log.d(TAG, "display cached now playing list");
        }
    }


    /*Note: ChangePath should be called when user navigates. folderUid is new path uid*/
    public void changePath(String folderUid, byte direction) {
        if (isAvrcpMBSConnected()) {
            Log.d(TAG, "changePath.direction = " + direction);
            String newPath = folderUid;
            resetFolderList();

            /* check direction and change the path */
            if (direction == 0x01) { /* move down */
                if (mMediaBrowser != null) {
                    mMediaBrowser.subscribe(newPath.concat(":" + BluetoothAvrcpController.
                            BROWSE_COMMAND_BROWSE_FOLDER_DOWN), folderItemsCb);
                    Log.d(TAG, "dir down pushed " + newPath);
                    mPathStack.push(newPath);
                } else {
                    Log.e(TAG, "mediaController is null");
                }
            } else if (direction == 0x00) {
                /* move folder up */
                Log.d(TAG," topmost = " + mPathStack.peek());
                if (!mPathStack.peek().equals(mRootFolderUid))
                    mPathStack.pop();
                newPath = mPathStack.peek();
                Log.d(TAG," dir up setting path as " + newPath);

                if (mMediaBrowser != null) {
                    mMediaBrowser.subscribe(newPath.concat(":" + BluetoothAvrcpController.
                                             BROWSE_COMMAND_BROWSE_FOLDER_UP), folderItemsCb);
                } else {
                    Log.e(TAG, "mediaController is null");
                }
            } else { /* invalid direction */
                Log.w(TAG, "changePath : Invalid direction");
                return;
            }
        } else {
            Log.w(TAG, "MBS to Avrcp apps not connected");
        }
    }

    public boolean isAvrcpMBSConnected() {
        if (mMediaBrowser != null) {
            return mMediaBrowser.isConnected();
        } else {
            Log.d(TAG, "isAvrcpMBSConnected: mMediaBrowser = null!");
            return false;
        }
    }

    /* called when connection to Avrcp MBS is closed */
    public void cleanup() {
        Log.d(TAG, "cleanup");
          disconnectFromAvrcpMediaBrowser();
          if (mFolderItems != null) {
               mFolderItems.clear();
               mFolderItems = null;
          }
          mCurrFolderNumItems = 0;
          if (mSearchItems != null) {
              mSearchItems.clear();
              mSearchItems = null;
         }
          if (mNowPlayingItems  != null) {
              mNowPlayingItems .clear();
              mNowPlayingItems  = null;
         }
          if (mPathStack   != null) {
              mPathStack  .clear();
              mPathStack   = null;
         }
        mConnState = DISCONNECTED; 
        mCurrBrowsePlayerID = 0;
        mCurrAddrPlayerID = 0;
        mMediaId = null;
        mRootFolderUid = null;
        mMediaController = null;
        mMediaBrowser = null;
        pendingFetchCmd = FETCH_DONE;
        if (itemDisplayList != null) {
            itemDisplayList.clear();
        }
    }

    private void connectToAvrcpMediaBrowser() {
        Log.d(TAG, "connectToAvrcpMediaBrowser");
        mMediaBrowser.connect();
    }

    public void disconnectFromAvrcpMediaBrowser() {
        Log.d(TAG, "disconnectFromAvrcpMediaBrowser");
        if (mMediaBrowser != null)
            mMediaBrowser.disconnect();
    }

    /* Subscription callback handler. Subscribe to a folder to get its contents */
    private MediaBrowser.SubscriptionCallback folderItemsCb =
            new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
            Log.d(TAG, "OnChildren Loaded folder items: childrens= " + children.size());

            if(mFolderItems == null) {
                Log.d(TAG, "sending setbrowsed player rsp");
                mFolderItems = children;
                String[] folderNames = {"root"};
            } else {
                mFolderItems = children;
                mCurrFolderNumItems = mFolderItems.size();
            }
            mMediaBrowser.unsubscribe(parentId);
            if (pendingFetchCmd == FETCH_VFS)
                pendingFetchCmd = FETCH_DONE;
            if (children.size() > 0) {
                boolean isChildrenFound = true;
                displayFolderItemList(parentId, mFolderItems);
            } else {
                Log.e (TAG, "children list is null for parent id:" + parentId);
                /* Lets Remove the last element from top, because list is empty here */
                String itemPopped = mPathStack.pop();
                Log.d(TAG," Item Popped = " + itemPopped);
            }
        }
    };

    private final ServiceListener mAvrcpControllerServiceListener = new ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Logger.v(TAG, "onServiceConnected() profile = " + profile);
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = (BluetoothAvrcpController) proxy;
                mPressandHoldHandler.sendEmptyMessageDelayed(FETCH_CURRENT_INFO, 200);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Logger.v(TAG, "onServiceDisconnected() profile = " + profile);
            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpController = null;
                mDevice = null;
                resetDisplay();
                cleanup();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "onCreate()");

        ActivityHelper.initialize(this, R.layout.activity_avrcp_test);
        BluetoothConnectionReceiver.registerObserver(this);
        ActivityHelper.setActionBarTitle(this, R.string.title_avrcp_test);
        mLocalActivity = this;
        initializeViewFragments();
        // bind to app service
        HandlerThread thread = new HandlerThread("BT-TestAppPressandHoldHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mPressandHoldHandler = new PressandHoldHandler(looper);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAvrcpController.ACTION_TRACK_EVENT);
        filter.addAction(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        filter.addAction(BluetoothAvrcpController.AVAILABLE_MEDIA_PLAYERS_UPDATE);
        filter.addAction(BluetoothAvrcpController.BROWSED_PLAYER_CHANGED);
        filter.addAction(BluetoothAvrcpController.ADDRESSED_PLAYER_CHANGED);
        filter.addAction(BluetoothAvrcpController.AVRCP_BROWSE_THUMBNAILS_UPDATE);
        registerReceiver(mAvrcpControllerReceiver, filter);
        mCurrentScope = AVRCP_SCOPE_NONE;
        mContext = this;
        mBluetoothAdapter.getProfileProxy(mContext, mAvrcpControllerServiceListener,
                BluetoothProfile.AVRCP_CONTROLLER);
        itemDisplayList = new ArrayList<DisplayItem>();
        pendingFetchCmd = FETCH_DONE;
    }

    @Override
    protected void onDestroy() {
        Logger.v(TAG, "onDestroy");
        cleanup();
        mDevice = null;
        unregisterReceiver(mAvrcpControllerReceiver);
        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.AVRCP_CONTROLLER,
                mAvrcpController);
        BluetoothConnectionReceiver.removeObserver(this);
        mPressandHoldHandler.removeCallbacksAndMessages(null);
        Looper looper = mPressandHoldHandler.getLooper();
        mCurrentScope = AVRCP_SCOPE_NONE;
        if (looper != null) {
            looper.quit();
        }
        mContext = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Logger.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Logger.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenu = menu;
        return true;
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Logger.v(TAG, "onDeviceChanged() BD = "+ device.getAddress());
        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Logger.v(TAG, "onDeviceDisconected");
        mDevice = null;
    }

    private void prepareActionBar() {
        Logger.v(TAG, "prepareActionBar()");

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.title_avrcp_test);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }
    public boolean isDeviceConnected() {
        return ((mAvrcpController != null) && mDevice != null &&
                BluetoothProfile.STATE_DISCONNECTED !=
                (mAvrcpController.getConnectionState(mDevice)));
    }

    public void onClickPassthruPlay(View v) {
        Logger.v(TAG, "onClickPassthruPlay()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
                    getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PLAY, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PLAY, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickNextGroup(View v) {
        Logger.v(TAG, "onClickNextGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                   PASS_THRU_CMD_ID_NEXT_GRP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_NEXT_GRP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "grp nav command not sent, connection unavailable");
        }
    }
    public void onClickPrevGroup(View v) {
        Logger.v(TAG, "onClickPrevGroup()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PREV_GRP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendGroupNavigationCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PREV_GRP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "grp nav command not sent, connection unavailable");
        }
    }
    public void onClickPassthruGetElementAttributes(View v) {
        Logger.v(TAG, "onClickPassthruGetElementAttributes()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            if (mEdit.getText().toString().equals("ca_bv_01_c")) {
                mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_VFS_ATTR_ID,
                        BluetoothAvrcpController.KEY_STATE_PRESSED);
            }
            else if (mEdit.getText().toString().equals("get_item")) {
                mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_ELEMENT_ATTRIBUTE_ID,
                        BluetoothAvrcpController.KEY_STATE_PRESSED);
            }
            else {
            mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_ELEMENT_ATTRIBUTE_ID,
                BluetoothAvrcpController.KEY_STATE_PRESSED);
            }
        } else {
            Logger.e(TAG, "onClickPassthruGetElementAttributes not sent, connection unavailable");
        }
    }
    public void onClickPassthruGetPlayStatus(View v) {
        Logger.v(TAG, "onClickPassthruGetPlayStatus()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, PTS_GET_PLAY_STATUS_ID,
                BluetoothAvrcpController.KEY_STATE_PRESSED);
        } else {
            Logger.e(TAG, "onClickPassthruGetElementAttributes not sent, connection unavailable");
        }
    }
    public void onClickPassthruForward(View v) {
        Logger.v(TAG, "onClickPassthruForward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_FORWARD, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_FORWARD, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickPassthruBackward(View v) {
        Logger.v(TAG, "onClickPassthruBackward()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_BACKWARD, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_BACKWARD, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }
    public void onClickToggleRepeat(View v) {
        Logger.v(TAG, "onClickToggleRepeat()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                                                                   getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) == 0) {
            Log.e(TAG,"onClickToggleRepeat repeat not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_REPEAT);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                                                                                   nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickToggleEq(View v) {
        Logger.v(TAG, "onClickToggleEq()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                                            getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) == 0) {
            Log.e(TAG,"onClickToggleEq equalizer not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ON;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ON:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER,
                            nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickAddToNPL(View v) {
        Logger.v(TAG, "onClickAddToNPL()");
        if ((mAvrcpController == null) ||
                (mDevice == null)||
                (BluetoothProfile.STATE_CONNECTED !=
                (mAvrcpController.getConnectionState(mDevice)))) {
            return;
        } else {
            addToNPL();
            return;
        }
    }
    public void onClickToggleScan(View v) {
        Logger.v(TAG, "onClickToggleScan()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) == 0) {
            Log.e(TAG,"onClickToggleScan Scan not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_SCAN);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN,
                                        nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }
    public void onClickToggleShuffle(View v) {
        Logger.v(TAG, "onClickToggleShuffle()");
        if ((mAvrcpController == null) ||
            (mDevice == null)||
            (BluetoothProfile.STATE_CONNECTED != (mAvrcpController.getConnectionState(mDevice)))) {
               return;
        }
        BluetoothAvrcpPlayerSettings mPlAppSetting = mAvrcpController.
                getPlayerSettings(mDevice);
        int setting = mPlAppSetting.getSettings();
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) == 0) {
            Log.e(TAG,"onClickToggleShuffle Shuffle not supported");
            return;
        }
        int value = mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE);
        int nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
        boolean supported = false;
        do {
            switch(value) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_GROUP;
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                nextVal = BluetoothAvrcpPlayerSettings.STATE_OFF;
                break;
            }
            BluetoothAvrcpPlayerSettings mNewRepeatSetting = new
                    BluetoothAvrcpPlayerSettings(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE);
            mNewRepeatSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE,
                                        nextVal);
            supported = mAvrcpController.setPlayerApplicationSetting(mNewRepeatSetting);
            value = nextVal;
        }while(!supported);
    }

    public void onClickPassthruPause(View v) {
        Logger.v(TAG, "onClickPassthruPause()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PAUSE, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_PAUSE, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }
    }

    public void onClickPassthruStop(View v) {
        Logger.v(TAG, "onClickPassthruStop()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_STOP, BluetoothAvrcpController.KEY_STATE_PRESSED);
            mAvrcpController.sendPassThroughCmd(mDevice, BluetoothAvrcpController.
                    PASS_THRU_CMD_ID_STOP, BluetoothAvrcpController.KEY_STATE_RELEASED);
        } else {
            Logger.e(TAG, "passthru command not sent, connection unavailable");
        }

    }
    public void onSetBrowsedPlayer(View v) {
        Logger.v(TAG, "onSetBrowsedPlayer()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            int playerId =  Integer.parseInt(mEdit.getText().toString());
            Message msg = mPressandHoldHandler.obtainMessage(TEST_SETBROWSED_PLAYER,
                                   playerId, 0);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onSetBrowsedPlayer not sent, connection unavailable");
        }
    }
    public void onSetAddressedPlayer(View v) {
        Logger.v(TAG, "onSetAddressedPlayer()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            int playerId =  Integer.parseInt(mEdit.getText().toString());
            Message msg = mPressandHoldHandler.obtainMessage(TEST_SETADDRESSED_PLAYER,
                    playerId, 0);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onSetAddressedPlayer not sent, connection unavailable");
        }
    }
    public void onFolderDown(View v) {
        Logger.v(TAG, "onFolderDown()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            Bundle data = new Bundle();
            data.putString("folder", mEdit.getText().toString());
            data.putInt("direction", 1);
            Message msg = mPressandHoldHandler.obtainMessage(TEST_CHANGE_PATH); msg.setData(data);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onFolderDown not sent, connection unavailable");
        }
    }
    public void onFolderRefresh(View v) {
        Logger.v(TAG, "onFolderRefresh()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            Message msg = mPressandHoldHandler.obtainMessage(REFRESH_CURRENT_FOLDER);
            mPressandHoldHandler.sendMessage(msg);
        } else {
            Logger.e(TAG, "onFolderDown not sent, connection unavailable");
        }
    }
    public void onFolderUP(View v) {
        Logger.v(TAG, "onFolderUP()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            Bundle data = new Bundle();
            data.putString("folder", "empty");
            data.putInt("direction", 0);
            Message msg = mPressandHoldHandler.obtainMessage(TEST_CHANGE_PATH); msg.setData(data);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onFolderUP not sent, connection unavailable");
        }
    }
    public void onFetchNPL(View v) {
        Logger.v(TAG, "onFetchNPL()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            Message msg = mPressandHoldHandler.obtainMessage(TEST_FETCH_NPL);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onFetchNPL not sent, connection unavailable");
        }
    }
    public void onBrowseRoot(View v) {
        Logger.v(TAG, "onBrowseRoot()");
        if ((mAvrcpController != null) && mDevice != null &&
            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.getConnectionState(mDevice))){
            Message msg = mPressandHoldHandler.obtainMessage(TEST_BROWSE_ROOT);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onBrowseRoot not sent, connection unavailable");
        }
    }
    public void onClickSearch(View v) {
        String id = mEdit.getText().toString();
        Logger.v(TAG, "onClickSearch() , id : " + id);
        if ((mAvrcpController != null) && mDevice != null &&
	            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
	                                                             getConnectionState(mDevice))){
            Message msg = mPressandHoldHandler.obtainMessage(TEST_SEARCH,0,0,id);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onClickSearch not sent, connection unavailable");
        }
    }
    public void onClickAddtoNowPlaying(View v) {
        Logger.v(TAG, "onClickAddtoNowPlaying()");
        long id = Long.parseLong(mEdit.getText().toString());
        if ((mAvrcpController != null) && mDevice != null &&
	            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
	                                                             getConnectionState(mDevice))){
            Message msg = mPressandHoldHandler.obtainMessage(TEST_ADD_TO_NPL,0,0);
            Bundle data = new Bundle();
            data.putLong("id", id);
            msg.setData(data);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onClickAddtoNowPlaying not sent, connection unavailable");
        }
    }
    public void onClickPlayItem(View v) {
        Logger.v(TAG, "onClickPlayItem()");
        long id = Long.parseLong(mEdit.getText().toString());
        if ((mAvrcpController != null) && mDevice != null &&
	            BluetoothProfile.STATE_DISCONNECTED != (mAvrcpController.
	                                                             getConnectionState(mDevice))){
            Bundle data = new Bundle();
            data.putLong("ID", id);
            Message msg = mPressandHoldHandler.obtainMessage(TEST_PLAY_ITEM,0,0);
            msg.setData(data);
            mPressandHoldHandler.sendMessageDelayed(msg, 100);
        } else {
            Logger.e(TAG, "onClickAddtoNowPlaying not sent, connection unavailable");
        }
    }

    private void parseMetaData(MediaMetadata mMetaData) {
        if(mMetaData == null) return;
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
            artistText = mMetaData.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TITLE))
            titleNameText = mMetaData.getString(MediaMetadata.METADATA_KEY_TITLE);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
            albumText = mMetaData.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_GENRE))
            genreText = mMetaData.getString(MediaMetadata.METADATA_KEY_GENRE);

        StringBuffer trackNumBuffer = new StringBuffer();
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            trackNumBuffer.append(String.valueOf(mMetaData.getLong(MediaMetadata.
                                                                METADATA_KEY_TRACK_NUMBER)));
        trackNumBuffer.append(" | ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
            trackNumBuffer.append(String.valueOf(mMetaData.getLong(MediaMetadata.
                                                               METADATA_KEY_NUM_TRACKS)));
        trackNumText = trackNumBuffer.toString();

        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_DURATION))
            trackLen = mMetaData.getLong(MediaMetadata.METADATA_KEY_DURATION);
        /*
         * Image is given preference over thumbnail
         */
        if (mMetaData.containsKey(MediaMetadata.METADATA_KEY_DISPLAY_ICON)) {
            Log.d(TAG," ParseMetaData, update Thumbnail");
            coverArtBitmap = mMetaData.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        }
        else if (mMetaData.containsKey(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)) {
            Log.d(TAG," ParseMetaData, update IMAGE");
            String mImageLocation = mMetaData.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
            coverArtBitmap = BitmapFactory.decodeFile(mImageLocation);
        }
        else
            coverArtBitmap = null;
    }
    private void parsePlaybackState(PlaybackState mState) {
        if(mState == null) return;
        playStatusText = "NONE";
        switch(mState.getState()) {
        case PlaybackState.STATE_STOPPED:
            playStatusText = "STOPPED";
            break;
        case PlaybackState.STATE_PLAYING:
            playStatusText = "PLAYING";
            break;
        case PlaybackState.STATE_PAUSED:
            playStatusText = "PAUSED";
            break;
        case PlaybackState.STATE_FAST_FORWARDING:
            playStatusText = "FORWARDING";
            break;
        case PlaybackState.STATE_REWINDING:
            playStatusText = "REWINDING";
            break;
        }

        long playing_time = mState.getPosition();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(playing_time);
        playing_time = playing_time - (60*minutes*1000);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(playing_time);
        StringBuffer strPlayTime = new StringBuffer();
        strPlayTime.append(String.valueOf(minutes));
        strPlayTime.append(":");
        strPlayTime.append(String.valueOf(seconds));
        strPlayTime.append(" | ");
        long totalTrackTime = trackLen;
        minutes = TimeUnit.MILLISECONDS.toMinutes(totalTrackTime);
        totalTrackTime = totalTrackTime - (60*minutes*1000);
        seconds = TimeUnit.MILLISECONDS.toSeconds(totalTrackTime);
        strPlayTime.append(String.valueOf(minutes));
        strPlayTime.append(":");
        strPlayTime.append(String.valueOf(seconds));
        playText = strPlayTime.toString();

    }
    private void parsePlayerAppSetting(BluetoothAvrcpPlayerSettings mPlAppSetting) {
        if(mPlAppSetting == null) return;
        int setting = mPlAppSetting.getSettings();
        Log.d(TAG," parsePlayerAppSetting Sett" + setting);
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) != 0) {
            Log.d(TAG," parsePlayerAppSetting value eq:" + mPlAppSetting.
                 getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                equalizerText = "EQ_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ON:
                equalizerText = "EQ_ON";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
            Log.d(TAG," parsePlayerAppSetting value rep:" + mPlAppSetting.getSettingValue
                    (BluetoothAvrcpPlayerSettings.SETTING_REPEAT));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                repeatText = "REP_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                repeatText = "REP_SINGLE_TRACK";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                repeatText = "REP_ALL_TRACK";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                repeatText = "REP_GRP";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) != 0) {
            Log.d(TAG," parsePlayerAppSetting value shuffle:" + mPlAppSetting.getSettingValue
                   (BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                shuffleText = "SHUFFLE_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                shuffleText = "SHUFFLE_ALL";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                shuffleText = "SHUFFLE_GRP";
                break;
            }
        }
        if((setting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) != 0) {
            Log.d(TAG," parsePlayerAppSetting value scan:" + mPlAppSetting.getSettingValue
                    (BluetoothAvrcpPlayerSettings.SETTING_SCAN));
            switch(mPlAppSetting.getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN)) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                scanText = "SCAN_OFF";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                scanText = "SCAN_ALL";
                break;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                scanText = "SCAN_GRP";
                break;
            }
        }
    }

    private void displayMetaData() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mTrackNumber.setText(trackNumText);
                    mTitleName.setText(titleNameText);
                    mArtistName.setText(artistText);
                    mGenreStatus.setText(genreText);
                    mAlbumName.setText(albumText);
                    if (coverArtBitmap != null) {
                        mCoverArtImageView.setImageBitmap(coverArtBitmap);
                    }
                    else {
                        Log.d(TAG," displayMetaData: bitmap is null, invalidate it ");
                        mCoverArtImageView.setImageBitmap(null);
                        mCoverArtImageView.invalidate();
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });

    }
    private void displayPlayState() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mPlayStatus.setText(playStatusText);
                    mPlayTime.setText(playText);
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void displayPlayeAppSetting() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    mEqualizerStatus.setText(equalizerText);
                    mScanStatus.setText(scanText);
                    mShuffleStatus.setText(shuffleText);
                    mRepeatStatus.setText(repeatText);
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }

	private void displayMediaPlayerList(BluetoothAvrcpRemoteMediaPlayers mMediaPlayers) {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    int[] playerIds = MediaPlayerList.getPlayerIds();
                    if (playerIds != null) {
                        String[] displayItem = new String[playerIds.length];
                        for (int i = 0; i < playerIds.length; i++) {
                            int id = playerIds[i];
                            StringBuilder str = new StringBuilder();
                            str.append(MediaPlayerList.getPlayerName(id)); str.append(" : ");
                            str.append(Integer.toString(id));str.append(" : ");
                            byte[] featureMask = new byte[PLAYER_FEATURE_MASK_SIZE];
                            featureMask = MediaPlayerList.getFeatureMask(id);
                            if(TestFeatureMaskBitSet(BluetoothAvrcpController.
                                             PLAYER_FEATURE_BITMASK_BROWSING_BIT, featureMask))
                                str.append("Browsable");
                            else
                                str.append("Non-Browsable");
                            str.append(" : ");
                            if(TestFeatureMaskBitSet(BluetoothAvrcpController.
                                               PLAYER_FEATURE_BITMASK_SEARCH_BIT, featureMask))
                                str.append("Searchable");
                            else
                                str.append("Non-Searchable");
                            str.append(" : ");
                            displayItem[i] = str.toString();
                        }
                        mBluetoothAvrcpPlayerAdapter.clear();
                        mBluetoothAvrcpPlayerAdapter.addAll(displayItem);
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }

    public void printMediaPlayerInfo(BluetoothAvrcpRemoteMediaPlayers mMediaPlayers) {
        if (mMediaPlayers == null) return;
        int[] playerIds = mMediaPlayers.getPlayerIds();
        if ((playerIds == null) || (playerIds.length == 0)) return;
        for (int i = 0; i < playerIds.length; i++) {
            int id = playerIds[i];
            Log.d(TAG," playerid = "+ id + " " + mMediaPlayers.getPlayStatus(id) + " " +
                   mMediaPlayers.getSubType(id) + " " + mMediaPlayers.getMajorType(id) +
                   " " +mMediaPlayers.getPlayerName(id));
            byte[] featureMask = new byte[PLAYER_FEATURE_MASK_SIZE];
            int start = i*PLAYER_FEATURE_MASK_SIZE;
            featureMask = mMediaPlayers.getFeatureMask(id);
            Log.d(TAG," Browsable = " + TestFeatureMaskBitSet(BluetoothAvrcpController.
                                           PLAYER_FEATURE_BITMASK_BROWSING_BIT, featureMask));
            Log.d(TAG," Browsable + addr = " + TestFeatureMaskBitSet(BluetoothAvrcpController.
                            PLAYER_FEATURE_BITMASK_ONLY_BROWSABLE_WHEN_ADDRESSED, featureMask));
            Log.d(TAG," search = " + TestFeatureMaskBitSet(BluetoothAvrcpController.
                                               PLAYER_FEATURE_BITMASK_SEARCH_BIT, featureMask));
        }
    }
    public boolean TestFeatureMaskBitSet (int featureBit, byte[] mFeatureMask) {
        int index = (featureBit)/8;
        int bit = (featureBit)%8;

        if ((mFeatureMask[index]&(1<<(bit))) != 0) {
            return true;
        }
        return false;
    }
    public void displayMediaBrowseItem(List<MediaBrowser.MediaItem> children) {
        Log.d(TAG," item Size = " + children.size());
        for (int i = 0; i < children.size(); i++) {
            Log.d(TAG," mediaId = " + children.get(i).getMediaId() + " browsable " +
                children.get(i).isBrowsable() + " isPlayable " + children.get(i).isPlayable());
            Log.d(TAG," data " + children.get(i).toString());
        }
    }

    private void displayFolderItemList(String parentId, List<MediaBrowser.MediaItem> children) {
        displayMediaBrowseItem(children);
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    Log.d(TAG," displayFolderItemList: ");
                    Log.d(TAG," item Size = " + mFolderItems.size());
                    if (mFolderItems.size() > 0) {
                        ArrayList<DisplayItem> displayList = new ArrayList<DisplayItem>();
                        for (int i = 0; i < mFolderItems.size() && i < MAX_SUPPORT_LIST_ENTRY; i++) {
                            DisplayItem display = new DisplayItem();
                            StringBuilder str = new StringBuilder();
                            str.append(mFolderItems.get(i).getMediaId());
                            str.append(" : ");
                            Bitmap image;
                            if(mFolderItems.get(i).isBrowsable()) {
                                str.append("Browsable");
                                image = BitmapFactory.decodeResource(
                                        mContext.getResources(), R.drawable.folder);
                            } else {
                                str.append("Non-Browsable");
                                image = BitmapFactory.decodeResource(
                                        mContext.getResources(), R.drawable.deafult_media);
                            }
                            display.addImageFeild(image);
                            str.append(" : ");
                            if(mFolderItems.get(i).isPlayable())
                                str.append("Playable");
                            else
                                str.append("Non-Playable");

                            str.append(" : ");
                            str.append(mFolderItems.get(i).getDescription().getTitle());
                            display.addTextFeild(str.toString());
                            displayList.add(display);
                        }
                        mBluetoothAvrcpFolderAdapter.clear();
                        mBluetoothAvrcpFolderAdapter.addAll(displayList);
                    }
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void resetFolderList() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                try {
                    if(mBluetoothAvrcpFolderAdapter != null)
                        mBluetoothAvrcpFolderAdapter.clear();
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void resetDisplay() {
        mLocalActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLock.lock();
                trackNumText = "NOT_SUPP";
                titleNameText = "NOT_SUPP";
                equalizerText = "NOT_SUPP";
                artistText = "NOT_SUPP";
                scanText = "NOT_SUPP";
                shuffleText = "NOT_SUPP";
                repeatText = "NOT_SUPP";
                playStatusText = "NOT_SUPP";
                genreText = "NOT_SUPP";
                playText = "NOT_SUPP";
                albumText = "NOT_SUPP";
                trackLen = 0;
                coverArtBitmap = null;
                try {
                    mTrackNumber.setText(trackNumText);
                    mTitleName.setText(titleNameText);
                    mEqualizerStatus.setText(equalizerText);
                    mArtistName.setText(artistText);
                    mScanStatus.setText(scanText);
                    mShuffleStatus.setText(shuffleText);
                    mRepeatStatus.setText(repeatText);
                    mPlayStatus.setText(playStatusText);
                    mGenreStatus.setText(genreText);
                    mPlayTime.setText(playText);
                    mAlbumName.setText(albumText);
                    mCoverArtImageView.setImageBitmap(null);
                    mCoverArtImageView.invalidate();
                    mFolderList = null;
                    mBluetoothAvrcpFolderAdapter.clear();
                    mBluetoothAvrcpPlayerAdapter.clear();
                }
                finally {
                    mLock.unlock();
                }
            }
        });
    }
    private void initializeViewFragments() {
        Log.v(TAG,"initializeViewFragments");
        mShuffleStatus = (TextView) findViewById(R.id.shuffle_status);
        mRepeatStatus = (TextView) findViewById(R.id.repeat_status);
        mGenreStatus = (TextView) findViewById(R.id.genre_name);
        mArtistName = (TextView) findViewById(R.id.artist_name);
        mAlbumName = (TextView) findViewById(R.id.album_name);
        mPlayTime = (TextView) findViewById(R.id.playing_time);
        mScanStatus = (TextView) findViewById(R.id.scan_status);
        mPlayStatus = (TextView) findViewById(R.id.play_status);
        mEqualizerStatus = (TextView) findViewById(R.id.equalizer_status);
        mTrackNumber = (TextView) findViewById(R.id.track_number);
        mTitleName = (TextView) findViewById(R.id.title_name);
        ffButton = (Button) findViewById(R.id.onClickPassthruFF);
        rwButton = (Button) findViewById(R.id.onClickPassthruRewind);
        mCoverArtImageView = (ImageView) findViewById(R.id.cover_art_image_view);
        Log.d(TAG," call setImageBitmap ");
        mCoverArtImageView.setImageBitmap(null);
        ffButton.setOnTouchListener(onTouchListenerFF);
        rwButton.setOnTouchListener(onTouchListenerRW);
        folderListView = (ListView) findViewById(R.id.folderListView);
        playerListView = (ListView) findViewById(R.id.playerListView);
        mBluetoothAvrcpFolderAdapter = new BluetoothAvrcpFolderAdapter();
        mBluetoothAvrcpPlayerAdapter = new BluetoothAvrcpPlayerAdapter();
        folderListView.setAdapter(mBluetoothAvrcpFolderAdapter);
        search = (Button) findViewById(R.id.onClickSearch);
        addtonp = (Button) findViewById(R.id.onClickAddtoNowPlaying);
        playitem = (Button) findViewById(R.id.onClickPlayItem);
        mEdit   = (EditText)findViewById(R.id.editText);
        mEdit.setText(String.valueOf(0));
		playerListView.setAdapter(mBluetoothAvrcpPlayerAdapter);
    }

    private void GetRemoteAvailableMediaPlayers() {
        Log.v(TAG,"GetRemoteAvailableMediaPlayers");
        MediaPlayerList = mAvrcpController.GetRemoteAvailableMediaPlayer();
        mCurrentScope = AVRCP_SCOPE_MEDIA_PLAYERLIST;
        if (MediaPlayerList != null) {
            displayMediaPlayerList(MediaPlayerList);
            printMediaPlayerInfo(MediaPlayerList);
        } else {
            Log.e(TAG,"GetRemoteAvailableMediaPlayers: No available media players yet !!!");
        }
    }

    private void GetRemoteBrowsedPlayers() {
        BluetoothAvrcpRemoteMediaPlayers browsedPlayer = null;
        Log.v(TAG,"GetRemoteBrowsedPlayers");
        browsedPlayer = mAvrcpController.GetBrowsedPlayer();
        if (browsedPlayer != null) {
            Log.v(TAG,"GetRemoteBrowsedPlayers: Browse media player available");
        } else {
            Log.e(TAG,"GetRemoteBrowsedPlayers: No browsed media players yet !!!");
        }
    }

    /* utility function to update the global values of current Addressed and browsed player */
    private void updateNewIds(int addrPlayerId, int browsedPlayerId) {
        mCurrAddrPlayerID = addrPlayerId;
        mCurrBrowsePlayerID = browsedPlayerId;

        Log.d(TAG, "Updated CurrentIds: AddrPlayerID:" + mCurrAddrPlayerID + " to "
                + addrPlayerId + ", BrowsePlayerID: to " + mCurrBrowsePlayerID);
    }

    class BluetoothAvrcpFolderAdapter extends ArrayAdapter<DisplayItem> {
        BluetoothAvrcpFolderAdapter() {
            super(AvrcpTestActivity.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = getLayoutInflater();
                view = inflater.inflate(R.layout.folder_row, null);
            }
            DisplayItem tmp = getItem(position);
            String tmpStr = tmp.textFeild;
            Bitmap image = tmp.imageFeild;

            ((TextView) view.findViewById(R.id.folder_title)).setText(tmpStr);
            ((ImageView) view.findViewById(R.id.icon)).setImageBitmap(image);
            return view;
        }

    }

    class BluetoothAvrcpPlayerAdapter extends ArrayAdapter<String> {
        BluetoothAvrcpPlayerAdapter() {
            super(AvrcpTestActivity.this, android.R.layout.simple_list_item_1);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = getLayoutInflater();
                view = inflater.inflate(R.layout.media_player_row, null);
            }
            String tmp = getItem(position);
            ((TextView) view.findViewById(R.id.player_title)).setText(tmp);
            return view;
        }
    }
}
