package ennuo.craftworld.resources;

import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.serializer.Data;
import ennuo.craftworld.types.data.ResourceDescriptor;
import ennuo.craftworld.resources.enums.SerializationMethod;
import ennuo.craftworld.resources.structs.Revision;
import ennuo.craftworld.resources.structs.TextureInfo;
import ennuo.craftworld.serializer.Output;
import ennuo.craftworld.types.FileEntry;
import ennuo.craftworld.utilities.Bytes;
import ennuo.craftworld.utilities.Compressor;
import ennuo.craftworld.utilities.TEA;
import ennuo.toolkit.utilities.Globals;
import java.util.Arrays;

public class Resource {
    public ResourceType type = ResourceType.INVALID;
    public SerializationMethod method = SerializationMethod.UNKNOWN;
    public TextureInfo textureInfo;
    public Revision revision = new Revision(0x132, 0x0000, 0x0000);
    private boolean isCompressed = true;
    public byte compressionFlags = 0;
    public Data handle = null;
    public ResourceDescriptor[] dependencies = new ResourceDescriptor[0];
    
    public Resource(){}
    
    public Resource(Output output) {
        output.shrink();
        this.revision = output.revision;
        this.dependencies = output.dependencies.toArray(
                new ResourceDescriptor[output.dependencies.size()]);
        this.method = SerializationMethod.BINARY;
        this.compressionFlags = output.compressionFlags;
        this.handle = new Data(output.buffer, output.revision);
        this.handle.compressionFlags = output.compressionFlags;
    }
    
    public Resource(String path) {
        this.handle = new Data(path);
        this.process();
    }
    
    public Resource(byte[] data) {
        this.handle = new Data(data);
        this.process();
    }
    
    private void process() {
        if (this.handle == null || this.handle.length < 0xb) return;
        this.type = ResourceType.fromMagic(this.handle.str(3));
        if (this.type == ResourceType.INVALID || this.type == ResourceType.STATIC_MESH) { this.handle.seek(0); return; }
        this.method = SerializationMethod.getValue(this.handle.str(1));
        if (this.method == SerializationMethod.UNKNOWN) { this.handle.seek(0); return; }
        switch (this.method) {
            case BINARY:
            case ENCRYPTED_BINARY:
                this.revision = new Revision(this.handle.i32f());
                this.handle.revision = this.revision;
                int dependencyTableOffset = -1;
                if (this.revision.head >= 0x109) {
                    dependencyTableOffset = this.getDependencies();
                    if (this.revision.head >= 0x189) {
                        if (this.revision.head >= 0x271) { 
                            // NOTE(Abz): Were they actually added on 0x27a, but how can it be on 0x272 then?!
                            this.revision.branchID = this.handle.i16();
                            this.revision.branchRevision = this.handle.i16();
                            this.handle.revision = revision;
                        }
                        if (this.revision.head >= 0x297 || (this.revision.head == 0x272 && this.revision.branchID != 0)) {
                            this.compressionFlags = this.handle.i8();
                            this.handle.compressionFlags = this.compressionFlags;
                        }
                        this.isCompressed = this.handle.bool();
                    }
                }
                
                if (this.method == SerializationMethod.ENCRYPTED_BINARY) {
                    int size = this.handle.i32f();
                    this.handle.setData(TEA.decrypt(this.handle.bytes(size)));
                }
                
                if (this.isCompressed)
                    Compressor.decompressData(this.handle);
                else if (dependencyTableOffset != -1)
                    this.handle.setData(this.handle.bytes(dependencyTableOffset - this.handle.offset));
                else this.handle.setData(this.handle.bytes(this.handle.length - this.handle.offset));
                
                break;
            case TEXT:
                this.handle.setData(this.handle.bytes(this.handle.length - 4));
                break;
            case TEXTURE:
            case GXT_SIMPLE:
            case GXT_EXTENDED:
                if (this.type != ResourceType.TEXTURE)
                    this.textureInfo = new TextureInfo(this.handle, this.method);
                Compressor.decompressData(this.handle);
                break;
        }
    }
    
    public int registerDependencies(boolean recursive) {
        if (this.method != SerializationMethod.BINARY) return 0;
        int missingDependencies = 0;
        for (ResourceDescriptor dependency : this.dependencies) {
            FileEntry entry = Globals.findEntry(dependency);
            if (entry == null) {
                missingDependencies++;
                continue;
            }
            if (recursive && this.type != ResourceType.SCRIPT) {
                byte[] data = Globals.extractFile(dependency);
                if (data != null) {
                    Resource resource = new Resource(data);
                    if (resource.method == SerializationMethod.BINARY) {
                        entry.hasMissingDependencies = resource.registerDependencies(recursive) != 0;
                        entry.canReplaceDecompressed = true;
                        entry.dependencies = resource.dependencies; 
                    }
                }
            }
        }
        return missingDependencies;
    }
    
    public void replaceDependency(int index, ResourceDescriptor newDescriptor) {
        ResourceDescriptor oldDescriptor = this.dependencies[index];
        
        byte[] oldDescBuffer = Bytes.createResourceReference(oldDescriptor, this.revision, this.compressionFlags);
        byte[] newDescBuffer = Bytes.createResourceReference(newDescriptor, this.revision, this.compressionFlags);
        
        if (Arrays.equals(oldDescBuffer, newDescBuffer)) return;
        
        if (this.type == ResourceType.PLAN) {
            Plan plan = new Plan(this);
            Data thingData = new Data(plan.thingData, this.revision);
            Bytes.ReplaceAll(thingData, oldDescBuffer, newDescBuffer);
            plan.thingData = thingData.data;
            this.handle.setData(plan.build(false));
        }
        Bytes.ReplaceAll(this.handle, oldDescBuffer, newDescBuffer);
        
        this.dependencies[index] = newDescriptor;
    }
    
    private int getDependencies() {
        int dependencyTableOffset = this.handle.i32f();
        int originalOffset = this.handle.offset;
        this.handle.offset = dependencyTableOffset;
        
        this.dependencies = new ResourceDescriptor[this.handle.i32f()];
        for (int i = 0; i < this.dependencies.length; ++i) {
            ResourceDescriptor descriptor = new ResourceDescriptor();
            switch (this.handle.i8()) {
                case 1:
                    descriptor.hash = this.handle.bytes(0x14);
                    break;
                case 2:
                    descriptor.GUID = this.handle.u32f();
                    break;
            }
            descriptor.type = ResourceType.fromType(this.handle.i32f());
            this.dependencies[i] = descriptor;
        }
        
        this.handle.offset = originalOffset;
        
        return dependencyTableOffset;
    }
    
    public static byte[] compressToResource(byte[] data, Revision revision, int compressionFlags, ResourceType type, ResourceDescriptor[] dependencies) {
        Resource resource = new Resource();
        resource.handle = new Data(data, revision);
        resource.revision = revision;
        resource.type = type;
        if (resource.type == ResourceType.LOCAL_PROFILE)
            resource.method = SerializationMethod.ENCRYPTED_BINARY;
        else resource.method = SerializationMethod.BINARY;
        resource.dependencies = dependencies;
        return resource.compressToResource();
    }
    
    public static byte[] compressToResource(Output data, ResourceType type) {
        Resource resource = new Resource(data);
        resource.type = type;
        if (type == ResourceType.LOCAL_PROFILE)
            resource.method = SerializationMethod.ENCRYPTED_BINARY;
        return resource.compressToResource();
    }
    
    public byte[] compressToResource() {
        if (this.type == ResourceType.STATIC_MESH) return this.handle.data;
        Output output = new Output(this.dependencies.length * 0x1c + this.handle.length + 0x20);
        
        if (this.method == SerializationMethod.TEXT) {
            output.str(this.type.header + this.method.value + '\n');
            output.bytes(this.handle.data);
            output.shrink();
            return output.buffer;
        }
        
        output.str(this.type.header + this.method.value);
        output.i32f(this.revision.head);
        if (this.revision.head >= 0x109) {
            output.i32f(0); // Dummy value for dependency table offset.
            if (this.revision.head >= 0x189) {
                if (this.revision.head >= 0x271) {
                    output.i16(this.revision.branchID);
                    output.i16(this.revision.branchRevision);
                }
                if (this.revision.head >= 0x297 || (this.revision.head == 0x272 && this.revision.branchID != 0))
                    output.i8(this.compressionFlags);
                output.bool(this.isCompressed);
            }
            
            byte[] data = this.handle.data;
            if (this.isCompressed) data = Compressor.getCompressedStream(this.handle.data);
            if (this.method == SerializationMethod.ENCRYPTED_BINARY) {
                data = TEA.encrypt(data);
                output.i32f(data.length);
            }
            output.bytes(data);
            
            int dependencyTableOffset = output.offset;
            output.offset = 0x8;
            output.i32f(dependencyTableOffset);
            output.offset = dependencyTableOffset;
            
            output.i32f(this.dependencies.length);
            for (ResourceDescriptor dependency : this.dependencies) {
                if (dependency.GUID != -1) {
                    output.i8((byte) 2);
                    output.u32f(dependency.GUID);
                } else if (dependency.hash != null) {
                    output.i8((byte) 1);
                    output.bytes(dependency.hash);
                }
                output.i32f(dependency.type.value);
            }
        }
        
        output.shrink();
        return output.buffer;
    }
}
