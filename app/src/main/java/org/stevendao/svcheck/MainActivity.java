package org.stevendao.svcheck;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("CMTA Stored Value");
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);
        IntentFilter techDiscovered = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] intentFilters = new IntentFilter[] {techDiscovered};
        String[][] techLists = new String[][] { new String[] { MifareClassic.class.getName() } };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null && intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);

            try (MifareClassic mifareClassic = MifareClassic.get(tagFromIntent)) {
                if (mifareClassic == null) {
                    return;
                }

                mifareClassic.connect();

                Vibrator vibrator = getSystemService(Vibrator.class);
                vibrator.vibrate(VibrationEffect.createOneShot(
                        250, VibrationEffect.DEFAULT_AMPLITUDE));

                if (mifareClassic.authenticateSectorWithKeyA(1, MifareClassic.KEY_DEFAULT))
                {
                    int sectorOne = mifareClassic.sectorToBlock(1);
                    byte[] blockOne = mifareClassic.readBlock(sectorOne + 1);
                    byte[] blockTwo = mifareClassic.readBlock(sectorOne + 2);

                    int valOne = ((0xFF & blockOne[2]) << 8) | (0xFF & blockOne[3]);
                    int valTwo = ((0xFF & blockTwo[2]) << 8) | (0xFF & blockTwo[3]);
                    double curVal = valOne < valTwo ? valOne / 100.0 : valTwo / 100.0;

                    NumberFormat format = NumberFormat.getCurrencyInstance();
                    format.setMaximumFractionDigits(2);
                    format.setMinimumFractionDigits(2);
                    format.setCurrency(Currency.getInstance("USD"));

                    String dollarAmount = format.format(curVal);

                    TextView textView = findViewById(R.id.textView3);
                    textView.setText(dollarAmount);
                }
            } catch (IOException e) {
//                throw new RuntimeException(e);
            }
        }
    }
}