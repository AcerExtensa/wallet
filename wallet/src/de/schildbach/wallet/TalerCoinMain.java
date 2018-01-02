package de.schildbach.wallet;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.net.discovery.HttpDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;
import static org.bitcoinj.core.Coin.FIFTY_COINS;

/**
 * Created by alexk on 12/19/17.
 */

public class TalerCoinMain extends MainNetParams {

    private static TalerCoinMain instance;

    private TalerCoinMain() {
        id = "taler.main";

        addressHeader = 65;
        p2shHeader = 50;
        acceptableAddressCodes = new int[]{addressHeader, p2shHeader};
        spendableCoinbaseDepth = 100;
        dumpedPrivateKeyHeader = 176;

        port = 23153;

        dnsSeeds = new String[]{
                "dnsseed.taler.site"
        };

        maxTarget = Utils.decodeCompactBits(0x1e0fffff);//TODO: replace with actual

        checkpoints = new HashMap<>();

        checkpoints.put(1024, Sha256Hash.wrap("8d769df2ac2cabb10038ba2a0ffd269e5cf93701c27256a27fb580a25106a170"));
        checkpoints.put(2048, Sha256Hash.wrap("c4838cab89b16915d813f424198a999af82b3dce2afed5d82cab1fe9df08d701"));
        checkpoints.put(6602, Sha256Hash.wrap("f225e2f57a5e90539a4d74b3bf1ed906a8146c64addff0f5279473fb6c5e9f0e"));

        genesisBlock = createGenesis(this);

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("c079fd1ae86223e1522928776899d46e329da7919ca1e11be23643c67dd05d5f"),
                genesisHash);

        //packetMagic = 0x64b173d8L;

        httpSeeds = new HttpDiscovery.Details[]{};
        //addrSeeds = new int[]{};
        //name = "Taler";
        //symbol = "TLR";
        //uriScheme = "taler";
        //bip44Index = 1524;
        //unitExponent = 8;
        //feeValue = value(100000);
        //minNonDust = value(1000); // 0.00001 LTC mininput
        //softDustLimit = value(100000); // 0.001 LTC
        //softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        //signedMessageHeader = toBytes("Litecoin Signed Message:\n");
    }

    private static Block createGenesis(NetworkParameters n) {

        Transaction t = createGenesisTransaction(n);

        final Sha256Hash merkleRoot = null; //TODO: replace with actual
        final long version = 1;
        final Sha256Hash prevBlock = Sha256Hash.ZERO_HASH;
        final long time = 1505338813;
        final long difficultyTarget = 0x1e0ffff0;
        final long nonce = 725170;

        final Block genesisBlock = new Block(
                n,
                version,
                prevBlock,
                merkleRoot,
                time,
                difficultyTarget,
                nonce,
                Collections.singletonList(t)
        );
        //genesisBlock.getMerkleRoot()
        return genesisBlock;
    }

    private static Transaction createGenesisTransaction(NetworkParameters n) {

        final String message = "Taler pershaja belaruskaja kryptavaliuta by Denis L i Serge L ";

        Transaction t = new Transaction(n);
        try {
            // A script containing the difficulty bits and the following message:
            //
            //   "Taler pershaja belaruskaja kryptavaliuta by Denis L i Serge L "
            byte[] bytes = Utils.HEX.decode
                    ("04ffff001d01043e54616c6572207065727368616a612062656c617275736b616a61206b727970746176616c697574612062792044656e6973204c2069205365726765204c20");

            /*ScriptBuilder builder = new ScriptBuilder();

            builder.number(486604799)
                    .number(4)
                    .data(toByteArrray(message.toCharArray()));

            Script script = builder.build();*/


            t.addInput(new TransactionInput(n, t, bytes));

            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("04f360606cf909ce34d4276ce40a5dd6a844a4a72473086e0fc635f3c4195d77df513b7541dc5f6f6d01ec39e4b729893c6d42dd5e248379a32b5259f38f6bfbae"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(n, t, FIFTY_COINS, scriptPubKeyBytes.toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }

        return t;
    }

    private static byte[] toByteArrray(char[] charArray) {
        byte[] result = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            result[i] = (byte) charArray[i];
        }
        return result;
    }

    public static synchronized TalerCoinMain get() {
        if (instance == null) {
            instance = new TalerCoinMain();
        }
        return instance;
    }

    @Override
    public int getProtocolVersionNum(ProtocolVersion version) {
        return 70002;
    }
}
