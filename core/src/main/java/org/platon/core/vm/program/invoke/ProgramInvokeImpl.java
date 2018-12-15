/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.platon.core.vm.program.invoke;

import org.platon.common.wrapper.DataWord;
import org.platon.core.Repository;
import org.platon.core.db.BlockStoreIfc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
public class ProgramInvokeImpl implements ProgramInvoke {

    private BlockStoreIfc blockStore;
    /**
     * TRANSACTION  env **
     */
    private final DataWord address;
    private final DataWord origin, caller,
            balance, energon, energonPrice, callValue;
    private final long energonLong;

    byte[] msgData;

    /**
     * BLOCK  env **
     */
    private final DataWord prevHash, coinbase, timestamp,
            number, difficulty, energonLimit;

    private Map<DataWord, DataWord> storage;

    private final Repository repository;
    private boolean byTransaction = true;
    private boolean byTestingSuite = false;
    private int callDeep = 0;
    private boolean isStaticCall = false;

    public ProgramInvokeImpl(DataWord address, DataWord origin, DataWord caller, DataWord balance,
                             DataWord energonPrice, DataWord energon, DataWord callValue, byte[] msgData,
                             DataWord lastHash, DataWord coinbase, DataWord timestamp, DataWord number, DataWord
                                     difficulty,
                             DataWord energonlimit, Repository repository, int callDeep, BlockStoreIfc blockStore,
                             boolean isStaticCall, boolean byTestingSuite) {

        // Transaction env
        this.address = address;
        this.origin = origin;
        this.caller = caller;
        this.balance = balance;
        this.energonPrice = energonPrice;
        this.energon = energon;
        this.energonLong = this.energon.longValueSafe();
        this.callValue = callValue;
        this.msgData = msgData;

        // last Block env
        this.prevHash = lastHash;
        this.coinbase = coinbase;
        this.timestamp = timestamp;
        this.number = number;
        this.difficulty = difficulty;
        this.energonLimit = energonlimit;

        this.repository = repository;
        this.byTransaction = false;
        this.callDeep = callDeep;
        this.blockStore = blockStore;
        this.isStaticCall = isStaticCall;
        this.byTestingSuite = byTestingSuite;
    }

    public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, byte[] balance,
                             byte[] energonPrice, byte[] energon, byte[] callValue, byte[] msgData,
                             byte[] lastHash, byte[] coinbase, long timestamp, long number, byte[] difficulty,
                             byte[] energonlimit,
                             Repository repository, BlockStoreIfc blockStore, boolean byTestingSuite) {
        this(address, origin, caller, balance, energonPrice, energon, callValue, msgData, lastHash, coinbase,
                timestamp, number, difficulty, energonlimit, repository, blockStore);
        this.byTestingSuite = byTestingSuite;
    }


    public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, byte[] balance,
                             byte[] energonPrice, byte[] energon, byte[] callValue, byte[] msgData,
                             byte[] lastHash, byte[] coinbase, long timestamp, long number, byte[] difficulty,
                             byte[] energonlimit,
                             Repository repository, BlockStoreIfc blockStore) {

        // Transaction env
        this.address = DataWord.of(address);
        this.origin = DataWord.of(origin);
        this.caller = DataWord.of(caller);
        this.balance = DataWord.of(balance);
        this.energonPrice = DataWord.of(energonPrice);
        this.energon = DataWord.of(energon);
        this.energonLong = this.energon.longValueSafe();
        this.callValue = DataWord.of(callValue);
        this.msgData = msgData;

        // last Block env
        this.prevHash = DataWord.of(lastHash);
        this.coinbase = DataWord.of(coinbase);
        this.timestamp = DataWord.of(timestamp);
        this.number = DataWord.of(number);
        this.difficulty = DataWord.of(difficulty);
        this.energonLimit = DataWord.of(energonlimit);

        this.repository = repository;
        this.blockStore = blockStore;
    }

    /*           ADDRESS op         */
    public DataWord getOwnerAddress() {
        return address;
    }

    /*           BALANCE op         */
    public DataWord getBalance() {
        return balance;
    }

    /*           ORIGIN op         */
    public DataWord getOriginAddress() {
        return origin;
    }

    /*           CALLER op         */
    public DataWord getCallerAddress() {
        return caller;
    }

    /*           energonPRICE op       */
    public DataWord getMinEnergonPrice() {
        return energonPrice;
    }

    /*           energon op       */
    public DataWord getEnergon() {
        return energon;
    }

    @Override
    public long getEnergonLong() {
        return energonLong;
    }

    /*          CALLVALUE op    */
    public DataWord getCallValue() {
        return callValue;
    }

    /*****************/
    /***  msg data ***/
    /*****************/
    /* NOTE: In the protocol there is no restriction on the maximum message data,
     * However msgData here is a byte[] and this can't hold more than 2^32-1
     */
    private static BigInteger MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE);

    /*     CALLDATALOAD  op   */
    public DataWord getDataValue(DataWord indexData) {

        BigInteger tempIndex = indexData.value();
        int index = tempIndex.intValue(); // possible overflow is caught below
        int size = 32; // maximum datavalue size

        if (msgData == null || index >= msgData.length
                || tempIndex.compareTo(MAX_MSG_DATA) == 1)
            return DataWord.ZERO;
        if (index + size > msgData.length)
            size = msgData.length - index;

        byte[] data = new byte[32];
        System.arraycopy(msgData, index, data, 0, size);
        return DataWord.of(data);
    }

    /*  CALLDATASIZE */
    public DataWord getDataSize() {

        if (msgData == null || msgData.length == 0) return DataWord.ZERO;
        int size = msgData.length;
        return DataWord.of(size);
    }

    /*  CALLDATACOPY */
    public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

        int offset = offsetData.intValueSafe();
        int length = lengthData.intValueSafe();

        byte[] data = new byte[length];

        if (msgData == null) return data;
        if (offset > msgData.length) return data;
        if (offset + length > msgData.length) length = msgData.length - offset;

        System.arraycopy(msgData, offset, data, 0, length);

        return data;
    }


    /*     PREVHASH op    */
    public DataWord getPrevHash() {
        return prevHash;
    }

    /*     COINBASE op    */
    public DataWord getCoinbase() {
        return coinbase;
    }

    /*     TIMESTAMP op    */
    public DataWord getTimestamp() {
        return timestamp;
    }

    /*     NUMBER op    */
    public DataWord getNumber() {
        return number;
    }

    /*     DIFFICULTY op    */
    public DataWord getDifficulty() {
        return difficulty;
    }

    /*     energonLIMIT op    */
    public DataWord getEnergonLimit() {
        return energonLimit;
    }

    /*  Storage */
    public Map<DataWord, DataWord> getStorage() {
        return storage;
    }

    public Repository getRepository() {
        return repository;
    }

    @Override
    public BlockStoreIfc getBlockStore() {
        return blockStore;
    }

    @Override
    public boolean byTransaction() {
        return byTransaction;
    }

    @Override
    public boolean isStaticCall() {
        return isStaticCall;
    }

    @Override
    public boolean byTestingSuite() {
        return byTestingSuite;
    }

    @Override
    public int getCallDeep() {
        return this.callDeep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgramInvokeImpl that = (ProgramInvokeImpl) o;

        if (byTestingSuite != that.byTestingSuite) return false;
        if (byTransaction != that.byTransaction) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (balance != null ? !balance.equals(that.balance) : that.balance != null) return false;
        if (callValue != null ? !callValue.equals(that.callValue) : that.callValue != null) return false;
        if (caller != null ? !caller.equals(that.caller) : that.caller != null) return false;
        if (coinbase != null ? !coinbase.equals(that.coinbase) : that.coinbase != null) return false;
        if (difficulty != null ? !difficulty.equals(that.difficulty) : that.difficulty != null) return false;
        if (energon != null ? !energon.equals(that.energon) : that.energon != null) return false;
        if (energonPrice != null ? !energonPrice.equals(that.energonPrice) : that.energonPrice != null) return false;
        if (energonLimit != null ? !energonLimit.equals(that.energonLimit) : that.energonLimit != null) return false;
        if (!Arrays.equals(msgData, that.msgData)) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (prevHash != null ? !prevHash.equals(that.prevHash) : that.prevHash != null) return false;
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) return false;
        if (storage != null ? !storage.equals(that.storage) : that.storage != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return "ProgramInvokeImpl{" +
                "address=" + address +
                ", origin=" + origin +
                ", caller=" + caller +
                ", balance=" + balance +
                ", energon=" + energon +
                ", energonPrice=" + energonPrice +
                ", callValue=" + callValue +
                ", msgData=" + Arrays.toString(msgData) +
                ", prevHash=" + prevHash +
                ", coinbase=" + coinbase +
                ", timestamp=" + timestamp +
                ", number=" + number +
                ", difficulty=" + difficulty +
                ", energonlimit=" + energonLimit +
                ", storage=" + storage +
                ", repository=" + repository +
                ", byTransaction=" + byTransaction +
                ", byTestingSuite=" + byTestingSuite +
                ", callDeep=" + callDeep +
                '}';
    }
}
