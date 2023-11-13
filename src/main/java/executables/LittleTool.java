package executables;

import cwlib.enums.ResourceType;
import cwlib.resources.RLocalProfile;
import cwlib.singleton.ResourceSystem;
import cwlib.types.Resource;
import cwlib.types.archives.SaveArchive;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.SHA1;
import cwlib.types.data.WrappedResource;
import cwlib.util.FileIO;
import cwlib.util.GsonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LittleTool {
    public static void main(String[] args) throws IOException {
        ResourceSystem.LOG_LEVEL = Integer.MAX_VALUE;
        if (args.length == 0) {
            System.out.println("specify mode (import, export)");
            return;
        }

        String mode = args[0];

        switch (mode) {
            case "export":
                if (args.length != 3) {
                    System.out.println("exports to directory");
                    System.out.println("args: export <fart> <output directory>");
                    return;
                }
                exporter(args[1], args[2]);
                break;
            
            case "import":
                if (args.length != 3) {
                    System.out.println("imports to littlefart");
                    System.out.println("args: import <input directory> <output fart>");
                    return;
                }
                importer(args[1], args[2]);
                break;
        
            default:
                System.out.println("invalid mode, modes are import, export");
                break;
        }
    }

    private static void exporter(String fart, String outputDir) throws FileNotFoundException, IOException {
        SaveArchive archive = new SaveArchive(new File(fart));

        Resource profileRes = new Resource(archive.extract(archive.getKey().getRootHash()));
        GsonUtils.REVISION = profileRes.getRevision();

        // local
        WrappedResource wrapper = new WrappedResource(profileRes);
        FileIO.write(wrapper.toJSON(), Paths.get(outputDir, "local.json").toString());

        RLocalProfile profile = profileRes.loadResource(RLocalProfile.class);

        // synced
        Resource syncedRes = new Resource(archive.extract(profile.syncedProfile.getSHA1()));
        WrappedResource syncedWrapper = new WrappedResource(syncedRes);
        FileIO.write(syncedWrapper.toJSON(), Paths.get(outputDir, "synced.json").toString());

        ResourceDescriptor[] resDescs = {profile.saveIcon, profile.avatarIcon, profile.podLevel};
        String[] resNames = {"save.png", "avatar.tex", "pod.bin"};

        for (int i = 0; i < resDescs.length; i++) {
            ResourceDescriptor resDesc = resDescs[i];
            if (resDesc != null && resDesc.isHash()) {
                byte[] data = archive.extract(resDesc.getSHA1());
                FileIO.write(data, Paths.get(outputDir, resNames[i]).toString());
            }
        }

        if (profile.podLevel.isHash()) {
            Resource podLvl = new Resource(archive.extract(profile.podLevel.getSHA1()));
            for (ResourceDescriptor resDesc : podLvl.getDependencies()) {
                if (resDesc.isHash()) {
                    byte[] data = archive.extract(resDesc.getSHA1());
                    FileIO.write(data, Paths.get(outputDir, "pod_resources", resDesc.getSHA1().toString()).toString());
                }
            }
        }

        for (ResourceDescriptor resDesc : syncedRes.getDependencies()) {
            if (resDesc.isHash()) {
                byte[] data = archive.extract(resDesc.getSHA1());
                FileIO.write(data, Paths.get(outputDir, "synced_resources", resDesc.getSHA1().toString()).toString());
            }
        }

        Files.copy(Path.of(fart), Paths.get(outputDir, "littlefart_bkp"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void importer(String inputDir, String outFart) throws FileNotFoundException, IOException {
        SaveArchive archive = new SaveArchive(Paths.get(inputDir, "littlefart_bkp").toFile());

        WrappedResource localWrapper = GsonUtils.fromJSON(
            FileIO.readString(Paths.get(inputDir, "local.json")),
            WrappedResource.class
        );

        WrappedResource syncedWrapper = GsonUtils.fromJSON(
            FileIO.readString(Paths.get(inputDir, "synced.json")),
            WrappedResource.class
        );

        RLocalProfile profile = (RLocalProfile) localWrapper.resource;
        SHA1 syncedHash = archive.add(syncedWrapper.build());

        profile.syncedProfile = new ResourceDescriptor(syncedHash, ResourceType.SYNCED_PROFILE);

        profile.saveIcon = writeResource(archive, inputDir, "save.png", ResourceType.TEXTURE);
        profile.avatarIcon = writeResource(archive, inputDir, "avatar.tex", ResourceType.TEXTURE);
        profile.podLevel = writeResource(archive, inputDir, "pod.bin", ResourceType.LEVEL);

        if (profile.podLevel != null) {
            Resource podLvl = new Resource(archive.extract(profile.podLevel.getSHA1()));
            for (ResourceDescriptor resDesc : podLvl.getDependencies()) {
                if (resDesc.isHash()) {
                    byte[] data = FileIO.read(Paths.get(inputDir, "pod_resources", resDesc.getSHA1().toString()).toString());
                    archive.add(data);
                }
            }
        }

        Resource syncedRes = new Resource(archive.extract(profile.syncedProfile.getSHA1()));
        for (ResourceDescriptor resDesc : syncedRes.getDependencies()) {
            if (resDesc.isHash()) {
                byte[] data = FileIO.read(Paths.get(inputDir, "synced_resources", resDesc.getSHA1().toString()).toString());
                archive.add(data);
            }
        }

        localWrapper.resource = profile;

        archive.getKey().setRootHash(archive.add(localWrapper.build()));
        archive.save(outFart);
    }

    private static ResourceDescriptor writeResource(SaveArchive archive, String inputDir, String file, ResourceType type) {
        byte[] data = FileIO.read(Paths.get(inputDir, file).toString());
        if (data == null) {
            System.out.printf("%s is missing, writing as null\n", file);
            return null;
        }
        return new ResourceDescriptor(archive.add(data), type);
    }
}
