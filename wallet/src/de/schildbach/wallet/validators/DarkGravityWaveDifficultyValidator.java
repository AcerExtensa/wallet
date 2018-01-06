package de.schildbach.wallet.validators;


import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

public class DarkGravityWaveDifficultyValidator implements DifficultyValidator {
    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore) throws VerificationException, BlockStoreException {

    }
}
