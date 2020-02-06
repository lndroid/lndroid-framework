package org.lndroid.framework.room;

import java.util.List;

import org.lndroid.framework.WalletData;

class RoomUtils {

    static <Data extends RoomData.RoomEntityBase> void sortPage (List<Data> items, List<Long> ids) {
        if (items.size() != ids.size ())
            throw new RuntimeException("Sort page error");

        // since our expected lists are small (100 items max),
        // this O(n^2) shoould perform better than
        // other more complex and scalable solutions
        for(int i = 0; i < ids.size(); i++) {
            long id = ids.get(i);
            for(int j = i + 1; j < items.size(); j++) {
                if (items.get(j).id_ == id) {
                    // swap
                    Data t = items.get(i);
                    items.set(i, items.get(j));
                    items.set(j, t);
                    break;
                }
            }
        }
    }

    static int preparePageIds(long[] ids, WalletData.ListPage page, List<Long> pageIds) {

        // seek ItemKeyed cursor position
        int fromPos = 0;
        int tillPos = ids.length;
        if (page.afterId() != 0) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == page.afterId()) {
                    fromPos = i + 1; // after!
                    tillPos = fromPos + page.count();
                    break;
                }
            }
        } else if (page.beforeId() != 0) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == page.beforeId()) {
                    fromPos = i - 1 - page.count();
                    tillPos = fromPos + page.count();
                    break;
                }
            }
        } else if (page.aroundId() != 0) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == page.aroundId()) {
                    fromPos = i - 1 - page.count();
                    tillPos = fromPos + page.count() * 2 + 1;
                    break;
                }
            }
        }
        if (fromPos < 0)
            fromPos = 0;

        // get page.count ids after page.afterId
        for (int i = fromPos; i < ids.length && i < tillPos; i++) {
            pageIds.add(ids[i]);
        }

        return fromPos;
    }
}
