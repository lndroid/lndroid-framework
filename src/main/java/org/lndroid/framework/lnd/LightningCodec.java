package org.lndroid.framework.lnd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.lndroid.framework.common.HEX;
import org.lndroid.lnd.data.Data;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.WalletDataDecl;

public class LightningCodec {

    public static final long TLV_MESSAGE = 34349334;
    public static final long TLV_SENDER_PUBKEY = 34349339;
    public static final long TLV_SENDER_TIME = 34349343;
//    tlvSigRecord    = 34349337

    public static final long TLV_PREIMAGE = 5482373484L;

    public static byte[] hexToBytes(String s) {
        return HEX.toBytes(s);
    }

    public static String bytesToHex(byte[] bytes) {
        return HEX.fromBytes(bytes);
    }

    public static byte[] reverseBytes(byte[] b) {
        byte[] r = new byte[b.length];
        for(int i = 0; i < b.length; i++)
            r[b.length - i - 1] = b[i];
        return r;
    }

    public static class WalletBalanceConverter {

        public static void decode(Data.WalletBalanceResponse rep, WalletData.WalletBalance.Builder b) {
            b.setConfirmedBalance(rep.confirmedBalance);
            b.setTotalBalance(rep.totalBalance);
            b.setUnconfirmedBalance(rep.unconfirmedBalance);
        }
    }

    public static class ChannelBalanceConverter {

        public static void decode(Data.ChannelBalanceResponse rep, WalletData.ChannelBalance.Builder b) {
            b.setBalance(rep.balance);
            b.setPendingOpenBalance(rep.pendingOpenBalance);
        }
    }

    public static class WalletInfoConverter {

        public static void decode(Data.GetInfoResponse rep, WalletData.WalletInfo.Builder b) {
            b.setIdentityPubkey(rep.identityPubkey);
            b.setAlias(rep.alias);
            b.setNumPendingChannels(rep.numPendingChannels);
            b.setNumActiveChannels(rep.numActiveChannels);
            b.setNumPeers(rep.numPeers);
            b.setBlockHeight(rep.blockHeight);
            b.setBlockHash(rep.blockHash);
            b.setSyncedToChain(rep.syncedToChain);
            b.setUris(ImmutableList.copyOf(rep.uris));
            b.setBestHeaderTimestamp(rep.bestHeaderTimestamp);
            b.setLndVersion(rep.version);
            b.setNumInactiveChannels(rep.numInactiveChannels);
            /// A list of active chains the node is connected to,
            // Chains are a separate entity w/ separate getter
            //public List<String> chains;
            b.setColor(rep.color);
            b.setSyncedToGraph(rep.syncedToGraph);
        }
    }

    public static class PayReqConverter {

        public static void decode(Data.PayReq rep, WalletData.SendPayment.Builder b) {
            b.setFinalCltvDelta((int)rep.cltvExpiry);
            b.setDestPubkey(rep.destination);
            b.setPaymentHashHex(rep.paymentHash);
            b.setInvoiceDescription(rep.description);
            b.setInvoiceDescriptionHashHex(rep.descriptionHash);
            b.setInvoiceFallbackAddr(rep.fallbackAddr);
            b.setInvoiceExpiry(rep.expiry);
            b.setInvoiceTimestamp(rep.timestamp);
            b.setPaymentAddrHex(bytesToHex(rep.paymentAddr));
            b.setValueMsat(rep.numSatoshis * 1000);
            if (rep.features != null) {
                b.setFeatures(ImmutableList.copyOf(rep.features));
            }
            if (rep.routeHints != null) {
                ImmutableList.Builder<WalletData.RouteHint> rhsb = ImmutableList.builder();
                for (Data.RouteHint rh : rep.routeHints) {
                    WalletData.RouteHint.Builder rhb = WalletData.RouteHint.builder();
                    ImmutableList.Builder<WalletData.HopHint> hhsb = ImmutableList.builder();
                    for (int i = 0; i < rh.hopHints.size(); i++) {
                        Data.HopHint hh = rh.hopHints.get(i);
                        hhsb.add(WalletData.HopHint.builder()
                                .setChanId(hh.chanId)
                                .setNodeId(hh.nodeId)
                                .setCltvExpiryDelta(hh.cltvExpiryDelta)
                                .setFeeBaseMsat(hh.feeBaseMsat)
                                .setFeeProportionalMillionths(hh.feeProportionalMillionths)
                                .setIndex(i)
                                .build());
                    }
                    rhb.setHopHints(hhsb.build());
                    rhsb.add(rhb.build());
                }
                b.setRouteHints(rhsb.build());
            }
        }
    }

    public static class PaymentConverter {

        public static void decode(Data.Payment rep, WalletData.SendPayment.Builder b) {
            b.setSendTime(rep.creationTime);
            b.setFeeMsat(rep.feeMsat);
            b.setTotalValueMsat(rep.valueMsat + rep.feeMsat);
        }
    }

    public static class QueryRoutesConvertor {

        public static boolean encode(WalletData.SendPayment req, Data.QueryRoutesRequest r) {
            r.pubKey = req.destPubkey();
            r.amtMsat = req.valueMsat();
            r.lastHopPubkey = null; // FIXME
            r.cltvLimit = req.cltvLimit();
            if (req.feeLimitPercent() != 0 || req.feeLimitFixedMsat() != 0) {
                r.feeLimit = new Data.FeeLimit();
                r.feeLimit.percent = req.feeLimitPercent();
                r.feeLimit.fixedMsat = req.feeLimitFixedMsat();
            }
            r.finalCltvDelta = req.finalCltvDelta();
            r.outgoingChanId = req.outgoingChanId();
            if (req.features() != null) {
                r.destFeatures = req.features();
            }

            if (req.routeHints() != null) {
                r.routeHints = new ArrayList<>();
                for (WalletData.RouteHint wrh : req.routeHints()) {
                    Data.RouteHint rh = new Data.RouteHint();
                    rh.hopHints = new ArrayList<>();
                    for (WalletData.HopHint whh : wrh.hopHints()) {
                        Data.HopHint hh = new Data.HopHint();
                        hh.cltvExpiryDelta = whh.cltvExpiryDelta();
                        hh.feeProportionalMillionths = whh.feeProportionalMillionths();
                        hh.feeBaseMsat = whh.feeBaseMsat();
                        hh.nodeId = whh.nodeId();
                        hh.chanId = whh.chanId();
                        rh.hopHints.add(hh);
                    }
                    r.routeHints.add(rh);
                }
            }

            r.destCustomRecords = new HashMap<>();
            if (req.destCustomRecords() != null)
                r.destCustomRecords.putAll(req.destCustomRecords());

            if (req.isKeysend()) {
                r.destCustomRecords.put(TLV_PREIMAGE, LightningCodec.hexToBytes(req.paymentPreimageHex()));
            }

            try {
                if (req.message() != null) {
                    r.destCustomRecords.put(TLV_MESSAGE, req.message().getBytes("UTF-8"));

                    // ns
                    final long ns = System.currentTimeMillis() * 1000;
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    buffer.putLong(ns);

                    r.destCustomRecords.put(TLV_SENDER_TIME, buffer.array());

                }
                if (req.senderPubkey() != null) {
                    r.destCustomRecords.put(TLV_SENDER_PUBKEY, hexToBytes(req.senderPubkey()));
                }

                // FIXME add signature as in WhatSat see here https://github.com/joostjager/whatsat/blob/b3759020e913727ef2f9661b3463a5035b6887a6/cmd_chat.go#L272
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }

    public static class SendToRouteCodec {

        public static boolean encode(WalletDataDecl.SendPayment req, Data.SendToRouteRequest r) {
            r.paymentHash = LightningCodec.hexToBytes(req.paymentHashHex());
            // set MPP info if invoice requires it
            if (req.paymentAddrHex() != null) {
                Data.Hop h = r.route.hops.get(r.route.hops.size() - 1);
                h.mppRecord = new Data.MPPRecord();
                h.mppRecord.paymentAddr = LightningCodec.hexToBytes(req.paymentAddrHex());
                h.mppRecord.totalAmtMsat = h.amtToForwardMsat;
            }
            return true;
        }

        public static void decode(Data.SendResponse rep, WalletData.SendPayment.Builder b) {
            b.setPaymentError(rep.paymentError);
            b.setPaymentPreimageHex(LightningCodec.bytesToHex(rep.paymentPreimage));
            b.setFeeMsat(rep.paymentRoute.totalFeesMsat);
            b.setTotalValueMsat(rep.paymentRoute.totalAmtMsat + rep.paymentRoute.totalFeesMsat);
/*            Data.Hop hop = rep.paymentRoute.hops.get(0);
            ImmutableMap.Builder<Long,byte[]> tlvb = ImmutableMap.builder();
            for(Map.Entry<Long,byte[]> e: hop.tlv) {
                tlvb.put(e.getKey(), e.getValue());
            }
            b.setDestTlv(tlvb.build());
 */
        }
    }

    public static class NewAddressCodec {

        public static boolean encode(WalletData.NewAddressRequest req, Data.NewAddressRequest r) {
            r.type = req.type();
            return true;
        }

        public static void decode(Data.NewAddressResponse rep, WalletData.NewAddress.Builder r) {
            r.setAddress(rep.address);
        }

    }

    public static class ConnectPeerCodec {

        public static boolean encode(WalletData.ConnectPeerRequest req, Data.ConnectPeerRequest r) {
            r.perm = req.perm();
            r.addr = new Data.LightningAddress();
            r.addr.host = req.host();
            r.addr.pubkey = req.pubkey();
            return true;
        }

        public static void decode(Data.ConnectPeerResponse rep, WalletData.ConnectPeerResponse.Builder r) {
        }

    }

    public static class AddInvoiceCodec {

        public static boolean encode(WalletData.AddInvoiceRequest req, Data.Invoice r) {
            r.memo = req.description();
            r.fallbackAddr = req.fallbackAddr();
            r.expiry = req.expiry();
            r.value = req.valueSat();

            if (req.descriptionHashHex() != null) {
                r.descriptionHash = hexToBytes(req.descriptionHashHex());
                if (r.descriptionHash == null)
                    return false;
            }

            if (req.preimageHex() != null) {
                r.rPreimage = hexToBytes(req.preimageHex());
                if (r.rPreimage == null)
                    return false;
            }

            return true;
        }

        public static void decode(Data.AddInvoiceResponse rep, WalletData.Invoice.Builder b) {
            b.setAddIndex(rep.addIndex);
            b.setPaymentRequest(rep.paymentRequest);
            b.setPreimageHashHex(bytesToHex(rep.rHash));
        }
    }

    public static class InvoiceConverter {

        public static void decode(Data.Invoice rep, WalletData.Invoice.Builder b) {
            b.setDescription(rep.memo);
            b.setPreimageHex(bytesToHex(rep.rPreimage));
            b.setPreimageHashHex(bytesToHex(rep.rHash));
            b.setValueSat(rep.value);
            b.setCreateTime(rep.creationDate * 1000);
            b.setSettleTime(rep.settleDate * 1000);
            b.setPaymentRequest(rep.paymentRequest);
            b.setDescriptionHashHex(bytesToHex(rep.descriptionHash));
            b.setExpiry(rep.expiry);
            b.setFallbackAddr(rep.fallbackAddr);
            b.setCltvExpiry(rep.cltvExpiry);

            // FIXME public List<RouteHint> routeHints;

            b.setIsPrivate(rep.isPrivate);
            b.setAddIndex(rep.addIndex);
            b.setSettleIndex(rep.settleIndex);
            b.setAmountPaidMsat(rep.amtPaidMsat);

            // assume state mapping is the same
            b.setState(rep.state);

            b.setIsKeysend(rep.isKeysend);

            /// List of HTLCs paying to this invoice [EXPERIMENTAL].
            // FIXME public List<InvoiceHTLC> htlcs;
        }
    }

    public static class InvoiceHTLCConverter {

        public static void decode(Data.InvoiceHTLC rep, WalletData.InvoiceHTLC.Builder b) {
            b.setChanId(rep.chanId);
            b.setHtlcIndex(rep.htlcIndex);
            b.setAmountMsat(rep.amtMsat);
            b.setAcceptHeight(rep.acceptHeight);
            b.setAcceptTime(rep.acceptTime);
            b.setResolveTime(rep.resolveTime);
            b.setExpiryHeight(rep.expiryHeight);
            b.setState(rep.state);
            if (rep.tlv != null) {
                ImmutableMap.Builder<Long, byte[]> tlvb = ImmutableMap.builder();
                tlvb.putAll(rep.tlv);
                b.setCustomRecords(tlvb.build());
                byte[] message = rep.tlv.get(TLV_MESSAGE);
                if (message != null) {
                    try {
                        b.setMessage(new String(message, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                byte[] pubkey = rep.tlv.get(TLV_SENDER_PUBKEY);
                if (pubkey != null)
                    b.setSenderPubkey(bytesToHex(pubkey));
                byte[] time = rep.tlv.get(TLV_SENDER_TIME);
                if (time != null && time.length == 8) {
                    ByteBuffer buffer = ByteBuffer.wrap(time);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    long tm = buffer.getLong();

                    b.setSenderTime(tm / 1000);
                }
            }
        }
    }

    public static String channelPointToString(Data.ChannelPoint rep) {
        if (rep.fundingTxidBytes != null)
            return bytesToHex(reverseBytes(rep.fundingTxidBytes)).toLowerCase() + ":" + rep.outputIndex;
        else
            return rep.fundingTxidStr + ":" + rep.outputIndex;
    }

    public static class OpenChannelCodec {

        public static boolean encode(WalletData.Channel req, Data.OpenChannelRequest r) {

            // FIXME move these to req.isValid
            if (req.remotePubkey() == null)
                return false;
            r.nodePubkeyString = req.remotePubkey();
            r.nodePubkey = hexToBytes(req.remotePubkey());
            if (r.nodePubkey == null)
                return false;

            r.isPrivate = req.isPrivate();
            r.localFundingAmount = req.capacity();
            r.pushSat = req.remoteBalance();
            r.minConfs = req.minConfs();
            r.minHtlcMsat = req.minHtlcMsat();
            r.remoteCsvDelay = req.csvDelay();
            r.targetConf = req.targetConf();
            r.satPerByte = req.satPerByte();
            r.spendUnconfirmed = req.spendUnconfirmed();
            return r.localFundingAmount > 0;
        }

        public static void decode(Data.ChannelPoint rep, WalletData.Channel.Builder b) {
            b.setChannelPoint(channelPointToString(rep));
        }
    }

    public static class ChannelCloseSummaryConverter {

        public static void decode(Data.ChannelCloseSummary rep, WalletData.Channel.Builder b) {
            b.setChainHashHex(rep.chainHash);
            b.setClosingTxHashHex(rep.closingTxHash);
            b.setCloseHeight(rep.closeHeight);
            b.setSettledBalance(rep.settledBalance);
            b.setTimeLockedBalance(rep.timeLockedBalance);
            b.setCloseType(rep.closeType);
        }
    }

    public static class ChannelConverter {

        public static void decode(Data.Channel rep, WalletData.Channel.Builder b) {
            b.setActive(rep.active);
            b.setRemotePubkey(rep.remotePubkey);
            b.setChannelPoint(rep.channelPoint);
            b.setChanId(rep.chanId);
            b.setCapacity(rep.capacity);
            b.setLocalBalance(rep.localBalance);
            b.setRemoteBalance(rep.remoteBalance);
            b.setCommitFee(rep.commitFee);
            b.setCommitWeight(rep.commitWeight);
            b.setFeePerKw(rep.feePerKw);
            b.setUnsettledBalance(rep.unsettledBalance);
            b.setTotalSatoshisSent(rep.totalSatoshisSent);
            b.setTotalSatoshisReceived(rep.totalSatoshisReceived);
            b.setNumUpdates(rep.numUpdates);
            b.setCsvDelay(rep.csvDelay);
            b.setIsPrivate(rep.isPrivate);
            b.setInitiator(rep.initiator);
            b.setChanStatusFlags(rep.chanStatusFlags);
            b.setLocalChanReserveSat(rep.localChanReserveSat);
            b.setRemoteChanReserveSat(rep.remoteChanReserveSat);
            b.setStaticRemoteKey(rep.staticRemoteKey);
            b.setLifetime(rep.lifetime);
            b.setUptime(rep.uptime);
        }
    }

    public static class LightningNodeConverter {
        public static void decode(Data.LightningNode r, WalletData.LightningNode.Builder b) {
            if (r.features != null)
                b.setFeatures(ImmutableList.copyOf(r.features));
            b.setColor(r.color);
            b.setAlias(r.alias);
            b.setPubkey(r.pubKey);
            b.setLastUpdate(r.lastUpdate);
        }
    }

    public static class ChannelEdgeConverter {

        public static void decode(Data.RoutingPolicy r, WalletData.RoutingPolicy.Builder b) {
            b.setLastUpdate(r.lastUpdate);
            b.setMaxHtlcMsat(r.maxHtlcMsat);
            b.setDisabled(r.disabled);
            b.setFeeRateMilliMsat(r.feeRateMilliMsat);
            b.setFeeBaseMsat(r.feeBaseMsat);
            b.setMinHtlc(r.minHtlc);
            b.setTimeLockDelta(r.timeLockDelta);
        }

        public static void decode(Data.ChannelEdge r, WalletData.ChannelEdge.Builder b) {
            b.setCapacity(r.capacity);
            b.setNode1Pubkey(r.node1Pubkey);
            b.setNode2Pubkey(r.node2Pubkey);
            b.setChanPoint(r.chanPoint);
            b.setChannelId(r.channelId);
            if (r.node1Policy != null) {
                WalletData.RoutingPolicy.Builder pb = WalletData.RoutingPolicy.builder();
                pb.setChannelId(r.channelId);
                pb.setReverse(false);
                decode(r.node1Policy, pb);
                b.setNode1Policy(pb.build());
            }
            if (r.node2Policy != null) {
                WalletData.RoutingPolicy.Builder pb = WalletData.RoutingPolicy.builder();
                pb.setChannelId(r.channelId);
                pb.setReverse(true);
                decode(r.node2Policy, pb);
                b.setNode2Policy(pb.build());
            }
        }
    }


}
