package org.lndroid.framework.room;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.lndroid.framework.WalletData;

@Dao
abstract class RouteHintsDaoRoom {

    @Query("SELECT * FROM RouteHint WHERE parentId = :parentId ORDER BY id")
    abstract List<RoomData.RouteHint> getRouteHintsRoom(String parentId);

    @Query("SELECT * FROM HopHint WHERE routeHintId IN (:routeIds) ORDER BY routeHintId")
    abstract List<RoomData.HopHint> getHopHints(long[] routeIds);

    @Query("DELETE FROM HopHint WHERE routeHintId IN (SELECT id_ FROM RouteHint WHERE parentId = :parentId)")
    abstract void deleteHopHints(String parentId);

    @Query("DELETE FROM RouteHint WHERE parentId = :parentId")
    abstract void deleteRouteHints(String parentId);

    @Insert
    abstract void insertRouteHint(RoomData.RouteHint rh);

    @Insert
    abstract void insertHopHints(List<RoomData.HopHint> hh);

    @Transaction
    public ImmutableList<WalletData.RouteHint> getRouteHints(String parentId) {

        ImmutableList.Builder<WalletData.RouteHint> rb = ImmutableList.builder();

        // get routehints
        List<RoomData.RouteHint> routeHints = getRouteHintsRoom(parentId);
        if (routeHints.isEmpty())
            return rb.build();

        // get hophints
        long[] routeHintIds = new long[routeHints.size()];
        for (int i = 0; i < routeHints.size(); i++) {
            routeHintIds[i] = routeHints.get(i).id_;
        }
        LinkedList<RoomData.HopHint> hopHints = new LinkedList<>(getHopHints(routeHintIds));

        // map hh to rh
        for (RoomData.RouteHint rh : routeHints) {
            ImmutableList.Builder<WalletData.HopHint> hb = ImmutableList.builder();

            // given that both routehints and hophints are ordered by routehintid,
            // we can use this O(N) algo to attach hh to rh
            while (!hopHints.isEmpty() && hopHints.getFirst().getData().routeHintId() == rh.id_) {
                hb.add(hopHints.getFirst().getData());
                hopHints.removeFirst();
            }

            WalletData.RouteHint.Builder b = rh.getData().toBuilder();
            b.setHopHints(hb.build());
            rb.add(b.build());
        }

        return rb.build();
    }

    @Transaction
    public void upsertRouteHints(String parentId, @Nullable List<WalletData.RouteHint> routeHints) {

        deleteHopHints(parentId);
        deleteRouteHints(parentId);

        if (routeHints == null)
            return;

        // insert routehints
        for(WalletData.RouteHint rh: routeHints) {

            // insert RouteHint, make sure we insert a new one, not replace a copied one
            rh = rh.toBuilder().setParentId(parentId).build();
            RoomData.RouteHint rrh = new RoomData.RouteHint();
            rrh.setData(rh);
            insertRouteHint(rrh);

            // insert RouteHint Hops
            List<RoomData.HopHint> hhs = new ArrayList<>();
            for(WalletData.HopHint hh: rh.hopHints()) {
                RoomData.HopHint rhh = new RoomData.HopHint();
                rhh.setData(hh);
                hhs.add(rhh);
            }
            insertHopHints(hhs);
        }
    }
}
