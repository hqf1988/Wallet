package com.example.leon.hmwallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.google.common.collect.ImmutableList;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import java.security.SecureRandom;
import java.util.List;

public class MnemonicActivity extends AppCompatActivity {

    private List<String> words;

    private EditText mnemonicEdit;


    private EditText mAddressEdit;


    //m / 44' / 60' / 0' / 0
    //Hardened意思就是派生加固，防止获取到一个子私钥之后可以派生出后面的子私钥
    //必须还有上一级的父私钥才能派生
    public static final ImmutableList<ChildNumber> BIP44_ETH_ACCOUNT_ZERO_PATH =
            ImmutableList.of(new ChildNumber(44, true), new ChildNumber(60, true),
                    ChildNumber.ZERO_HARDENED, ChildNumber.ZERO);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mnemonic);

        mnemonicEdit = findViewById(R.id.mnemonic);
        mAddressEdit = findViewById(R.id.address);
    }

    public void onCreateMnemonic(View view) {
//        MnemonicUtils.generateMnemonic()
        //使用BitcoinJ
        //创建entropy
        SecureRandom random = new SecureRandom();
        //entropy 128bit = （128 / 8） 字节
        byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
        random.nextBytes(entropy);
        try {
            words = MnemonicCode.INSTANCE.toMnemonic(entropy);
            mnemonicEdit.setText(words.toString());
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }
    }

    public void onCreateWallet(View view) {
        //1. 通过助记词创建seed
        byte[] seed = MnemonicCode.toSeed(words, "");
        //2. 通过种子派生主私钥
        DeterministicKey rootKey = HDKeyDerivation.createMasterPrivateKey(seed);

        //3. 通过主私钥，派生出第一个地址
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(rootKey);
        //m/44'/60'/0'/0/0
        //parent path: m/44'/60'/0'/0/
        //child number 0
        DeterministicKey deterministicKey = hierarchy.deriveChild(BIP44_ETH_ACCOUNT_ZERO_PATH, false, true, new ChildNumber(0));
        //派生出来的第一个地址对应的私钥
        byte[] privKeyBytes = deterministicKey.getPrivKeyBytes();
        ECKeyPair ecKeyPair = ECKeyPair.create(privKeyBytes);
        try {
            //创建keysotore = 使用用户输入的密码加密子私钥
            WalletFile walletFile = Wallet.createLight("a1234567", ecKeyPair);
            mAddressEdit.setText("0x" + walletFile.getAddress());

            //将walletFile 序列化存储文件当中
        } catch (CipherException e) {
            e.printStackTrace();
        }

    }
}
