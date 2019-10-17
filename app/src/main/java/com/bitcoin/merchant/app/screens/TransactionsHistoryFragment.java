package com.bitcoin.merchant.app.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.database.DBControllerV3;
import com.bitcoin.merchant.app.database.PaymentRecord;
import com.bitcoin.merchant.app.util.DateUtil;
import com.bitcoin.merchant.app.util.MonetaryUtil;
import com.bitcoin.merchant.app.util.PrefsUtil;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.Context.CLIPBOARD_SERVICE;

public class TransactionsHistoryFragment extends Fragment {
    private static final String TAG = "TransactionsHistory";
    private TransactionAdapter adapter = null;
    private ListView listView = null;
    private LinearLayout noTxHistoryLv = null;
    private SwipeRefreshLayout swipeLayout = null;
    private Activity thisActivity = null;
    private volatile boolean ready;
    private final BroadcastReceiver fragmentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MainActivity.ACTION_QUERY_ALL_UXTO_FINISHED.equals(intent.getAction())) {
                swipeLayout.setRefreshing(false);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ready = true;
        View rootView = inflater.inflate(getResources().getLayout(R.layout.fragment_transaction), container, false);
        initListView(rootView);
        swipeLayout = rootView.findViewById(R.id.swipe_container);
        swipeLayout.setProgressViewEndTarget(false, (int) (getResources().getDisplayMetrics().density * (72 + 20)));
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new LoadTxFromDatabaseTask(true).execute();
            }
        });
        swipeLayout.setColorSchemeResources(
                R.color.bitcoindotcom_darker_green,
                R.color.bitcoindotcom_green,
                R.color.bitcoindotcom_darkest_green);
        thisActivity = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_QUERY_ALL_UXTO_FINISHED);
        LocalBroadcastManager.getInstance(thisActivity).registerReceiver(fragmentBroadcastReceiver, filter);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        ready = false;
        super.onDestroyView();
        LocalBroadcastManager.getInstance(thisActivity).unregisterReceiver(fragmentBroadcastReceiver);
    }

    private void initListView(View rootView) {
        adapter = new TransactionAdapter();
        listView = rootView.findViewById(R.id.txList);
        noTxHistoryLv = rootView.findViewById(R.id.no_tx_history_lv);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showTransactionMenu(id);
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (listView.getChildAt(0) != null) {
                    swipeLayout.setEnabled(listView.getFirstVisiblePosition() == 0 && listView.getChildAt(0).getTop() == 0);
                }
            }
        });
        if (adapter.getCount() == 0) {
            listView.setVisibility(View.GONE);
            noTxHistoryLv.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            noTxHistoryLv.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new LoadTxFromDatabaseTask(false).execute();
    }

    private void showTransactionMenu(final long item) {
        final ContentValues val = adapter.mListItems.get((int) item);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String tx = val.getAsString("tx");
        final String address = val.getAsString("iad");
        builder.setTitle(tx);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setItems(new CharSequence[]{
                        "View transaction",
                        "View all TX with this address",
                        "Copy transaction",
                        "Copy address",
                },
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0: {
                                openExplorer("https://explorer.bitcoin.com/bch/tx/" + tx);
                                break;
                            }
                            case 1: {
                                openExplorer("https://explorer.bitcoin.com/bch/address/" + address);
                                break;
                            }
                            case 2: {
                                copyToClipboard(tx);
                                break;
                            }
                            case 3: {
                                copyToClipboard(address);
                                break;
                            }
                        }
                    }
                });
        builder.create().show();
    }

    private void openExplorer(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    private void copyToClipboard(String text) {
        final ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            ClipData clipData = ClipData.newPlainText("Source Text", text);
            clipboardManager.setPrimaryClip(clipData);
        }
    }

    protected boolean isSafe() {
        if (!ready || thisActivity == null || adapter == null) {
            return false; // view not yet created
        }
        return !(this.isRemoving() || this.getActivity() == null || this.isDetached() || !this.isAdded() || this.getView() == null);
    }

    public void addTx(final ContentValues val) {
        try {
            if (!isSafe()) {
                return;
            }
            listView.setVisibility(View.VISIBLE);
            noTxHistoryLv.setVisibility(View.GONE);
            thisActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isSafe()) {
                        return;
                    }
                    adapter.addNewPayment(val);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "addTx", e);
            Crashlytics.logException(e);
        }
    }

    public void updateTx(final ContentValues val) {
        try {
            if (!isSafe()) {
                return;
            }
            listView.setVisibility(View.VISIBLE);
            noTxHistoryLv.setVisibility(View.GONE);
            thisActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new LoadTxFromDatabaseTask(false).execute();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updateTx", e);
            Crashlytics.logException(e);
        }
    }

    private class LoadTxFromDatabaseTask extends AsyncTask<Void, Void, ArrayList<ContentValues>> {
        private boolean queryServer;

        public LoadTxFromDatabaseTask(boolean queryServer) {
            this.queryServer = queryServer;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeLayout.setRefreshing(true);
        }

        @Override
        protected ArrayList<ContentValues> doInBackground(Void... params) {
            if (!ready) {
                return null;
            }
            String address = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
            if ((address != null) && (address.length() > 0)) {
                try {
                    return new DBControllerV3(getActivity()).getAllPayments();
                } catch (Exception e) {
                    Log.e(TAG, "getAllPayments", e);
                    Crashlytics.logException(e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<ContentValues> result) {
            super.onPostExecute(result);
            if (ready) {
                if (result != null && adapter != null) {
                    adapter.reset(result);
                    if (result.size() != 0) {
                        listView.setVisibility(View.VISIBLE);
                        noTxHistoryLv.setVisibility(View.GONE);
                    }
                }
                if (queryServer) {
                    findAllPotentialMissingTx();
                } else {
                    if (result != null && result.size() != 0) {
                        listView.setVisibility(View.VISIBLE);
                        noTxHistoryLv.setVisibility(View.GONE);
                    }
                    swipeLayout.setRefreshing(false);
                }
            }
        }
    }

    private void findAllPotentialMissingTx() {
        LocalBroadcastManager.getInstance(thisActivity).sendBroadcast(new Intent(MainActivity.ACTION_QUERY_ALL_UXTO));
    }

    private class TransactionAdapter extends BaseAdapter {
        private final List<ContentValues> mListItems = new ArrayList<>();
        private final LayoutInflater inflater;

        TransactionAdapter() {
            inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addNewPayment(ContentValues val) {
            for (ContentValues c : mListItems) {
                if (val.getAsString("tx").equals(c.getAsString("tx"))) {
                    return; // already added
                }
            }
            mListItems.add(0, val);
            notifyDataSetChanged();
        }

        public void reset(ArrayList<ContentValues> vals) {
            mListItems.clear();
            mListItems.addAll(vals);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mListItems.size();
        }

        @Override
        public String getItem(int position) {
            return mListItems.get(position).getAsString("iad");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : inflater.inflate(R.layout.list_item_transaction, parent, false);
            try {
                ContentValues vals = mListItems.get(position);
                PaymentRecord r = new PaymentRecord(vals);
                setupView(view, r.bchAmount, r.fiatAmount, r.timeInSec, r.confirmations);
            } catch (Exception e) {
                Log.e(TAG, "getView", e);
            }
            return view;
        }

        private void setupView(View view, Long bch, String fiat, long timeInSec, int confirmations) {
            String date = DateUtil.getInstance().format(timeInSec);
            SpannableStringBuilder ds = new SpannableStringBuilder(date);
            int idx = date.indexOf("@");
            if (idx != -1) {
                ds.setSpan(new StyleSpan(Typeface.NORMAL), 0, idx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ds.setSpan(new RelativeSizeSpan(0.75f), idx, date.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            TextView tvDate = view.findViewById(R.id.tv_date);
            tvDate.setText(ds);
            tvDate.setAlpha(0.7f);
            // display coin amount
            long amount = Math.abs(bch);
            SpannableStringBuilder coinSpan = new SpannableStringBuilder(thisActivity.getResources().getString(R.string.bitcoin_currency_symbol));
            coinSpan.setSpan(new RelativeSizeSpan((float) 0.75), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView coinView = view.findViewById(R.id.tv_amount_coin);
            String displayValue = MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(amount);
            coinView.setText(displayValue + " " + coinSpan);
            // display fiat amount
            TextView fiatView = view.findViewById(R.id.tv_amount_fiat);
            if (fiat != null && fiat.length() > 1) {
                SpannableStringBuilder fiatSpan = new SpannableStringBuilder(fiat.subSequence(0, 1));
                fiatSpan.setSpan(new RelativeSizeSpan((float) 0.75), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                fiatView.setText(fiatSpan + " " + fiat.substring(1));
            } else {
                fiatView.setText("");
            }
            float secs = System.currentTimeMillis() / 1000f;
            if ((secs - timeInSec) > TimeUnit.HOURS.toSeconds(3)) {
                confirmations = 2;
            }
            ImageView ivStatus = view.findViewById(R.id.iv_status);
            ivStatus.setImageResource(getIcon(bch, confirmations));
            ivStatus.setAlpha(getAlpha(bch, confirmations));
        }

        private int getIcon(Long bch, int confirmations) {
            if (bch < 0L) { // under/over payment
                return R.drawable.ic_warning_black_18dp;
            } else if (confirmations <= 0) {
                return R.drawable.ic_done_white_24dp;
            } else if (confirmations == 1) {
                return R.drawable.ic_done_white_24dp;
            } else { // 2 or more confirmations
                return R.drawable.ic_doublecheck_white_24dp;
            }
        }

        private float getAlpha(Long bch, int confirmations) {
            if (bch < 0L) { // under/over payment
                return 1.0f;
            } else if (confirmations <= 0) {
                return 0f;
            } else {
                return 1.0f;
            }
        }
    }
}
