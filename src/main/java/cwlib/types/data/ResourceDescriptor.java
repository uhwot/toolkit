package cwlib.types.data;

import cwlib.enums.ResourceFlags;
import cwlib.enums.ResourceType;
import cwlib.util.Strings;

/**
 * Represents references to resources in either
 * the UGC/Profile caches and data archives,
 * or the FileDB.
 */
public final class ResourceDescriptor {
    private final ResourceType type;
    private final GUID guid;
    private final SHA1 sha1;
    private transient int flags;

    /**
     * Constructs a ResourceReference from parsed string and type.
     * @param resource
     * @param type
     */
    public ResourceDescriptor(String resource, ResourceType type) {
        if (Strings.isGUID(resource)) {
            this.guid = new GUID(Strings.getLong(resource));
            this.sha1 = null;
        }
        else if (Strings.isSHA1(resource)) {
            this.sha1 = Strings.getSHA1(resource);
            this.guid = null;
        }
        else throw new IllegalArgumentException("Invalid resource reference passed into resource reference!");

        this.type = type;
    }

    /**
     * Constructs a ResourceReference with GUID and type.
     * @param guid Unique identifer of resource
     * @param type Type of resource
     */
    public ResourceDescriptor(long guid, ResourceType type) {
        this.guid = new GUID(guid);
        this.type = type;
        this.sha1 = null;
        this.flags = ResourceFlags.NONE;
    }

    /**
     * Constructs a ResourceReference with GUID and type.
     * @param guid Unique identifer of resource
     * @param type Type of resource
     */
    public ResourceDescriptor(GUID guid, ResourceType type) {
        this.guid = guid;
        this.type = type;
        this.sha1 = null;
        this.flags = ResourceFlags.NONE;
    }

    /**
     * Constructs a ResourceReference with SHA1 and type.
     * @param sha1 SHA1 signature of resource
     * @param type Type of resource
     */
    public ResourceDescriptor(SHA1 sha1, ResourceType type) {
        this.sha1 = sha1;
        this.type = type;
        this.guid = null;
        this.flags = ResourceFlags.NONE;
    }

    public ResourceDescriptor(GUID guid, SHA1 sha1, ResourceType type) {
        this.sha1 = sha1;
        this.type = type;
        this.guid = guid;
        this.flags = ResourceFlags.NONE;
    }

    /**
     * Is this resource a GUID reference?
     * @return Whether or not this resource contains a GUID reference
     */
    public boolean isGUID() { return this.guid != null; }

    /**
     * IS this resource a SHA1 reference?
     * @return Whether or not this resource contains a SHA1 reference
     */
    public boolean isHash() { return this.sha1 != null; }

    public GUID getGUID() { return this.guid; }
    public SHA1 getSHA1() { return this.sha1; }
    public ResourceType getType() { return this.type; }

    public int getFlags() { return this.flags; }
    public void setFlags(int flags) { this.flags = flags; }

    @Override public int hashCode() { return this.toString().hashCode(); }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof ResourceDescriptor)) return false;
        ResourceDescriptor reference = (ResourceDescriptor) other;
        return reference.toString().equals(this.toString());
    }

    @Override public String toString() {
        if (this.sha1 != null && this.guid != null)
            return String.format("%s (%s)", this.sha1, this.guid);
        if (this.sha1 != null) 
            return "h" + this.sha1.toString();
        else if (this.guid != null)
            return this.guid.toString();
        return "null";
    }
}
