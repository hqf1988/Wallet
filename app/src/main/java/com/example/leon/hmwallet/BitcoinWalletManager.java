package com.example.leon.hmwallet;

import android.content.Context;

import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

//钱包管理器，完成钱包的创建，加载钱包
public class BitcoinWalletManager {


    Wallet wallet;

    //单例模式
    private static BitcoinWalletManager sInstance; //整个应用只有一个管理器对象

    //私有构造方法，为了不让外面new 对象
    private BitcoinWalletManager() {
    }

    //获取单例
    public static BitcoinWalletManager getInstance() {
        //两个非空一个加锁, 避免多线程调用时，重复创建对象
        if (sInstance == null) {
            synchronized (BitcoinWalletManager.class) {
                if (sInstance == null) {
                    sInstance = new BitcoinWalletManager();
                }
            }
        }
        return sInstance;
    }

    //钱包的创建
    public Wallet createWallet(Context context) {
        try {
            //data/data/com.exmaple.leon.hmwallet/files/
            File file = context.getFileStreamPath("wallet-protobuf");
            //NetworkParams 网络参数，配置是比特币主链还是测试链
            Wallet wallet = new Wallet(Constants.NETWORK_PARAMS);
            //指定钱包自动保存到钱包文件, 每个多少毫秒检测是否保存钱包
            WalletFiles walletFiles = wallet.autosaveToFile(file, 3 * 1000, TimeUnit.MILLISECONDS, null);
            // 第一次创建，就立即保存，进行序列化，变成字符串保存到文件
            walletFiles.saveNow();

            return wallet;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    //从已有的钱包文件中加载出钱包，如果没有的，则创建
    public Wallet loadWallet(Context context) {
        File file = context.getFileStreamPath("wallet-protobuf");
        try {
            if (file.exists()) {
                //读取文件，进行反序列化，变成Wallet对象
                InputStream inputStream = new FileInputStream(file);
                wallet = new WalletProtobufSerializer().readWallet(inputStream);
            } else {
                //钱包文件不存在，则创建钱包
                wallet = createWallet(context);
            }
            return wallet;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
