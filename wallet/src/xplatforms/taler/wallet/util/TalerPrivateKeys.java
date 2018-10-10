package xplatforms.taler.wallet.util;

import android.util.Log;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.Wallet;

import java.math.BigInteger;
import java.net.InetAddress;

import xplatforms.taler.wallet.TalerCoinMain;

/**
 * Created by acerextensa on 12.02.18.
 */

public class TalerPrivateKeys {

    public static void fromQrInput(String addr, String dest) throws Exception
    {
        // TODO: Assumes main network not testnet. Make it selectable.
        NetworkParameters params = TalerCoinMain.get();
        try {
            // Decode the private key from Satoshis Base58 variant. If 51 characters long then it's from Bitcoins
            // dumpprivkey command and includes a version byte and checksum, or if 52 characters long then it has
            // compressed pub key. Otherwise assume it's a raw key.
            ECKey key;
            if (addr.length() == 51 || addr.length() == 52)
            {
                DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, addr);
                key = dumpedPrivateKey.getKey();
            }
            else
            {
                BigInteger privKey = Base58.decodeToBigInteger(addr);
                key = ECKey.fromPrivate(privKey);
            }

            //Log.d("EXDBG:", "Address from private key is: " + key.toAddress(params).toString());
            //System.out.println("Address from private key is: " + key.toAddress(params).toString());
            // And the address ...




            Address destination = Address.fromBase58(params, dest);

            // Import the private key to a fresh wallet.
            Wallet wallet = new Wallet(params);
            wallet.importKey(key);

            // Find the transactions that involve those coins.
            final MemoryBlockStore blockStore = new MemoryBlockStore(params);
            BlockChain chain = new BlockChain(params, wallet, blockStore);

            final PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addAddress(new PeerAddress(params, InetAddress.getLocalHost()));
            peerGroup.startAsync();
            peerGroup.downloadBlockChain();
            peerGroup.stopAsync();

            // And take them!
            Log.d("EXDBG:", "Claiming " + wallet.getBalance().toFriendlyString());
            //System.out.println("Claiming " + wallet.getBalance().toFriendlyString());
            wallet.sendCoins(peerGroup, destination, wallet.getBalance());
            // Wait a few seconds to let the packets flush out to the network (ugly).
            Thread.sleep(5000);
            //System.exit(0);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            Log.d("EXDBG:", "First arg should be private key in Base58 format. Second argument should be address to send to.");
        }
    }
}
