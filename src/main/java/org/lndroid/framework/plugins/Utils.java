package org.lndroid.framework.plugins;

import androidx.work.impl.utils.IdGenerator;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.engine.IIdGenerator;

public class Utils {

    public static ImmutableList<WalletData.RouteHint> assignRouteHintsIds(
            ImmutableList<WalletData.RouteHint> hints,
            IIdGenerator idGenerator
    ) {
        if (hints == null)
            return null;

        ImmutableList.Builder<WalletData.RouteHint> b = ImmutableList.builder();
        for(WalletData.RouteHint rh: hints) {
            WalletData.RouteHint.Builder rhb = rh.toBuilder();
            rhb.setId(idGenerator.generateId(WalletData.RouteHint.class));

            ImmutableList.Builder<WalletData.HopHint> hhsb = ImmutableList.builder();
            for(WalletData.HopHint hh: rh.hopHints()) {
                hhsb.add(hh.toBuilder()
                        .setId(idGenerator.generateId(WalletData.HopHint.class))
                        .setRouteHintId(rhb.id())
                        .build()
                );
            }
            rhb.setHopHints(hhsb.build());
            b.add(rhb.build());
        }

        return b.build();
    }
}
