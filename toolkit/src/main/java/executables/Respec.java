package executables;

import cwlib.enums.CompressionFlags;
import cwlib.io.Resource;
import cwlib.io.serializer.Serializer;
import cwlib.resources.RPlan;
import cwlib.structs.things.Thing;
import cwlib.types.SerializedResource;
import cwlib.types.data.Revision;
import cwlib.util.FileIO;
import cwlib.util.Strings;

import java.io.File;

public class Respec
{
      public static void main(String[] args)
      {
            if (args.length < 3 || args.length > 4)
            {
                  System.out.println("java -jar reroller.java <resource> <output> <revision> " +
                                     "<descriptor?>");
                  return;
            }

            int head = (int) Strings.getLong(args[2]);
            int branchDescriptor = 0;
            if (args.length == 4)
                  branchDescriptor = (int) Strings.getLong(args[3]);

            byte compressionFlags = CompressionFlags.USE_NO_COMPRESSION;
            if (head >= 0x297 || (head == 0x272 && (branchDescriptor >> 0x10 == 0x4c44) && ((branchDescriptor & 0xffff) > 1)))
                  compressionFlags = CompressionFlags.USE_ALL_COMPRESSION;

            Revision revision = new Revision(head, branchDescriptor);

            if (!new File(args[0]).exists())
            {
                  System.err.println("File doesn't exist!");
                  return;
            }

            Resource compressable = null;
            try
            {
                  SerializedResource resource = new SerializedResource(args[0]);
                  Serializer serializer = resource.getSerializer();
                  Object struct = serializer.struct(null,
                          resource.getResourceType().getCompressable());
                  if (struct instanceof RPlan)
                  {
                        RPlan plan = (RPlan) struct;
                        Thing[] things = null;
                        try { things = plan.getThings(); }
                        catch (Exception ex)
                        {
                              System.out.println("There was an error processing the thing data of" +
                                                 " this " +
                                                 "RPlan!");
                              System.out.println(ex.getMessage());
                              return;
                        }
                        plan.revision = revision;
                        plan.compressionFlags = compressionFlags;
                        plan.setThings(things);
                  }
                  compressable = (Resource) struct;
            }
            catch (Exception ex)
            {
                  System.out.println("There was an error processing this resource!");
                  System.out.println(ex.getMessage());
            }

            FileIO.write(SerializedResource.compress(compressable.build(revision,
                    compressionFlags)), args[1]);
      }
}
