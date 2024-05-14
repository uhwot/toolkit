package cwlib.util;

import cwlib.enums.CellGcmEnumForGtf;
import cwlib.io.streams.MemoryOutputStream;
import cwlib.structs.texture.CellGcmTexture;

public class DDS
{
    public static int DDS_HEADER_FLAGS_TEXTURE = 0x00001007;
    public static int DDS_HEADER_FLAGS_MIPMAP = 0x00020000;

    public static int DDS_SURFACE_FLAGS_COMPLEX = 0x00000008;
    public static int DDS_SURFACE_FLAGS_TEXTURE = 0x00001000;
    public static int DDS_SURFACE_FLAGS_MIPMAP = 0x00400000;

    public static int DDS_SURFACE_FLAGS_CUBEMAP = 0x00000200;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEX = 0x00000400;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEX = 0x00000800;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEY = 0x00001000;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEY = 0x00002000;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEZ = 0x00004000;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEZ = 0x00008000;
    public static int DDS_SURFACE_FLAGS_CUBEMAP_ALL_FACES = 0x0000FC00;
    public static int DDS_SURFACE_FLAGS_VOLUME = 0x00200000;

    public static int DDS_FOURCC = 0x4;
    public static int DDS_RGB = 0x40;
    public static int DDS_RGBA = 0x41;
    public static int DDS_LUMINANCE = 0x00020000;
    public static int DDS_LUMINANCEA = 0x00020001;

    public static int[] DDSPF_DXT1 = { 0x20, DDS_FOURCC, 0x31545844, 0, 0, 0, 0, 0 };
    public static int[] DDSPF_DXT3 = { 0x20, DDS_FOURCC, 0x33545844, 0, 0, 0, 0, 0 };
    public static int[] DDSPF_DXT5 = { 0x20, DDS_FOURCC, 0x35545844, 0, 0, 0, 0, 0 };
    public static int[] DDSPF_A8R8G8B8 = { 0x20, DDS_RGBA, 0, 32, 0x00ff0000, 0x0000ff00,
        0x000000ff, 0xff000000 };
    public static int[] DDSPF_R5G6B5 = { 0x20, DDS_RGB, 0, 16, 0x0000f800, 0x000007e0, 0x0000001f,
        0x00000000 };
    public static int[] DDSPF_A4R4G4B4 = { 0x20, DDS_RGBA, 0, 16, 0x00000f00, 0x000000f0,
        0x0000000f, 0x0000f000 };
    public static int[] DDSPF_A16B16G16R16F = { 0x20, DDS_FOURCC, 113, 0, 0, 0, 0, 0 };
    public static int[] DDSPF_A8L8 = { 0x20, DDS_LUMINANCEA, 0, 16, 0xff, 0, 0, 0xff00 };
    public static int[] DDSPF_L8 = { 0x20, DDS_LUMINANCE, 0, 8, 0xff, 0, 0, 0 };
    public static int[] DDSPF_A1R5G5B5 = { 0x20, DDS_RGBA, 0, 16, 0x00007c00, 0x000003e0,
        0x0000001f, 0x00008000 };

    /**
     * Generates a DDS header.
     *
     * @param texture Texture header data
     * @return Generated DDS header.
     */
    public static byte[] getDDSHeader(CellGcmTexture texture)
    {
        return DDS.getDDSHeader(
            texture.getFormat(),
            texture.getWidth(),
            texture.getHeight(),
            texture.getMipCount(),
            texture.isCubemap()
        );
    }

    /**
     * Generates a DDS header.
     *
     * @param format DDS format for PS3
     * @param width  Width of texture
     * @param height Height of texture
     * @param mips   Mip level count
     * @return Generated DDS header
     */
    public static byte[] getDDSHeader(CellGcmEnumForGtf format, int width, int height, int mips,
                                      boolean cubemap)
    {
        // For details on the DDS header structure, see:
        // https://docs.microsoft.com/en-us/windows/win32/direct3ddds/dds-header

        MemoryOutputStream header = new MemoryOutputStream(0x80);
        header.setLittleEndian(true);

        header.str("DDS ", 4);
        header.u32(0x7C); // dwSize
        header.u32(DDS.DDS_HEADER_FLAGS_TEXTURE | ((mips != 1) ? DDS.DDS_HEADER_FLAGS_MIPMAP : 0));

        header.u32(height);
        header.u32(width);
        header.u32(0); // dwPitchOrLinearSize
        header.u32(0); // dwDepth
        header.u32(mips);
        for (int i = 0; i < 11; ++i)
            header.u32(0); // dwReserved[11]

        // DDS_PIXELFORMAT
        int[] pixelFormat = null;
        switch (format)
        {
            case B8:
                pixelFormat = DDS.DDSPF_L8;
                break;
            case A1R5G5B5:
                pixelFormat = DDS.DDSPF_A1R5G5B5;
                break;
            case A4R4G4B4:
                pixelFormat = DDS.DDSPF_A4R4G4B4;
                break;
            case R5G6B5:
                pixelFormat = DDS.DDSPF_R5G6B5;
                break;
            case A8R8G8B8:
                pixelFormat = DDS.DDSPF_A8R8G8B8;
                break;
            case DXT1:
                pixelFormat = DDS.DDSPF_DXT1;
                break;
            case DXT3:
                pixelFormat = DDS.DDSPF_DXT3;
                break;
            case DXT5:
                pixelFormat = DDS.DDSPF_DXT5;
                break;
            default:
                throw new RuntimeException("Unknown or unimplemented DDS Type!");
        }
        for (int value : pixelFormat)
            header.u32(value);

        int caps1 = DDS.DDS_SURFACE_FLAGS_TEXTURE;
        int caps2 = 0;

        if (mips != 1)
        {
            caps1 |= DDS_SURFACE_FLAGS_MIPMAP;
            caps1 |= DDS_SURFACE_FLAGS_COMPLEX;
        }

        if (cubemap)
        {
            caps1 |= DDS_SURFACE_FLAGS_COMPLEX;

            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP;

            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEX;
            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEX;

            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEY;
            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEY;

            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_POSITIVEZ;
            caps2 |= DDS_SURFACE_FLAGS_CUBEMAP_NEGATIVEZ;
        }

        header.u32(caps1);
        header.u32(caps2);

        for (int i = 0; i < 3; ++i)
            header.u32(0); // dwReserved

        return header.getBuffer();
    }
    
    public static int getMortonNumber(int x, int y, int width, int height)
    {
        int logW = 31 - Integer.numberOfLeadingZeros(width);
        int logH = 31 - Integer.numberOfLeadingZeros(height);

        int d = Integer.min(logW, logH);
        int m = 0;

        for (int i = 0; i < d; ++i)
            m |= ((x & (1 << i)) << (i + 1)) | ((y & (1 << i)) << i);

        if (width < height)
            m |= ((y & ~(width - 1)) << d);
        else
            m |= ((x & ~(height - 1)) << d);

        return m;
    }

    /**
     * Unswizzles compressed DXT1/5 pixel data for PSVita GXT textures.
     *
     * @param texture Texture metadata
     * @param swizzled Swizzled texture data
     * @return Unswizzled texture data
     */
    public static byte[] unswizzleGxtCompressed(CellGcmTexture texture, byte[] swizzled)
    {
        byte[] pixels = new byte[swizzled.length];

        int blockWidth = 4, blockHeight = 4;
        int bpp = 4;
        if (texture.getFormat().equals(CellGcmEnumForGtf.DXT5))
            bpp = 8;

        int base = 0;

        int width = Integer.max(texture.getWidth(), blockWidth);
        int height = Integer.max(texture.getHeight(), blockHeight);

        int log2width = 1 << (31 - Integer.numberOfLeadingZeros(width + (width - 1)));
        int log2height = 1 << (31 - Integer.numberOfLeadingZeros(height + (height - 1)));

        for (int i = 0; i < texture.getMipCount(); ++i)
        {
            int w = ((width + blockWidth - 1) / blockWidth);
            int h = ((height + blockHeight - 1) / blockHeight);
            int blockSize = bpp * blockWidth * blockHeight;

            int log2w = 1 << (31 - Integer.numberOfLeadingZeros(w + (w - 1)));
            int log2h = 1 << (31 - Integer.numberOfLeadingZeros(h + (h - 1)));

            int mx = getMortonNumber(log2w - 1, 0, log2w, log2h);
            int my = getMortonNumber(0, log2h - 1, log2w, log2h);

            int pixelSize = blockSize / 8;

            int oy = 0, tgt = base;
            for (int y = 0; y < h; ++y)
            {
                int ox = 0;
                for (int x = 0; x < w; ++x)
                {
                    int offset = base + ((ox + oy) * pixelSize);
                    System.arraycopy(swizzled, offset, pixels, tgt, pixelSize);
                    tgt += pixelSize;
                    ox = (ox - mx) & mx;
                }
                oy = (oy - my) & my;
            }

            base += ((bpp * log2width * log2height) / 8);

            width = width > blockWidth ? width / 2 : blockWidth;
            height = height > blockHeight ? height / 2 : blockHeight;

            log2width = log2width > blockWidth ? log2width / 2 : blockWidth;
            log2height = log2height > blockHeight ? log2height / 2 : blockHeight;
        }

        return pixels;
    }

    /**
     * Unswizzles pixel data for PS3 GTF textures.
     *
     * @param texture Texture metadata
     * @param in Texture data to swizzle
     * @param isInputSwizzled Whether input data is swizzled
     * @return Unswizzled texture data
     */
    public static byte[] convertSwizzleGtf(CellGcmTexture texture, byte[] in, boolean isInputSwizzled)
    {
        // NOTE(Aidan): For original source, see:
        // https://github.com/RPCS3/rpcs3/blob/3d49976b3c0f2d2fe5fbd9dba0419c13b389c6ba/rpcs3/Emu/RSX/rsx_utils.h

        CellGcmEnumForGtf format = texture.getFormat();
        if (format.isDXT()) return in;

        int width = texture.getWidth();
        int height = texture.getHeight();
        int bpp = format.getDepth();

        byte[] out = new byte[in.length];
        int textureLayoutOffset = 0;

        for (int i = 0; i < texture.getMipCount(); ++i)
        {
            int log2width = (int) (Math.log(width) / Math.log(2));
            int log2height = (int) (Math.log(height) / Math.log(2));
    
            int xMask = 0x55555555;
            int yMask = 0xAAAAAAAA;
    
            int limitMask = (log2width < log2height) ? log2width : log2height;
            limitMask = 1 << (limitMask << 1);
    
    
            xMask = (xMask | ~(limitMask - 1));
            yMask = (yMask & (limitMask - 1));
    
            int offsetY = 0, offsetX = 0, offsetX0 = 0, yIncr = limitMask, adv = width;

            if (isInputSwizzled)
            {
                for (int y = 0; y < height; ++y)
                {
                    offsetX = offsetX0;
                    for (int x = 0; x < width; ++x)
                    {
                        System.arraycopy(
                            in, 
                            textureLayoutOffset + ((offsetY + offsetX) * bpp), 
                            out, 
                            textureLayoutOffset + (((y * adv) + x) * bpp), 
                            bpp
                        );
                        
                        offsetX = (offsetX - xMask) & xMask;
                    }
                    offsetY = (offsetY - yMask) & yMask;
                    if (offsetY == 0) offsetX0 += yIncr;
                }
            }
            else
            {
                for (int y = 0; y < height; ++y)
                {
                    offsetX = offsetX0;
                    for (int x = 0; x < width; ++x)
                    {
                        System.arraycopy(
                            in, 
                            textureLayoutOffset + (((y * adv) + x) * bpp),
                            out,
                            textureLayoutOffset + ((offsetY + offsetX) * bpp),  
                            bpp
                        );
                        
                        offsetX = (offsetX - xMask) & xMask;
                    }
                    offsetY = (offsetY - yMask) & yMask;
                    if (offsetY == 0) offsetX0 += yIncr;
                }
            }


            textureLayoutOffset += format.getImageSize(width, height);
            width >>>= 1;
            height >>>= 1;

            if (width == 0 && height == 0) break;
            if (width == 0) width = 1;
            if (height == 0) height = 1;
        }

        // Swap the endianness around, PS3 data is in big endian
        if (bpp == 2)
        {
            for (int i = 0; i < out.length; i += 2)
            {
                byte tmp = out[i];
                out[i] = out[i + 1];
                out[i + 1] = tmp;
            }
        }

        if (bpp == 4)
        {
            for (int i = 0; i < out.length; i += 4)
            {
                byte tmp = out[i];
                out[i] = out[i + 3];
                out[i + 3] = tmp;

                tmp = out[i + 1];
                out[i + 1] = out[i + 2];
                out[i + 2] = tmp;
            }
        }

        return out;
    }
}
