package de.schildbach.wallet.validators;


import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

public interface DifficultyValidator {
    void checkDifficultyTransitions(
            final StoredBlock storedPrev,
            final Block nextBlock,
            final BlockStore blockStore
    ) throws VerificationException, BlockStoreException;
}
