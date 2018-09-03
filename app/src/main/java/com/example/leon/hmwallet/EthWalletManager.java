package com.example.leon.hmwallet;

import android.content.Context;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EthWalletManager {

    private static EthWalletManager sInstance;

    public static final String PASSWORD = "a12345678";

    private ObjectMapper objectMapper = new ObjectMapper();

    private EthWalletManager() {
    }

    public static EthWalletManager getInstance() {
        if (sInstance == null) {
            synchronized (EthWalletManager.class) {
                if (sInstance == null) {
                    sInstance = new EthWalletManager();
                }
            }
        }
        return sInstance;
    }

    //跟助记词没关系，这里没有遵循bip协议
    public WalletFile createWallet(Context context) {
        try {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            WalletFile walletFile = Wallet.createLight(PASSWORD, ecKeyPair);
            //保存到本地
            File dir = context.getDir("eth", Context.MODE_PRIVATE);
            //创建了WalletFile对应的文件
            File file = new File(dir, getWalletFileName(walletFile));
            //讲WalletFile对象序列化写入磁盘文件中
            objectMapper.writeValue(file, walletFile);
            return walletFile;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (CipherException e) {
            e.printStackTrace();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据WalletFile的文件名字
    private static String getWalletFileName(WalletFile walletFile) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("'UTC--'yyyy-MM-dd'T'HH-mm-ss.SSS'--'");
        return dateFormat.format(new Date()) + walletFile.getAddress() + ".json";
    }

    //首先从文件当中读取钱包，如果没有钱包文件，则再创建
    public WalletFile loadWallet(Context context) {
        WalletFile walletFile = null;
        File dir = context.getDir("eth", Context.MODE_PRIVATE);
        //存在钱包文件
        if (dir.exists() && dir.listFiles().length > 0) {
            File file = dir.listFiles()[0];
            try {
                 walletFile = objectMapper.readValue(file, WalletFile.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            walletFile = createWallet(context);
        }
        return walletFile;
    }
}
