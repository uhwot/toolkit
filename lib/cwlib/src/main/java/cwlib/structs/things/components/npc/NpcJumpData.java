package cwlib.structs.things.components.npc;

import org.joml.Vector3f;

import cwlib.io.Serializable;
import cwlib.io.gson.GsonRevision;
import cwlib.io.serializer.Serializer;

public class NpcJumpData implements Serializable
{
    public static final int BASE_ALLOCATION_SIZE = 0x50;

    public float a, b, c;
    public Vector3f min, max;

    @GsonRevision(min = 0x273)
    public boolean flipped;

    @GsonRevision(min = 0x273)
    public NpcMoveCmd[] commandList;

    @GsonRevision(min = 0x273)
    public Vector3f apex;

    @Override
    public void serialize(Serializer serializer)
    {
        int version = serializer.getRevision().getVersion();

        a = serializer.f32(a);
        b = serializer.f32(b);
        c = serializer.f32(c);

        min = serializer.v3(min);
        max = serializer.v3(max);

        if (version > 0x272)
        {
            flipped = serializer.bool(flipped);
            commandList = serializer.array(commandList, NpcMoveCmd.class);
            apex = serializer.v3(apex);
        }
    }

    @Override
    public int getAllocatedSize()
    {
        int size = NpcJumpData.BASE_ALLOCATION_SIZE;
        if (this.commandList != null)
            size += (this.commandList.length * NpcMoveCmd.BASE_ALLOCATION_SIZE);
        return size;
    }
}
