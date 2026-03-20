package com.example.idgen.core;

import com.example.idgen.config.IdGenProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Compose 64-bit ID:
 *  [timeBits][tenantBits][bizBits][seqBits]
 *
 * - time unit: seconds (trend-ordered)
 * - seq: per-second sequence from Redis segments
 */
@Component
public class IdComposer {

    private final IdGenProperties props;

    private final int timeBits;
    private final int tenantBits;
    private final int bizBits;
    private final int seqBits;

    private final long timeMask;
    private final long tenantMask;
    private final long bizMask;
    private final long seqMask;

    public IdComposer(IdGenProperties props) {
        this.props = props;

        this.timeBits = props.getTimeBits();
        this.tenantBits = props.getTenantBits();
        this.bizBits = props.getBizBits();
        this.seqBits = props.getSeqBits();

        if (timeBits + tenantBits + bizBits + seqBits != 64) {
            throw new IllegalArgumentException("timeBits+tenantBits+bizBits+seqBits must equal 64");
        }

        this.timeMask = (timeBits == 64) ? -1L : ((1L << timeBits) - 1);
        this.tenantMask = (tenantBits == 64) ? -1L : ((1L << tenantBits) - 1);
        this.bizMask = (bizBits == 64) ? -1L : ((1L << bizBits) - 1);
        this.seqMask = (seqBits == 64) ? -1L : ((1L << seqBits) - 1);
    }

    public long compose(long epochSecondNow, int tenantId, String bizKey, long seq) {
        long t = (epochSecondNow - props.getEpochSeconds()) & timeMask;
        long tenant = ((long) tenantId) & tenantMask;
        long biz = ((long) bizId(bizKey)) & bizMask;
        long s = seq & seqMask;

        // overflow protection: if seq exceeds range, you MUST increase seqBits or reduce per-second throughput per key.
        if (seq != s) {
            throw new IllegalStateException("Sequence overflow for current second. seq=" + seq +
                    " exceeds " + seqMask + ". Consider increasing idgen.seq-bits or sharding bizKey.");
        }

        int shiftTenant = bizBits + seqBits;
        int shiftTime = tenantBits + bizBits + seqBits;
        long id = (t << shiftTime) | (tenant << shiftTenant) | (biz << seqBits) | s;
        return id;
    }

    public int bizId(String bizKey) {
        Map<String, Integer> map = props.getBizMap();
        if (map != null) {
            Integer v = map.get(bizKey);
            if (v != null) return v;
        }
        // fallback to crc32-based hash -> fit into bizBits
        CRC32 crc32 = new CRC32();
        crc32.update(bizKey.getBytes(StandardCharsets.UTF_8));
        long val = crc32.getValue();
        return (int) (val & bizMask);
    }
}
