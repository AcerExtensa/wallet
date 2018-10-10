package org.bitcoinj.params;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.net.discovery.HttpDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by acerextensa on 31.01.18.
 */


public class TalerNetParams extends MainNetParams {

    private static final int TARGET_TIMESPAN = 10 * 60;  // 10 minutes per difficulty cycle, on average.
    private static final int TARGET_SPACING = 5 * 60;  // 5 minutes per block.
    private static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;

    private final int lyra2ZHeight = 10000;
    private final int powAveragingWindow = 24;


    private static TalerNetParams instance;

    private TalerNetParams() {
        id = "taler.main";
        interval = INTERVAL;
        targetTimespan = TARGET_SPACING;
        maxTarget = Utils.decodeCompactBits(0x1e0fffff);//TODO: replace with actual
        dumpedPrivateKeyHeader = 176;//TODO: I dont know what is it
        addressHeader = 65;
        p2shHeader = 50;
        acceptableAddressCodes = new int[]{addressHeader, p2shHeader};
        port = 23153;
        packetMagic = 0x64b173d8L;
        bip32HeaderPub = 0x0488B21E; //The 4 byte header that serializes in base58 to "xpub". //TODO: I dont know what is it
        bip32HeaderPriv = 0x0488ADE4; //The 4 byte header that serializes in base58 to "xprv" //TODO: I dont know what is it

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE; //TODO: I dont know what is it
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED; //TODO: I dont know what is it
        majorityWindow = MAINNET_MAJORITY_WINDOW; //TODO: I dont know what is it

        //genesisBlock = TalerGenesisBlockBuilder.createGenesis(this);

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("c079fd1ae86223e1522928776899d46e329da7919ca1e11be23643c67dd05d5f"),
                genesisHash);

        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 20;

        checkpoints = new HashMap<>();

        checkpoints.put(1024, Sha256Hash.wrap("8d769df2ac2cabb10038ba2a0ffd269e5cf93701c27256a27fb580a25106a170"));
        checkpoints.put(2048, Sha256Hash.wrap("c4838cab89b16915d813f424198a999af82b3dce2afed5d82cab1fe9df08d701"));
        checkpoints.put(6602, Sha256Hash.wrap("f225e2f57a5e90539a4d74b3bf1ed906a8146c64addff0f5279473fb6c5e9f0e"));


        dnsSeeds = new String[]{
                "dnsseed.taler.site"
        };


        httpSeeds = new HttpDiscovery.Details[]{};
    }


    public static synchronized TalerNetParams get() {
        if (instance == null) {
            instance = new TalerNetParams();
        }
        return instance;
    }

    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {
        int nextHeight = storedPrev.getHeight() + 1;
        if (nextHeight < lyra2ZHeight) {
            if (nextBlock.getDifficultyTargetAsInteger().compareTo(maxTarget) <= 0 &&
                    nextBlock.getHash().toBigInteger().compareTo(nextBlock.getDifficultyTargetAsInteger()) > 0) {
            } else {
                throw new VerificationException("wrong block");
            }
        } else {
            if (nextBlock.getDifficultyTargetAsInteger().compareTo(Utils.decodeCompactBits(0x2000ffff)) <= 0 &&
                    nextBlock.getHash().toBigInteger().compareTo(nextBlock.getDifficultyTargetAsInteger()) > 0) {
            } else {
                throw new VerificationException("wrong block");
            }
        }

//        if(nextHeight < lyra2ZHeight) {
//            btcDifficultyValidator.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
//        } else if (nextHeight < lyra2ZHeight + powAveragingWindow) {
//            constDifficultyValidator.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
//        } else {
//            darkGravityDifficultyValidator.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
//        }
    }

    @Override
    public int getProtocolVersionNum(ProtocolVersion version) {
        return 70015;
    }
}
