package org.lndroid.framework.room;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;

final class RoomData {

    @Entity
    static class NextId{
        @PrimaryKey
        public long pk_;
        public long id;
    }

    @Entity
    static class WalletBalance {
        @PrimaryKey
        public long id_;
        @Embedded
        public WalletData.WalletBalance data;
    }

    @Entity
    static class ChannelBalance {
        @PrimaryKey
        public long id_;
        @Embedded
        public WalletData.ChannelBalance data;
    }

    @Entity
    @TypeConverters({RoomConverters.ImmutableStringListConverter.class})
    static class WalletInfo {
        @PrimaryKey
        public long id_;
        @Embedded
        public WalletData.WalletInfo data;
    }

    static class RoomEntityBase<Type extends WalletDataDecl.EntityBase> {
        @PrimaryKey
        public long id_;
        @Embedded
        public Type data_;

        // some helpers to ensure id_ is always in sync w/ data_.id
        Type getData() {
            return data_;
        }
        void setData(@NonNull Type data) {
            id_ = data.id();
            data_ = data;
        }
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"appPubkey"}),
            @Index(value = {"appPackageName"}),
            @Index(unique = true, value = {"pubkey"}),
    })
    static class User extends RoomEntityBase<WalletData.User> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"userId", "txId"})
    })
    static class AuthRequest extends RoomEntityBase<WalletData.AuthRequest> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"preimageHashHex"}),
            @Index(unique = true, value = {"preimageHex"}),
            // FIXME unique?
            @Index(value = {"addIndex"}),
            @Index(value = {"settleIndex"}),
    })
    @TypeConverters({RoomConverters.ImmutableIntListConverter.class})
    static class Invoice extends RoomEntityBase<WalletData.Invoice> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"chanId", "htlcIndex"}),
            @Index(value = {"invoiceId"}),
    })
    @TypeConverters({RoomConverters.DestTLVConverter.class})
    static class InvoiceHTLC extends RoomEntityBase<WalletData.InvoiceHTLC> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"state"}),
    })
    @TypeConverters({
            RoomConverters.DestTLVConverter.class,
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class SendPayment extends RoomEntityBase<WalletData.SendPayment> {
    }

    static String routeHintsParentId(WalletData.SendPayment c) {
        return "sp:"+c.id();
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"sendPaymentId"}),
    })
    @TypeConverters({RoomConverters.DestTLVConverter.class})
    static class HTLCAttempt extends RoomEntityBase<WalletData.HTLCAttempt> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"state"}),
            @Index(value = {"chanId"}),
            @Index(unique = true, value = {"channelPoint"}),
    })
    static class Channel extends RoomEntityBase<WalletData.Channel> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"type", "sourceId"}),
    })
    @TypeConverters({RoomConverters.PaymentConverter.class})
    static class Payment extends RoomEntityBase<WalletData.Payment> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"pubkey"}),
    })
    @TypeConverters({
            RoomConverters.TransientRouteHintsConverter.class,
            RoomConverters.ImmutableIntListConverter.class,
    })
    static class Contact extends RoomEntityBase<WalletData.Contact> {
    }

    static String routeHintsParentId(WalletData.Contact c) {
        return "contact:"+c.id();
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"userId"}),
    })
    @TypeConverters({RoomConverters.TransientRouteHintsConverter.class})
    static class ListContactsPrivilege extends RoomEntityBase<WalletData.ListContactsPrivilege> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"userId", "contactId"}),
    })
    static class ContactPaymentsPrivilege extends RoomEntityBase<WalletData.ContactPaymentsPrivilege> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"parentId"}),
    })
    @TypeConverters({RoomConverters.TransientHopHintsConverter.class})
    static class RouteHint extends RoomEntityBase<WalletData.RouteHint> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(value = {"routeHintId"}),
    })
    static class HopHint extends RoomEntityBase<WalletData.HopHint> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"pubkey"}),
    })
    @TypeConverters({RoomConverters.ImmutableIntListConverter.class})
    static class LightningNode extends RoomEntityBase<WalletData.LightningNode> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"channelId"}),
    })
    @TypeConverters({RoomConverters.TransientRoutingPolicyConverter.class})
    static class ChannelEdge extends RoomEntityBase<WalletData.ChannelEdge> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"channelId", "reverse"}),
    })
    static class RoutingPolicy extends RoomEntityBase<WalletData.RoutingPolicy> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"txHash"}),
    })
    @TypeConverters({
            RoomConverters.ImmutableStringLongMapConverter.class,
            RoomConverters.ImmutableStringListConverter.class,
    })
    static class Transaction extends RoomEntityBase<WalletData.Transaction> {
    }

    @Entity(indices = {
            @Index(unique = true, value = {"id"}),
            @Index(unique = true, value = {"txidHex", "outputIndex"}),
    })
    static class Utxo extends RoomEntityBase<WalletData.Utxo> {
    }


}
