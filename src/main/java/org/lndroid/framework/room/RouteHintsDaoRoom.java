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

    @Query("SELECT * FROM RouteHint WHERE parentId = :parentId ORDER BY id_")
    abstract List<RoomData.RouteHint> getRouteHintsRoom(String parentId);

    @Query("SELECT * FROM HopHint WHERE routeHintId IN (:routeIds) ORDER BY routeHintId")
    abstract List<RoomData.HopHint> getHopHints(long[] routeIds);

    @Query("DELETE FROM HopHint WHERE routeHintId IN (SELECT id_ FROM RouteHint WHERE parentId = :parentId)")
    abstract void deleteHopHints(String parentId);

    @Query("DELETE FROM RouteHint WHERE parentId = :parentId")
    abstract void deleteRouteHints(String parentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertRouteHint(RoomData.RouteHint rh);
    @Query("UPDATE RouteHint SET id = id_ WHERE id_ = :id")
    abstract void setRouteHintId(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long[] insertHopHints(List<RoomData.HopHint> hh);
    @Query("UPDATE HopHint SET id = id_ WHERE id_ IN (:ids)")
    abstract void setHopHintIds(long[] ids);

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
            rh = rh.toBuilder().setParentId(parentId).setId(0).build();
            RoomData.RouteHint rrh = new RoomData.RouteHint();
            rrh.setData(rh);
            final long rhid = insertRouteHint(rrh);
            setRouteHintId(rhid);

            // insert RouteHint Hops
            List<RoomData.HopHint> hhs = new ArrayList<>();
            for(WalletData.HopHint hh: rh.hopHints()) {
                hh = hh.toBuilder().setRouteHintId(rhid).setId(0).build();
                RoomData.HopHint rhh = new RoomData.HopHint();
                rhh.setData(hh);
                hhs.add(rhh);
            }
            long[] hhids = insertHopHints(hhs);
            setHopHintIds(hhids);
        }
    }
}
