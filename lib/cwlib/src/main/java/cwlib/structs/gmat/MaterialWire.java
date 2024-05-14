package cwlib.structs.gmat;

import cwlib.io.Serializable;
import cwlib.io.serializer.Serializer;

/**
 * A wire that connects two boxes on the shader graph,
 * used by Media Molecule in their shader editor.
 */
public class MaterialWire implements Serializable
{
    public static final int BASE_ALLOCATION_SIZE = 0x10;
    public static final int SWIZZLE_ELEMENT_COUNT = 5;

    public int boxFrom, boxTo;
    public byte portFrom, portTo;
    public byte[] swizzle = new byte[SWIZZLE_ELEMENT_COUNT];

    /**
     * Empty constructor for serialization.
     */
    public MaterialWire() { }

    /**
     * Creates a connection between two boxes.
     */
    public MaterialWire(int boxFrom, int boxTo, int portFrom, int portTo)
    {
        this.boxFrom = boxFrom;
        this.boxTo = boxTo;
        this.portFrom = (byte) portFrom;
        this.portTo = (byte) portTo;
    }

    @Override
    public void serialize(Serializer serializer)
    {
        boxFrom = serializer.s32(boxFrom);
        boxTo = serializer.s32(boxTo);
        portFrom = serializer.i8(portFrom);
        portTo = serializer.i8(portTo);
        for (int i = 0; i < SWIZZLE_ELEMENT_COUNT; ++i)
            swizzle[i] = serializer.i8(swizzle[i]);
    }

    @Override
    public int getAllocatedSize()
    {
        return MaterialWire.BASE_ALLOCATION_SIZE;
    }
}
