/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codeaurora.bluetooth.bttestapp;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothPbapClient;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.util.Log;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.NameData;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardProperty;
import org.codeaurora.bluetooth.bttestapp.R;
import org.codeaurora.bluetooth.bttestapp.services.IPbapServiceCallback;
import org.codeaurora.bluetooth.bttestapp.util.Logger;
import org.codeaurora.bluetooth.bttestapp.util.MonkeyEvent;
import org.codeaurora.bluetooth.bttestapp.PbapProfile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

public class PbapTestActivity extends MonkeyActivity implements IBluetoothConnectionObserver {

    private final String TAG = "PbapTestActivity";

    private static final int MSG_UNKNOWN = -1;

    // Message for PBAP connection
    private static final int MSG_DISCONNECTED = 0;
    private static final int MSG_CONNECTED = 1;

    // Message for custom action response
    private static final int MSG_PULL_PHONEBOOK_RESP = 80;
    private static final int MSG_PULL_VCARD_LISTING_RESP = 81;
    private static final int MSG_PULL_VCARD_ENTRY_RESP = 82;
    private static final int MSG_SET_PHONEBOOK_RESP = 83;
    private static final int MSG_ABORT_RESP = 84;

    // "telecom/pb.vcf"
    private static final int DEFAULT_PHONEBOOK_POSITION = 0;

    /*
     * Class constants.
     */
    private static final int REQUEST_CODE_GET_FILTER_FOR_DOWNLOAD_TAB = 0;
    private static final int REQUEST_CODE_GET_FILTER_FOR_VCARD_TAB = 1;

    private static final short MAX_COUNT_DEFAULT_VALUE = 10;
    private static final short OFFSET_DEFAULT_VALUE = 0;
    private static final String HANDLE_DEFAULT_VALUE = "1";  // with suffix ".vcf"
    private static final String VCF_SUFFIX = ".vcf";

    private BluetoothDevice mDevice = null;

    /*
     * Common UI.
     */
    private ActionBar mActionBar = null;
    private ViewFlipper mViewFlipper = null;
    /*
     * Download functionality UI.
     */
    private Spinner mDownloadSpinner = null;
    private RadioGroup mRadioGroupFormat = null;
    private RadioButton mRadioButtonVCard21 = null;
    private RadioButton mRadioButtonVCard30 = null;
    private EditText mEditTextDownloadMaxListCount = null;
    private EditText mEditTextDownloadOffsetValue = null;
    private Button mButtonFilter = null;
    private Button mButtonDownload = null;

    private ProgressBar mDownloadProgressBar = null;
    private TextView mTextViewDownloadNothingFound = null;
    private ListView mListViewDownloadContacts = null;

    private RadioButton mRadioButtonSearchName = null;
    private RadioButton mRadioButtonSearchNumber = null;
    private RadioButton mRadioButtonSearchSound = null;
    private EditText mEditTextSearchValue = null;
    private EditText mEditTextBrowseMaxListCount = null;
    private EditText mEditTextBrowseOffsetValue = null;
    private RadioButton mRadioButtonOrderUnordered = null;
    private RadioButton mRadioButtonOrderAlphabetical = null;
    private RadioButton mRadioButtonOrderIndexed = null;
    private RadioButton mRadioButtonOrderPhonetic = null;
    private Button mButtonBrowseSearch = null;

    private Spinner mBrowseSpinner = null;
    private ProgressBar mBrowseProgressBar = null;
    private TextView mTextViewBrowseNothingFound = null;
    private ListView mListViewBrowseContacts = null;

    /*
     * vCard Details functionality.
     */
    private Spinner mVcardSpinner = null;
    private VcardView mVcardView = null;
    private EditText mEditTextHandleValue = null;

    /*
     * Download local variables.
     */
    private long mDownloadValueFilter = 0;
    private byte mDownloadValueCardType = PbapProfile.VCARD_TYPE_21;
    private int mDownloadValueMaxCount = MAX_COUNT_DEFAULT_VALUE;
    private int mDownloadValueOffset = OFFSET_DEFAULT_VALUE;
    private int mPhonebookSize = 0;
    private int mNewMissedCalls = 0;

    /*
     * Browse local variables.
     */
    private byte mBrowseValueOrder = PbapProfile.ORDER_INDEXED;
    private byte mBrowseValueSearchAttr = PbapProfile.SEARCH_ATTR_NAME;
    private int mBrowseValueMaxCount = MAX_COUNT_DEFAULT_VALUE;
    private int mBrowseValueOffset = OFFSET_DEFAULT_VALUE;
    private boolean mSetPhoneBookButtonClicked = false;

    /*
     * Action bar tabs.
     */
    private ActionBar.Tab mDownloadTab = null;
    private ActionBar.Tab mBrowseTab = null;
    private ActionBar.Tab mVcardTab = null;

    /*
     * vCard local variables.
     */
    private String mVcardHandleValue = HANDLE_DEFAULT_VALUE;
    private long mVcardValueFilter = 0;
    private byte mVcardValueCardType = PbapProfile.VCARD_TYPE_30;

    /*
     * Download adapter.
     */
    private VCardEntryAdapter mVCardEntryAdapter = null;

    OnItemClickListener mOnDownloadItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VCardEntry vcard = (VCardEntry) parent.getAdapter().getItem(position);
            mVcardView.setVCardEntry(vcard);
            mActionBar.selectTab(mVcardTab);
        }
    };

    /*
     * Browse adapter.
     */
    private BluetoothPbapCardAdapter mBluetoothPbapCardAdapter = null;
    private BluetoothPbapVcardListingAdapter mBluetoothPbapVcardListingAdapter = null;

    OnItemClickListener mOnBrowseItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VCardEntry pbapCard = (VCardEntry) parent.getAdapter().getItem(position);
            // FIXME: Not supported
            String vcardHandle = null;
            pullVcardEntry(vcardHandle);
        }
    };

    /*
     * PBAP Service.
     */
    private PbapProfile mPbap = null;
    private final IPbapServiceCallback mPbapServiceCallback = new IPbapServiceCallback() {

        @Override
        public void onSetPhoneBookDone() {
            if (mSetPhoneBookButtonClicked) {
                mSetPhoneBookButtonClicked = false;
                showToastShort("SetPhoneBook done");
            }

            new MonkeyEvent("pbap-setphonebook", true).send();
        }

        @Override
        public void onPullPhoneBookDone(String pbName, int phonebookSize, int missedCalls) {
            // vCard has been stored into Contact DB.
            storeResponse(phonebookSize, missedCalls);

            stopProgressBarDownload();

            showToastLong(pbName + ", size: " + phonebookSize);

            cleanResponse();
        }

        @Override
        public void onPullVcardListingDone(ArrayList<String> vcardListing, int phonebookSize, int missedCalls) {
            storeResponse(phonebookSize, missedCalls);

            if (vcardListing != null) {
                mBluetoothPbapVcardListingAdapter.clear();
                mBluetoothPbapVcardListingAdapter.addAll(vcardListing);
            }

            stopProgressBarBrowse();

            showToastShort("PhonebookSize=" + phonebookSize + ", NewMissedCalls=" + missedCalls);

            cleanResponse();
        }

        @Override
        public void onPullVcardEntryDone(String vcard) {
            Logger.d(TAG, "onReceivedPbapPullVcardEntryDone()");
            showToastShort("PullVcardEntry done { " + getContactInfo(vcard) + " }");

            VCardEntry vcardEntry = createVcardEntry(vcard);

            mVcardView.setVCardEntry(vcardEntry);
            mActionBar.selectTab(mVcardTab);
        }

        @Override
        public void onAbortDone() {
            showToastLong("PBAP session abort done");
        }

        @Override
        public void onSetPhoneBookError(int result) {
            showResult("SetPhoneBook", result);

            new MonkeyEvent("pbap-setphonebook", false).send();
        }

        @Override
        public void onPullPhoneBookError(String pbName, int result) {
            Logger.e(TAG, "Received from PBAP pull phone book error.");
            stopProgressBarDownload();
            mListViewDownloadContacts.setVisibility(View.GONE);
            showResult("PullPhoneBook " + pbName, result);
            new MonkeyEvent("pbap-pullphonebook", false).send();
        }

        @Override
        public void onPullVcardListingError(int result) {
            Logger.e(TAG, "Received from PBAP listing error.");
            stopProgressBarBrowse();
            mListViewBrowseContacts.setVisibility(View.GONE);
            showResult("PullvCardListing", result);
            new MonkeyEvent("pbap-pullvcardlisting", false).send();
        }

        @Override
        public void onPullVcardEntryError(int result) {
            Logger.e(TAG, "Received from PBAP vCard entry error.");
            showResult("PullvCardEntry", result);
            new MonkeyEvent("pbap-pullvcardentry", false).send();
        }

        @Override
        public void onPullPhoneBookSizeError(int result) {
            Logger.e(TAG, "Received from PBAP pull phone book size error.");
            showResult("PullPhoneBookSize", result);
            new MonkeyEvent("pbap-pullphonebook-size", false).send();
        }

        @Override
        public void onPullVcardListingSizeError(int result) {
            Logger.e(TAG, "Received from PBAP pull vCard listing size error.");
            showResult("PullvCardListingSize", result);
            new MonkeyEvent("pbap-pullvcardlisting-size", false).send();
        }

        @Override
        public void onAbortError(int result) {
            showResult("PBAP session abort", result);
        }

        @Override
        public void onSessionConnected() {
            showToastShort("PBAP session connected");
            invalidateOptionsMenu();
            setButtonsVisible(true);
        }

        @Override
        public void onSessionDisconnected() {
            showToastShort("PBAP session disconnected");
            invalidateOptionsMenu();
            setButtonsVisible(false);
        }
    };

    /*
     * PBAP Service Connection.
     */
    private final ServiceConnection mPbapServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(TAG, "onServiceConnected()");

            ProfileService profileService = ((ProfileService.LocalBinder) service).getService();
            mPbap = profileService.getPbapProfile();

            mDevice = profileService.getDevice();
            Logger.d(TAG, "onServiceConnected device: " + mDevice);

            setButtonsVisible(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d(TAG, "onServiceDisconnected()");
            mPbap = null;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action " + action);
            if (BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                handleActionConnectionStateChanged(intent);
            } else if (BluetoothPbapClient.ACTION_PHONEBOOK_DOWNLOAD_STATE_CHANGED.equals(action)) {
                handlePhonebookDownloadStateChanged(intent);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Logger.d(TAG, "handleMessage " + what);
            switch (what) {
                case MSG_DISCONNECTED:
                    processDisconnected();
                    break;

                case MSG_CONNECTED:
                    processConnected();
                    break;

                case MSG_PULL_PHONEBOOK_RESP:
                    processPullPhonebookResp((String) msg.obj, msg.arg1, msg.arg2);
                    break;

                case MSG_PULL_VCARD_LISTING_RESP:
                    processPullVcardListingResp(msg.arg1, (Bundle) msg.obj);
                    break;

                case MSG_PULL_VCARD_ENTRY_RESP:
                    processPullVcardEntryResp(msg.arg1, (Bundle) msg.obj);
                    break;

                case MSG_SET_PHONEBOOK_RESP:
                    processSetPhonebookResp(msg.arg1, (Bundle) msg.obj);
                    break;

                case MSG_ABORT_RESP:
                    processAbortResp(msg.arg1, (Bundle) msg.obj);
                    break;

                default:
                    Logger.e(TAG, "Unknown msg: " + msg.what);
                    break;
            }
        }
    };

    private void enableButton(int id, boolean enable) {
        Button button = (Button) findViewById(id);
        button.setEnabled(enable);
    }

    private void setButtonsVisible (boolean visible) {
        enableButton(R.id.pbap_download_filter_button, visible);
        enableButton(R.id.pbap_download, visible);
        enableButton(R.id.pbap_browse_search, visible);
        enableButton(R.id.pbap_download_getsize, visible);
        enableButton(R.id.pbap_download_abort, visible);
        enableButton(R.id.pbap_browse_getsize, visible);
        enableButton(R.id.pbap_browse_abort, visible);
        enableButton(R.id.pbap_browse_set_phonebook, visible);
        enableButton(R.id.pbap_vcard_set_phonebook, visible);
        enableButton(R.id.pbap_vcard_get_vcard, visible);
    }

    private void initIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothPbapClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothPbapClient.ACTION_PHONEBOOK_DOWNLOAD_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Logger.v(TAG, "onCreate()");
        ActivityHelper.initialize(this, R.layout.activity_pbap_test);
        ActivityHelper.setActionBarTitle(this, R.string.title_pbap_test);

        prepareActionBar();
        prepareUserInterfaceDownload();
        prepareUserInterfaceBrowse();
        prepareUserInterfaceVcard();
        setButtonsVisible (false);

        mBluetoothPbapVcardListingAdapter = new BluetoothPbapVcardListingAdapter();
        mListViewBrowseContacts.setAdapter(mBluetoothPbapVcardListingAdapter);

        mVCardEntryAdapter = new VCardEntryAdapter();
        mListViewDownloadContacts.setAdapter(mVCardEntryAdapter);

        BluetoothConnectionReceiver.registerObserver(this);

        initIntentFilter();

        // bind to PBAP service
        Intent intent = new Intent(this, ProfileService.class);
        bindService(intent, mPbapServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.v(TAG, "onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.v(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        /*
         * Going to call abort if any pending request is ongoing,
         * checks for the same are handled internally
         */
        abort();

        super.onDestroy();
        Logger.v(TAG, "onDestroy()");

        unbindService(mPbapServiceConnection);
        BluetoothConnectionReceiver.removeObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.v(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.v(TAG, "onPause()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mActionBarMenu = menu;

        getMenuInflater().inflate(R.menu.menu_pbap_test, menu);

        int state = getConnectionState();
        if (state != BluetoothProfile.STATE_DISCONNECTED) {
            menu.findItem(R.id.menu_pbap_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_pbap_connect).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pbap_connect:
                connect();
                break;
            case R.id.menu_pbap_disconnect:
                disconnect();
                break;
            default:
                Logger.w(TAG, "Unknown item selected.");
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.v(TAG, "onActivityResult()");

        if (resultCode != RESULT_OK) {
            Logger.w(TAG, "Result code is not ok for req: " + requestCode + ".");
            return;
        }

        if (requestCode == REQUEST_CODE_GET_FILTER_FOR_DOWNLOAD_TAB) {
            long result = data.getLongExtra("result", -1);

            if (result != -1) {
                Logger.d(TAG, "Received download tab filter " + result + ".");
                mDownloadValueFilter = result;
            } else {
                Logger.w(TAG, "Somethings go wrong here!");
            }
        }

        if (requestCode == REQUEST_CODE_GET_FILTER_FOR_VCARD_TAB) {
            long result = data.getLongExtra("result", -1);

            if (result != -1) {
                Logger.d(TAG, "Received vcard tab filter " + result + ".");
                mVcardValueFilter = result;
            } else {
                Logger.w(TAG, "Somethings go wrong here!");
            }
        }
    }

    private void prepareUserInterfaceDownload() {
        mDownloadSpinner = (Spinner) findViewById(R.id.pbap_download_spinner);
        mDownloadSpinner.setSelection(DEFAULT_PHONEBOOK_POSITION, true);
        mButtonFilter = (Button) findViewById(R.id.pbap_download_filter_button);
        mButtonFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mButtonFilter.onClick()");

                Intent intent = new Intent(PbapTestActivity.this, VcardFilterActivity.class);
                intent.putExtra("filter", mDownloadValueFilter);
                intent.putExtra("type", mDownloadValueCardType);
                startActivityForResult(intent, REQUEST_CODE_GET_FILTER_FOR_DOWNLOAD_TAB);
            }
        });

        mEditTextDownloadMaxListCount = (EditText) findViewById(R.id.pbap_download_max_list_count);
        mEditTextDownloadMaxListCount.setText(String.valueOf(mDownloadValueMaxCount));

        mEditTextDownloadOffsetValue = (EditText) findViewById(R.id.pbap_download_offset_value);
        mEditTextDownloadOffsetValue.setText(String.valueOf(mDownloadValueOffset));

        mRadioGroupFormat = (RadioGroup) findViewById(R.id.pbap_download_formats);
        mRadioButtonVCard21 = (RadioButton) findViewById(R.id.pbap_download_vcard21);
        mRadioButtonVCard21.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButonVCard21.onClick()");
                mDownloadValueCardType = PbapProfile.VCARD_TYPE_21;
            }
        });

        mRadioButtonVCard30 = (RadioButton) findViewById(R.id.pbap_download_vcard30);
        mRadioButtonVCard30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButonVCard30.onClick()");
                mDownloadValueCardType = PbapProfile.VCARD_TYPE_30;
            }
        });

        mButtonDownload = (Button) findViewById(R.id.pbap_download);
        mButtonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mButtonDownload.onClick()");
                runPbapTestDownload();
            }
        });

        mDownloadProgressBar = (ProgressBar) findViewById(R.id.pbap_download_progress_bar);
        mTextViewDownloadNothingFound = (TextView) findViewById(R.id.pbap_download_test_list_empty);
        mListViewDownloadContacts = (ListView) findViewById(R.id.pbap_download_listview_contacts_list);
        mListViewDownloadContacts.setOnItemClickListener(mOnDownloadItemClickListener);

        mDownloadProgressBar.setVisibility(View.GONE);
        mListViewDownloadContacts.setVisibility(View.GONE);
    }

    private void prepareUserInterfaceBrowse() {
        mBrowseSpinner = (Spinner) findViewById(R.id.pbap_browse_spinner);
        mBrowseSpinner.setSelection(DEFAULT_PHONEBOOK_POSITION, true);
        mRadioButtonSearchName = (RadioButton) findViewById(R.id.pbap_browse_name);
        mRadioButtonSearchName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonSearchName.onClick()");
                mBrowseValueSearchAttr = PbapProfile.SEARCH_ATTR_NAME;
            }
        });

        mRadioButtonSearchNumber = (RadioButton) findViewById(R.id.pbap_browse_number);
        mRadioButtonSearchNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonSearchNumber.onClick()");
                mBrowseValueSearchAttr = PbapProfile.SEARCH_ATTR_NUMBER;
            }
        });

        mRadioButtonSearchSound = (RadioButton) findViewById(R.id.pbap_browse_sound);
        mRadioButtonSearchSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonSearchSound.onClick()");
                mBrowseValueSearchAttr = PbapProfile.SEARCH_ATTR_SOUND;
            }
        });

        mEditTextSearchValue = (EditText) findViewById(R.id.pbap_browse_search_value);

        mEditTextBrowseMaxListCount = (EditText) findViewById(R.id.pbap_browse_max_list_count);
        mEditTextBrowseMaxListCount.setText(String.valueOf(mBrowseValueMaxCount));

        mEditTextBrowseOffsetValue = (EditText) findViewById(R.id.pbap_browse_offset_value);
        mEditTextBrowseOffsetValue.setText(String.valueOf(mBrowseValueOffset));

        mRadioButtonOrderUnordered = (RadioButton) findViewById(R.id.pbap_browse_unordered);
        mRadioButtonOrderUnordered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonOrderUnordered.onClick()");
                mBrowseValueOrder = PbapProfile.ORDER_INDEXED;
            }
        });

        mRadioButtonOrderAlphabetical = (RadioButton) findViewById(R.id.pbap_browse_alphabetical);
        mRadioButtonOrderAlphabetical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonOrderAlphabetical.onClick()");
                mBrowseValueOrder = PbapProfile.ORDER_ALPHABETICAL;
            }
        });

        mRadioButtonOrderIndexed = (RadioButton) findViewById(R.id.pbap_browse_indexed);
        mRadioButtonOrderIndexed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonOrderIndexed.onClick()");
                mBrowseValueOrder = PbapProfile.ORDER_INDEXED;
            }
        });

        mRadioButtonOrderPhonetic = (RadioButton) findViewById(R.id.pbap_browse_phonetic);
        mRadioButtonOrderPhonetic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mRadioButtonOrderPhonetic.onClick()");
                mBrowseValueOrder = PbapProfile.ORDER_PHONETICAL;
            }
        });

        mButtonBrowseSearch = (Button) findViewById(R.id.pbap_browse_search);
        mButtonBrowseSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "mButtonBrowseSearch.onClick()");
                runPbapTestBrowse();
            }
        });

        mBrowseProgressBar = (ProgressBar) findViewById(R.id.pbap_browse_progress_bar);
        mTextViewBrowseNothingFound = (TextView) findViewById(R.id.pbap_browse_test_list_empty);
        mListViewBrowseContacts = (ListView) findViewById(R.id.pbap_browse_listview_contacts_list);
        mListViewBrowseContacts.setOnItemClickListener(mOnBrowseItemClickListener);

        mBrowseProgressBar.setVisibility(View.GONE);
        mListViewBrowseContacts.setVisibility(View.GONE);
    }

    private void prepareUserInterfaceVcard() {
        mVcardSpinner = (Spinner) findViewById(R.id.pbap_vcard_spinner);
        mVcardSpinner.setSelection(DEFAULT_PHONEBOOK_POSITION, true);
        mVcardView = (VcardView) findViewById(R.id.pbap_vcard_vcardview);

        mEditTextHandleValue = (EditText) findViewById(R.id.pbap_vcard_header);
        mEditTextHandleValue.setText(mVcardHandleValue);
    }

    private void prepareActionBar() {
        mActionBar = getActionBar();

        if (mActionBar != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mDownloadTab = mActionBar.newTab().setText(R.string.download);
            mBrowseTab = mActionBar.newTab().setText(R.string.browse);
            mVcardTab = mActionBar.newTab().setText(R.string.vcard);

            mViewFlipper = (ViewFlipper) findViewById(R.id.pbap_test_viewflipper);

            mDownloadTab.setTabListener(new TabListener() {
                @Override
                public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }

                @Override
                public void onTabSelected(Tab tab, FragmentTransaction ft) {
                    mViewFlipper.setDisplayedChild(0);
                }

                @Override
                public void onTabReselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }
            });

            mBrowseTab.setTabListener(new TabListener() {
                @Override
                public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }

                @Override
                public void onTabSelected(Tab tab, FragmentTransaction ft) {
                    mViewFlipper.setDisplayedChild(1);
                }

                @Override
                public void onTabReselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }
            });

            mVcardTab.setTabListener(new TabListener() {

                @Override
                public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }

                @Override
                public void onTabSelected(Tab tab, FragmentTransaction ft) {
                    mViewFlipper.setDisplayedChild(2);
                }

                @Override
                public void onTabReselected(Tab tab, FragmentTransaction ft) {
                    /* NoP */
                }
            });

            mActionBar.addTab(mDownloadTab);
            mActionBar.addTab(mBrowseTab);
            mActionBar.addTab(mVcardTab);
        }
    }

    private void runPbapTestDownload() {
        Logger.d(TAG, "runPbapTestDownload()");

        mDownloadValueCardType = getVcardFormat();
        mDownloadValueMaxCount = getIntValue(mEditTextDownloadMaxListCount, MAX_COUNT_DEFAULT_VALUE);
        mDownloadValueOffset = getIntValue(mEditTextDownloadOffsetValue, OFFSET_DEFAULT_VALUE);

        Logger.d(TAG, "runPbapTestDownload offset: " + mDownloadValueOffset +
                ", maxCount: " + mDownloadValueMaxCount);

        // Refresh UI EditTexts value if some are blank after edit.
        mEditTextDownloadMaxListCount.setText(String.valueOf(mDownloadValueMaxCount));
        mEditTextDownloadOffsetValue.setText(String.valueOf(mDownloadValueOffset));

        String pbName = mDownloadSpinner.getSelectedItem().toString();

        try {
            setPhoneBookRoot();

            if (pullPhoneBook(pbName, mDownloadValueFilter, mDownloadValueOffset,
                    mDownloadValueMaxCount)) {
                startProgressBarDownload();
            }
        } catch (IllegalArgumentException e) {
            showToastLong("PullPhoneBook FAILED: illegal arguments (" + e.getMessage() + ")");
        }
    }

    private void startProgressBarDownload() {
        mDownloadProgressBar.setVisibility(View.VISIBLE);
        mListViewDownloadContacts.setVisibility(View.GONE);
        mTextViewDownloadNothingFound.setVisibility(View.GONE);
    }

    private void stopProgressBarDownload() {
        mDownloadProgressBar.setVisibility(View.GONE);
        if (mPhonebookSize <= 0) {
            mTextViewDownloadNothingFound.setVisibility(View.VISIBLE);
            mListViewDownloadContacts.setVisibility(View.GONE);
        } else {
            mTextViewDownloadNothingFound.setVisibility(View.GONE);
            mListViewDownloadContacts.setVisibility(View.VISIBLE);
        }
    }

    private void runPbapTestBrowse() {
        Logger.d(TAG, "runPbapTestBrowse()");

        String path = getFolder(mBrowseSpinner);
        byte order = mBrowseValueOrder;
        String searchValue = mEditTextSearchValue.getText().toString();
        mBrowseValueMaxCount = getIntValue(mEditTextBrowseMaxListCount, MAX_COUNT_DEFAULT_VALUE);
        mBrowseValueOffset = getIntValue(mEditTextBrowseOffsetValue, OFFSET_DEFAULT_VALUE);

        // Refresh UI EditTexts value if some are blank after edit.
        mEditTextBrowseMaxListCount.setText(String.valueOf(mBrowseValueMaxCount));
        mEditTextBrowseOffsetValue.setText(String.valueOf(mBrowseValueOffset));

        try {
            setPhoneBookRoot();

            boolean started = pullVcardListing(path, order, mBrowseValueSearchAttr,
                searchValue, mBrowseValueMaxCount, mBrowseValueOffset);

            if (started) {
                startProgressBarBrowse();
            } else {
                showToastLong("PullvCardListing FAILED");
            }
        } catch (IllegalArgumentException e) {
            showToastLong("PullvCardListing FAILED: illegal arguments (" + e.getMessage() + ")");
        }
    }

    private void startProgressBarBrowse() {
        mBrowseProgressBar.setVisibility(View.VISIBLE);
        mListViewBrowseContacts.setVisibility(View.GONE);
        mTextViewBrowseNothingFound.setVisibility(View.GONE);
    }

    private void stopProgressBarBrowse() {
        mBrowseProgressBar.setVisibility(View.GONE);
        if (mPhonebookSize <= 0) {
            mTextViewBrowseNothingFound.setVisibility(View.VISIBLE);
            mListViewBrowseContacts.setVisibility(View.GONE);
        } else {
            mTextViewBrowseNothingFound.setVisibility(View.GONE);
            mListViewBrowseContacts.setVisibility(View.VISIBLE);
        }
    }

    class VCardEntryAdapter extends ArrayAdapter<VCardEntry> {
        VCardEntryAdapter() {
            super(PbapTestActivity.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            boolean defaultPhoto = true;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.vcard_row, null);
            }

            VCardEntry tmp = getItem(position);

            ((TextView) row.findViewById(R.id.vcard_title)).setText(tmp.getDisplayName());

            if (tmp.getPhotoList() != null && tmp.getPhotoList().size() > 0) {
                byte[] data = tmp.getPhotoList().get(0).getBytes();
                try {
                    if (data != null) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                        ((ImageView) row.findViewById(R.id.vcard_photo)).setImageBitmap(bmp);
                        defaultPhoto = false;
                    }
                } catch (IllegalArgumentException e) {
                }
            }

            if (defaultPhoto) {
                ((ImageView) row.findViewById(R.id.vcard_photo))
                        .setImageResource(R.drawable.ic_contact_picture);
            }

            return row;
        }
    }

    class BluetoothPbapCardAdapter extends ArrayAdapter<VCardEntry> {
        BluetoothPbapCardAdapter() {
            super(PbapTestActivity.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.vcard_row, null);
            }

            VCardEntry tmp = getItem(position);
            NameData nd = tmp.getNameData();

            StringBuilder sb = new StringBuilder();

            if (nd.getGiven() != null) {
                sb.append(nd.getGiven()).append(" ");
            }
            sb.append(nd.getFamily());

            ((TextView) row.findViewById(R.id.vcard_title)).setText(sb.toString());
            return row;
        }
    }

    class BluetoothPbapVcardListingAdapter extends ArrayAdapter<String> {
        BluetoothPbapVcardListingAdapter() {
            super(PbapTestActivity.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.vcard_row, null);
            }

            String vcardString = getItem(position);

            ((TextView) row.findViewById(R.id.vcard_title)).setText(vcardString);
            return row;
        }
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        Logger.d(TAG, "onDeviceChanged device: " + device);
        mDevice = device;
    }

    @Override
    public void onDeviceDisconected() {
        Logger.e(TAG, "BT device disconnected!");
        mDevice = null;
    }

    public void onClick_download_getsize(View v) {
        Logger.d(TAG, "onClick_download_getsize");
        setPhoneBookRoot();
        String path = mDownloadSpinner.getSelectedItem().toString();
        pullPhoneBookSize(path);
    }

    public void onClick_browse_getsize(View v) {
        Logger.d(TAG, "onClick_browse_getsize");
        setPhoneBookRoot();
        String path = getFolder(mBrowseSpinner);
        pullVcardListingSize(path);
    }

    public void onClickVcardFilterAttributes(View v) {
        Logger.d(TAG, "onClickVcardFilterAttributes");
        Intent intent = new Intent(PbapTestActivity.this, VcardFilterActivity.class);
        intent.putExtra("filter", mVcardValueFilter);
        intent.putExtra("type", mVcardValueCardType);
        startActivityForResult(intent, REQUEST_CODE_GET_FILTER_FOR_VCARD_TAB);
    }

    public void onClickRadioButtonVcardType21(View v) {
        mVcardValueCardType = PbapProfile.VCARD_TYPE_21;
    }

    public void onClickRadioButtonVcardType30(View v) {
        mVcardValueCardType = PbapProfile.VCARD_TYPE_30;
    }

    public void onClick_setPhonebook(View v) {
        int id = getResources().getIdentifier(v.getTag().toString(), "id", getPackageName());

        Spinner spinner = (Spinner) findViewById(id);

        mSetPhoneBookButtonClicked = true;

        if (spinner != null) {
            String folder = getFolder(spinner);
            setPhoneBook(folder);
        }
    }

    public void onClickVcardButtonGet(View v) {
        String handle = mEditTextHandleValue.getText().toString();

        if ((handle == null) || handle.isEmpty()) {
            handle = HANDLE_DEFAULT_VALUE;
            mEditTextHandleValue.setText(handle);
        }

        mVcardHandleValue = handle + VCF_SUFFIX;

        pullVcardEntry(mVcardHandleValue, mVcardValueFilter, mVcardValueCardType);
    }

    public void onClick_abort(View v) {
        mPhonebookSize = 0;

        stopProgressBarDownload();        

        /*
         * Going to call abort if any pending request is ongoing,
         * checks for the same are handled internally
         */
        abort();
    }

    private void storeResponse(int phonebookSize, int missedCalls) {
        mPhonebookSize = phonebookSize;
        mNewMissedCalls = missedCalls;
    }

    private void cleanResponse() {
        mPhonebookSize = 0;
        mNewMissedCalls = 0;
    }

    private String getContactInfo(String vcard) {
        String nullStr = "null";
        String familyPatten = " family: ";
        String givenPatten = " given: ";
        String middlePatten = " middle: ";
        String numberPatten = " number: ";
        String typePatten = " type: ";
        StringBuilder sb = new StringBuilder();

        if (vcard == null) {
            return null;
        }

        // FamilyName
        String familyName = vcard.substring(
            vcard.indexOf(familyPatten) + familyPatten.length(),
            vcard.indexOf(givenPatten));
        Logger.d(TAG, "familyName: " + familyName);
        if ((familyName != null) && !familyName.equals(nullStr)) {
            sb.append(familyName).append(" ");
        }

        // GivenName
        String givenName = vcard.substring(
            vcard.indexOf(givenPatten) + givenPatten.length(),
            vcard.indexOf(middlePatten));
        Logger.d(TAG, "givenName: " + givenName);
        if ((givenName != null) && !givenName.equals(nullStr)) {
            sb.append(givenName).append(" ");
        }

        // TEL (only get the 1st)

        String tel = vcard.substring(
            vcard.indexOf(numberPatten) + numberPatten.length(),
            vcard.indexOf(typePatten));
        Logger.d(TAG, "tel: " + tel);
        if ((tel != null) && !tel.equals(nullStr)) {
            sb.append(tel).append(" ");
        }

        return sb.toString();
    }

    private VCardEntry createVcardEntry(String vcard) {
        VCardEntry vcardEntry = new VCardEntry();
        String nullStr = "null";
        String familyPatten = " family: ";
        String givenPatten = " given: ";
        String middlePatten = " middle: ";
        String numberPatten = " number: ";
        String typePatten = " type: ";

        if (vcard == null) {
            return null;
        }

        // FamilyName
        String familyName = vcard.substring(
            vcard.indexOf(familyPatten) + familyPatten.length(),
            vcard.indexOf(givenPatten));
        Logger.d(TAG, "familyName: " + familyName);
        if ((familyName != null) && !familyName.equals(nullStr)) {
            VCardProperty nProp = new VCardProperty();
            nProp.setName(VCardConstants.PROPERTY_N);
            nProp.addValues(familyName);
            vcardEntry.addProperty(nProp);
        }

        // GivenName
        String givenName = vcard.substring(
            vcard.indexOf(givenPatten) + givenPatten.length(),
            vcard.indexOf(middlePatten));
        Logger.d(TAG, "givenName: " + givenName);

        // TEL (only get the 1st)

        String tel = vcard.substring(
            vcard.indexOf(numberPatten) + numberPatten.length(),
            vcard.indexOf(typePatten));
        Logger.d(TAG, "tel: " + tel);
        if ((tel != null) && !tel.equals(nullStr)) {
            VCardProperty telProp = new VCardProperty();
            telProp.setName(VCardConstants.PROPERTY_TEL);
            telProp.addValues(tel);
            vcardEntry.addProperty(telProp);
        }

        return vcardEntry;
    }

    private String getFolder(Spinner spinner) {
        if (spinner != null) {
            String pbName = spinner.getSelectedItem().toString();
            Logger.d(TAG, "getFolder pbName: " + pbName);
            // E.g. "telecom/pb.vcf" -> "telecom/pb"
            String[] folders = pbName.split("\\.");
            return (folders != null) ? folders[0] : null;
        } else {
            return null;
        }
    }

    private byte getVcardFormat() {
        byte format = 0;
        int btnId = mRadioGroupFormat.getCheckedRadioButtonId();

        switch (btnId) {
            case R.id.pbap_download_vcard30:
                format = PbapProfile.VCARD_TYPE_30;
                break;

            case R.id.pbap_download_vcard21:
            default:
                format = PbapProfile.VCARD_TYPE_21;
                break;
        }

        return format;
    }

    private void handleActionConnectionStateChanged(Intent intent) {
        int currState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                BluetoothProfile.STATE_DISCONNECTED);
        BluetoothDevice device = (BluetoothDevice) intent.getExtra(BluetoothDevice.EXTRA_DEVICE);
        Logger.d(TAG, "handleActionConnectionStateChanged device: " + device + ", currState: " + currState);

        mDevice = device;

        if (currState == BluetoothProfile.STATE_CONNECTED) {
            Logger.d(TAG, "PBAP connected");
            mHandler.sendEmptyMessage(MSG_CONNECTED);
        } else if (currState == BluetoothProfile.STATE_DISCONNECTED) {
            Logger.d(TAG, "PBAP disconnected");
            mHandler.sendEmptyMessage(MSG_DISCONNECTED);
        }
    }

    private void handlePhonebookDownloadStateChanged(Intent intent) {
        Logger.d(TAG, "handlePhonebookDownloadStateChanged");

        String pbName = intent.getStringExtra(BluetoothPbapClient.EXTRA_PHONEBOOK_PATH);
        int state = intent.getIntExtra(BluetoothPbapClient.EXTRA_DOWNLOAD_STATE,
                BluetoothPbapClient.DOWNLOAD_FAILED);
        int result = intent.getIntExtra(BluetoothPbapClient.EXTRA_DOWNLOAD_RESULT,
                BluetoothPbapClient.RESULT_FAILURE);

        mHandler.sendMessage(mHandler.obtainMessage(MSG_PULL_PHONEBOOK_RESP, state, result, pbName));
    }

    private void processDisconnected() {
        Logger.d(TAG, "processDisconnected");
        mPbapServiceCallback.onSessionDisconnected();
    }

    private void processConnected() {
        Logger.d(TAG, "processConnected");
        mPbapServiceCallback.onSessionConnected();
    }

    private void processPullPhonebookResp(String pbName, int state, int result) {
        if (state == BluetoothPbapClient.DOWNLOAD_COMPLETED) {
            Log.i(TAG, "processPullPhonebookResp download completed, pbName: " +
                    pbName + ", phonebook size: " + result);
            mPbapServiceCallback.onPullPhoneBookDone(pbName, result, 0);
        } else if (state == BluetoothPbapClient.DOWNLOAD_FAILED) {
            Logger.e(TAG, "processPullPhonebookResp download failed, pbName: " +
                    pbName + ", error: " + result);
            mPbapServiceCallback.onPullPhoneBookError(pbName, BluetoothPbapClient.RESULT_FAILURE);
        } else if (state == BluetoothPbapClient.DOWNLOAD_IN_PROGRESS) {
            Logger.d(TAG, "processPullPhonebookResp download in-progress, pbName: " + pbName);
        }
    }

    private void processPullVcardListingResp(int result, Bundle extras) {
        int phonebookSize = extras.getInt(PbapProfile.KEY_PHONEBOOK_SIZE);
        int newMissedCalls = extras.getInt(PbapProfile.KEY_NEW_MISSED_CALLS);
        ArrayList<String> vcardListing = extras.getStringArrayList(PbapProfile.KEY_VCARD_LISTING);

        Logger.d(TAG, "processPullVcardListingResp");

        if (PbapProfile.isSuccess(result)) {
            mPbapServiceCallback.onPullVcardListingDone(vcardListing, phonebookSize, newMissedCalls);
        } else {
            mPbapServiceCallback.onPullVcardListingError(result);
        }
    }

    private void processPullVcardEntryResp(int result, Bundle extras) {
        Logger.d(TAG, "processPullVcardEntryResp");
        String vcard = extras.getString(PbapProfile.KEY_VCARD_ENTRY);
        if (PbapProfile.isSuccess(result) && (vcard != null)) {
            mPbapServiceCallback.onPullVcardEntryDone(vcard);
        } else {
            mPbapServiceCallback.onPullVcardEntryError(result);
        }
    }

    private void processSetPhonebookResp(int result, Bundle extras) {
        Logger.d(TAG, "processSetPhonebookResp");
        if (PbapProfile.isSuccess(result)) {
            mPbapServiceCallback.onSetPhoneBookDone();
        } else {
            mPbapServiceCallback.onSetPhoneBookError(result);
        }
    }

    private void processAbortResp(int result, Bundle extras) {
        Logger.d(TAG, "processAbortResp");
        if (PbapProfile.isSuccess(result)) {
            mPbapServiceCallback.onAbortDone();
        } else {
            mPbapServiceCallback.onAbortError(result);
        }
    }

    private int getMessage(String cmd) {
        if (cmd == null) {
            return MSG_UNKNOWN;
        }

        if (cmd.equals(PbapProfile.CUSTOM_ACTION_PULL_PHONEBOOK)) {
            return MSG_PULL_PHONEBOOK_RESP;
        } else if (cmd.equals(PbapProfile.CUSTOM_ACTION_PULL_VCARD_LISTING)) {
            return MSG_PULL_VCARD_LISTING_RESP;
        } else if (cmd.equals(PbapProfile.CUSTOM_ACTION_PULL_VCARD_ENTRY)) {
            return MSG_PULL_VCARD_ENTRY_RESP;
        } else if (cmd.equals(PbapProfile.CUSTOM_ACTION_SET_PHONEBOOK)) {
            return MSG_SET_PHONEBOOK_RESP;
        } else if (cmd.equals(PbapProfile.CUSTOM_ACTION_ABORT)) {
            return MSG_ABORT_RESP;
        } else {
            return MSG_UNKNOWN;
        }
    }

    private void connect() {
        Logger.d(TAG, "connect");
        if (!verifyPbapDevice()) {
            return;
        }

        mPbap.connect(mDevice);
    }

    private void disconnect() {
        Logger.d(TAG, "disconnect");
        if (!verifyPbapDevice()) {
            return;
        }

        mPbap.disconnect(mDevice);
    }

    private int getConnectionState() {
        Logger.d(TAG, "getConnectionState");
        if (!verifyPbapDevice()) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }

        return mPbap.getConnectionState(mDevice);
    }

    private boolean pullPhoneBook(String pbName, long filter,
            int listStartOffset, int maxListCount) {
        Logger.d(TAG, "pullPhoneBook pbName: " + pbName +
                ", filter: " + Long.toHexString(filter) +
                ", listStartOffset: " + listStartOffset +
                ", maxListCount: " + maxListCount);

        if (!verifyPbapDevice()) {
            return false;
        }

        try {
            return mPbap.pullPhoneBook(mDevice, pbName,
                    filter, listStartOffset, maxListCount);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
    }

    private void pullPhoneBookSize(String path) {
        Logger.d(TAG, "pullPhoneBookSize path: " + path);
        // Get phonebook size with MaxListCount = 0
        pullPhoneBook(path, PbapProfile.PBAP_REQUESTED_FIELDS, 0, 0);
    }

    private boolean pullVcardListing(String path, byte order, byte searchProp, String searchValue,
            int maxListCount, int listStartOffset) {
        Logger.d(TAG, "pullVcardListing path: " + path + ", order: " + order +
            ", searchProp" + searchProp + ", searchValue: " + searchValue +
            ", maxListCount: " + maxListCount + ", listStartOffset" + listStartOffset);
        if (!verifyPbapDevice()) {
            return false;
        }

        try {
            return mPbap.pullVcardListing(mDevice, path, order,
                    searchProp, searchValue, maxListCount, listStartOffset);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
    }

    private void pullVcardListingSize(String path) {
        Logger.d(TAG, "pullVcardListingSize path: " + path);
        // Get vCard listing size with MaxListCount = 0
        pullVcardListing(path, PbapProfile.ORDER_INDEXED,
                PbapProfile.SEARCH_ATTR_NAME, null, 0, 0);
    }

    private void pullVcardEntry(String vcardHandle, long filter, byte format) {
        Logger.d(TAG, "pullVcardEntry vcardHandle: " + vcardHandle +
            ", filter: " + filter + ", format: " + format);
        if (!verifyPbapDevice()) {
            return;
        }

        try {
            mPbap.pullVcardEntry(mDevice, vcardHandle, filter, format);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void pullVcardEntry(String vcardHandle) {
        Logger.d(TAG, "pullVcardEntry vcardHandle: " + vcardHandle);
        pullVcardEntry(vcardHandle, PbapProfile.PBAP_REQUESTED_FIELDS,
            PbapProfile.VCARD_TYPE_30);
    }

    private void setPhoneBook(String folder) {
       Logger.d(TAG, "setPhoneBook folder: " + folder);
       if (!verifyPbapDevice()) {
            return;
        }

        try {
            mPbap.setPhoneBook(mDevice, folder);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void setPhoneBookRoot() {
        Logger.d(TAG, "setPhoneBookRoot");
        setPhoneBook(PbapProfile.ROOT_PATH);
    }

    private void abort() {
        Logger.d(TAG, "abort");
        if (!verifyPbapDevice()) {
            return;
        }

        try {
            mPbap.abort(mDevice);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private boolean verifyPbapDevice() {
        if (mPbap == null) {
            Logger.e(TAG, " PBAP client not connected ");
            return false;
        }

        if (mDevice == null) {
            Logger.e(TAG, " Bluetooth device null ");
            return false;
        }

        return true;
    }

    private int getIntValue(EditText editText, int defaultValue) {
        int value = defaultValue;

        if (editText == null) {
            return defaultValue;
        }

        try {
            value = Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException e) {
            Logger.e(TAG, "Can't parse int, use default " + defaultValue);
        }
        return value;
    }

    private void showResult(String msg, int result) {
        StringBuilder sb = new StringBuilder();
        String resultString;

        switch (result) {
            case BluetoothPbapClient.RESULT_SUCCESS:
                resultString = "success";
                break;
            case BluetoothPbapClient.RESULT_CANCELED:
                resultString = "cancelled";
                break;
            case BluetoothPbapClient.RESULT_INVALID_PARAMETER:
                resultString = "invalid parameter";
                break;
            case BluetoothPbapClient.RESULT_FAILURE:
            // pass-through
            default:
                resultString = "fail";
                break;
        }

        sb.append(msg).append(" ").append(resultString);

        showToastLong(sb.toString());
    }

    private void showToastShort(String msg) {
        Logger.d(TAG, msg);
        Toast.makeText(PbapTestActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showToastLong(String msg) {
        Logger.d(TAG, msg);
        Toast.makeText(PbapTestActivity.this, msg, Toast.LENGTH_LONG).show();
    }
}
