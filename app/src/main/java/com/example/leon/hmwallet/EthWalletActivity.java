package com.example.leon.hmwallet;

import android.content.Intent;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rx.functions.FuncN;

public class EthWalletActivity extends AppCompatActivity {
    private static final String TAG = "EthWalletActivity";

    private String address;

    private EditText mAddressEdit;
    private WalletFile wallet;

    private TextView mEthBalance;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String CONTRACT_ADDRESS = "0xaac1a52900b8651c9e1e2972d8e4c80cab2ce875";

    private Web3j mWeb3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/1UoO4I/"));

    private EditText mToAddress;
    private EditText mAmount;

    private EditText mToTokenAddress;
    private EditText mTokenAmount;

    private TextView mTokenBalance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eth_wallet);

        mEthBalance = findViewById(R.id.eth_balance);
        mAddressEdit = findViewById(R.id.address);
        mTokenBalance = findViewById(R.id.token_balance);

        mToTokenAddress = findViewById(R.id.to_token_address);
        mTokenAmount = findViewById(R.id.token_amount);
//        WalletFile wallet = EthWalletManager.getInstance().createWallet(this);
        wallet = EthWalletManager.getInstance().loadWallet(this);
        address = "0x" + wallet.getAddress();
        mAddressEdit.setText(address);

        mToAddress = findViewById(R.id.to_address);
        mAmount = findViewById(R.id.eth_amount);

        updateBalance();
    }

    private void updateBalance() {
        //网络请求， 通过异步任务
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                //获取当前钱包地址的余额
                try {
                    //单位是Wei
                    BigInteger balance = mWeb3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
                    //用户看到的应该是ether
                    //单位的转换
                    final BigDecimal balanceEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);
                    //从子线程切换到主线程更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mEthBalance.setText(balanceEther.toPlainString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //查询当前钱包地址对应MET的余额
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //构建智能合约balanceOf方法
                    Function function = new Function("balanceOf",
                            Collections.singletonList(new Address(address)),
                            Collections.singletonList(new TypeReference<Uint256>() {
                            }));
                    String encode = FunctionEncoder.encode(function);
                    Transaction ethCallTransaction = Transaction.createEthCallTransaction(address, CONTRACT_ADDRESS, encode);
                    //调用智能合约的balanceOf，获取到结果
                    String value = mWeb3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send().getValue();
                    //从结果当中解析MET代币的余额
                    List<Type> decode = FunctionReturnDecoder.decode(value, function.getOutputParameters());
                    Uint256 balance = (Uint256) decode.get(0);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTokenBalance.setText(balance.getValue().toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public void onStartMnemonic(View view) {
        Intent intent = new Intent(this, MnemonicActivity.class);
        startActivity(intent);
    }

    public void onExportKeyStore(View view) {
        //将WalletFile变成json字符串
        try {
            String s = objectMapper.writeValueAsString(wallet);
            Log.d(TAG, "onExportKeyStore: " + s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void onExportPrivateKey(View view) {
        try {
            ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", wallet);
            BigInteger privateKey = ecKeyPair.getPrivateKey();
            //将私钥变成16精致字符串，不需要前缀，左边位数不够补0
            String privateKeyString = Numeric.toHexStringNoPrefixZeroPadded(privateKey, Keys.PRIVATE_KEY_LENGTH_IN_HEX);
            Log.d(TAG, "onExportPrivateKey: " + privateKeyString);
        } catch (CipherException e) {
            e.printStackTrace();
        }
    }

    public void onSendETH(View view) throws CipherException {
        //获取用户的输入
        final String etherAmount = mAmount.getText().toString();
        final String toAddress = mToAddress.getText().toString();
        if (TextUtils.isEmpty(etherAmount) || TextUtils.isEmpty(toAddress)) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                //开始转账
                //创建Transaction
                //获取nonce
                try {
                    BigInteger nonce = mWeb3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().getTransactionCount();
                    BigInteger gasPrice = mWeb3j.ethGasPrice().send().getGasPrice(); //单位为wei
                    BigInteger gasLimit = new BigInteger("200000");
                    //最大油费 = gasPrice * gasLimit
                    //将etherAmount变成wei
                    BigDecimal bigDecimal = Convert.toWei(etherAmount, Convert.Unit.ETHER);
                    RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, bigDecimal.toBigInteger());

                    //walletfile = keystore
                    //将用户输入的密码和keystore 解密出私钥
                    ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", wallet);
                    Credentials credentials = Credentials.create(ecKeyPair);
                    byte[] bytes = TransactionEncoder.signMessage(rawTransaction, credentials);
                    String hexValue = Numeric.toHexString(bytes);
                    String transactionHash = mWeb3j.ethSendRawTransaction(hexValue).send().getTransactionHash();
                    Log.d(TAG, "onSendETH: " + transactionHash);
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public void onSendMET(View view) {
        //发送MET  token
        String toAddress = mToTokenAddress.getText().toString();
        String tokenAmount = mTokenAmount.getText().toString();
        if (TextUtils.isEmpty(toAddress) || TextUtils.isEmpty(tokenAmount)) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //调用智能transfer方法完成代币转账
                    BigInteger tokenAmountInteger = new BigInteger(tokenAmount);
                    Function function = new Function(
                            "transfer",
                            Arrays.asList(new Address(toAddress), new Uint256(tokenAmountInteger)),
                            Collections.singletonList(new TypeReference<Bool>() {
                            }));
                    String encode = FunctionEncoder.encode(function);
                    //涉及到数字资产的转账，需要进行签名，只能创建RawTransaction
                    BigInteger nonce = mWeb3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().getTransactionCount();
                    BigInteger gasPrice = mWeb3j.ethGasPrice().send().getGasPrice(); //单位为wei
                    BigInteger gasLimit = new BigInteger("200000");
                    RawTransaction rawTransaction = RawTransaction
                            .createTransaction(nonce, gasPrice, gasLimit, CONTRACT_ADDRESS, encode);
                    ECKeyPair ecKeyPair = Wallet.decrypt("a12345678", wallet);
                    Credentials credentials = Credentials.create(ecKeyPair);
                    byte[] bytes = TransactionEncoder.signMessage(rawTransaction, credentials);
                    String s = Numeric.toHexString(bytes);
                    String transactionHash = mWeb3j.ethSendRawTransaction(s).send().getTransactionHash();
                    Log.d(TAG, "onSendMET: " + transactionHash);
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
