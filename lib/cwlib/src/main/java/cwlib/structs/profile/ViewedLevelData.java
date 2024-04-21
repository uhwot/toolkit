package cwlib.structs.profile;

import cwlib.io.Serializable;
import cwlib.io.serializer.Serializer;
import cwlib.structs.slot.SlotID;

public class ViewedLevelData implements Serializable
{
      public static final int BASE_ALLOCATION_SIZE = 0x30;

      public SlotID slotID;
      public int lastReviewCount;
      public int lastCommentCount;
      public int lastPhotoCount;
      public int lastAuthorPhotoCount;
      public long lastStreamEventTimestamp;
      public long lastViewedTimestamp;

      @Override
      public void serialize(Serializer serializer)
      {
            slotID = serializer.struct(slotID, SlotID.class);
            lastReviewCount = serializer.i32(lastReviewCount);
            lastCommentCount = serializer.i32(lastCommentCount);
            lastPhotoCount = serializer.i32(lastPhotoCount);
            lastAuthorPhotoCount = serializer.i32(lastAuthorPhotoCount);
            lastStreamEventTimestamp = serializer.i64(lastStreamEventTimestamp);
            lastViewedTimestamp = serializer.i64(lastViewedTimestamp);
      }

      @Override
      public int getAllocatedSize()
      {
            return ViewedLevelData.BASE_ALLOCATION_SIZE;
      }
}
