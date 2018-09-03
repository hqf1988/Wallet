package com.example.leon.hmwallet;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAddress = findViewById(R.id.address);
    }

    public void onStartBitcoinWallet(View view) {
        //点击调转到比特币钱包界面
        Intent intent = new Intent(this, BitcoinActivity.class);
        startActivity(intent);
    }

    public void onStartETHWallet(View view) {
        //跳转到以太坊的钱包
        Intent intent = new Intent(this, EthWalletActivity.class);
        startActivity(intent);
    }
}
