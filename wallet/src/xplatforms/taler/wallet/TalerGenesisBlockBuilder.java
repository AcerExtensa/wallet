package xplatforms.taler.wallet;

import android.util.Log;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.bitcoinj.core.Coin.FIFTY_COINS;

public class TalerGenesisBlockBuilder {

    public static Block createGenesis(NetworkParameters n) {

        Transaction t = createGenesisTransaction(n);

        final Sha256Hash merkleRoot = null;
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

        Log.d("EXDBG:", "BLOCK AGAIN:  " + genesisBlock.toString());

        return genesisBlock;
    }

    private static Transaction createGenesisTransaction(NetworkParameters n)
    {

        Transaction t = new Transaction(n);
        try
        {
            byte[] bytes = Utils.HEX.decode
                    ("04ffff001d01043e54616c6572207065727368616a612062656c617275736b616a61206b727970746176616c697574612062792044656e6973204c2069205365726765204c20");

            t.addInput(new TransactionInput(n, t, bytes));

            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("04f360606cf909ce34d4276ce40a5dd6a844a4a72473086e0fc635f3c4195d77df513b7541dc5f6f6d01ec39e4b729893c6d42dd5e248379a32b5259f38f6bfbae"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(n, t, FIFTY_COINS, scriptPubKeyBytes.toByteArray()));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return t;
    }
}
