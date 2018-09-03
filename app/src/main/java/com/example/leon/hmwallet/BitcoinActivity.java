package com.example.leon.hmwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

//比特币钱包界面
public class BitcoinActivity extends AppCompatActivity {

    private EditText mAddressEdit;
    private TextView mBalanceText;

    private EditText mToAddressEdit;
    private EditText mAmountEdit;

    Wallet wallet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_wallet);
        mAddressEdit = findViewById(R.id.address);
        mBalanceText = findViewById(R.id.balance);

        mToAddressEdit = findViewById(R.id.to_address);
        mAmountEdit = findViewById(R.id.amount);

//        Wallet wallet = BitcoinWalletManager.getInstance().createWallet(this);
        wallet = BitcoinWalletManager.getInstance().loadWallet(this);
        //公钥 --> 钱包地址
        //获取钱包收款地址，展示到编辑框内， 创建地址Address对象，对应公钥的Hash
        Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        //address.toString() base58得到比特币的地址
        mAddressEdit.setText(address.toString());

        Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED); //satoshi
        mBalanceText.setText(String.valueOf(balance.value));

        //监听别人转账，如果有人转账，就会通过P2P网络同步到手机，回调onCoinsReceived
        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                //获取余额，但是有些余额可能不能马上使用，还在确认当中
                final Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED); //satoshi
                //刷新ui， 在主线程中刷新
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //展示钱包余额
                        mBalanceText.setText(String.valueOf(balance.value));
                    }
                });
            }
        });

        if (wallet != null) {
            Intent intent = new Intent(this, BlockChainService.class);
            startService(intent);
        }
    }

    public void onSendBitcoin(View view) {
        //检查用户输入
        //获取用户输入转账地址
        String toAddress = mToAddressEdit.getText().toString();
        String amount = mAmountEdit.getText().toString();//以毫比特为单位
        if (TextUtils.isEmpty(toAddress) || TextUtils.isEmpty(amount)) {
            return; //返回，不处理
        }
        //开始转账
        Address to = Address.fromBase58(Constants.NETWORK_PARAMS, toAddress);//构建Address
        //将用户输入金额（mBTC）转换成satoshi
        Coin value = MonetaryFormat.MBTC.parse(amount);
        //创建发送请求
        SendRequest request = SendRequest.to(to, value);
        try {
            //创建Tx，离线，此时没有广播，但会出发钱包的更新
            Transaction transaction = wallet.sendCoinsOffline(request);
            //通过P2P网络进行广播
            BlockChainService.broadcastTransaction(this, transaction);
        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
        }

    }
}
