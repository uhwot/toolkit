package cwlib.structs.things.components.shapes;

import cwlib.io.Serializable;
import cwlib.io.gson.GsonRevision;
import cwlib.io.serializer.Serializer;

public class ContactCache implements Serializable
{
    public static final int BASE_ALLOCATION_SIZE = 0x8;

    public Contact[] contacts;
    public boolean contactsSorted;
    @GsonRevision(lbp3 = true, max = 0x46)
    public boolean cacheDirtyButRecomputed;

    @Override
    public void serialize(Serializer serializer)
    {
        contacts = serializer.array(contacts, Contact.class);
        contactsSorted = serializer.bool(contactsSorted);
        if (serializer.getRevision().getSubVersion() < 0x46)
            cacheDirtyButRecomputed = serializer.bool(cacheDirtyButRecomputed);
    }

    @Override
    public int getAllocatedSize()
    {
        int size = BASE_ALLOCATION_SIZE;
        if (this.contacts != null)
            for (Contact contact : this.contacts)
                size += contact.getAllocatedSize();
        return size;
    }
}