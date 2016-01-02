package com.mediatek.rcs.message.utils;


import java.util.List;

public final class RcsVcardData {
    private final String mData;
    private final String mType;

    public RcsVcardData(String data, String type) {
        mData = data;
        mType = type;
    }

    public String getData() {
        return mData;
    }

    public String getType() {
        return mType;
    }

    @Override
    public String toString() {
        return String.format("\n\n%s", mData);
    }
}

