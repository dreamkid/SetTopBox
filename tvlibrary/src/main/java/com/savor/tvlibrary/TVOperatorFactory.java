package com.savor.tvlibrary;

import android.content.Context;

/**
 * Created by zhang.haiqiang on 2017/7/20.
 */

public class TVOperatorFactory {

    public static ITVOperator getTVOperator(Context context, TVType tvType) {
        ITVOperator itvOperator = null;
        switch (tvType) {
            case V600:
//                itvOperator = new V600TVOperator();
                break;
            case T966:
                // TODO: add T966 operator
                break;
            case GIEC:
                itvOperator = new GiecTVOperator(context);
                break;
        }
        return itvOperator;
    }

    public enum TVType {
        V600,
        T966,
        GIEC,
    }
}
