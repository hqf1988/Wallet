package com.example.leon.hmwallet;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

public class Constants {
    //定义一个开关，是测试网络还是主网
    public static final boolean TEST = true;
    public static NetworkParameters NETWORK_PARAMS = TEST ? TestNet3Params.get(): MainNetParams.get();
}
