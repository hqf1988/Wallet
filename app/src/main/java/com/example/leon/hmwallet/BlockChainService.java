package com.example.leon.hmwallet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;

import java.io.File;

public class BlockChainService extends Service {
    private static final String TAG = "BlockChainService";

    BlockChain blockChain;
    private PeerGroup peerGroup;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        try {
            //创建区块数据保存的文件目录，私有的
            File dir = getDir("blockstore", Context.MODE_PRIVATE); // 目录
            File blockfile = new File(dir, "blockchain"); //区块文件
            //创建区块数据存储管理器
            BlockStore blockStore = new SPVBlockStore(Constants.NETWORK_PARAMS, blockfile);
            //创建区块链，同步区块数据
            Wallet wallet = BitcoinWalletManager.getInstance().getWallet();
            blockChain = new BlockChain(Constants.NETWORK_PARAMS, wallet, blockStore);
            startUp();
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }

    }

    private void startUp() {
        peerGroup = new PeerGroup(Constants.NETWORK_PARAMS, blockChain);
        //设置钱包
        peerGroup.addWallet(BitcoinWalletManager.getInstance().getWallet());
        //设置监听器，监听P2P节点链接情况
        peerGroup.addConnectedEventListener(peerConnectedEventListener);
        peerGroup.addDisconnectedEventListener(peerDisconnectedEventListener);
        //添加节点搜索器
        peerGroup.addPeerDiscovery(MultiplexingDiscovery.forServices(Constants.NETWORK_PARAMS, 0));
        peerGroup.startAsync();//开启节点搜索
        peerGroup.startBlockChainDownload(null);//下载区块数据
    }

    private PeerConnectedEventListener peerConnectedEventListener = new PeerConnectedEventListener() {
        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerConnected: " + peer.toString());
        }
    };

    private PeerDisconnectedEventListener peerDisconnectedEventListener = new PeerDisconnectedEventListener() {
        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerDisconnected: " + peer.toString());
        }
    };

    public static void broadcastTransaction(Context context, Transaction transaction) {
        //再次的启动服务，如果服务已经创建了，则不会再创建服务，调用startCommand
        Intent intent = new Intent(context, BlockChainService.class);
        //将Transaction的hash值变成字节数组传递给BlockChainService
        intent.putExtra("tx", transaction.getHash().getBytes());
        context.startService(intent);//发送命令
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanArrayExtra("tx") != null) {
            //通过传递过来的Tx hash, 再次获取Tx, 使用PeerGroup进行P2P广播
            byte[] txes = intent.getByteArrayExtra("tx");
            //因为Transaction已经保存到钱包里面了
            Wallet wallet = BitcoinWalletManager.getInstance().getWallet();
            Sha256Hash wrap = Sha256Hash.wrap(txes);
            Transaction transaction = wallet.getTransaction(wrap);
            peerGroup.broadcastTransaction(transaction);
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
