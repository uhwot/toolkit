package cwlib.io.gson;

import com.google.gson.*;
import cwlib.types.mods.patches.ModPatch;
import cwlib.types.mods.patches.TranslationPatch;

import java.lang.reflect.Type;

public class PatchSerializer implements JsonDeserializer<ModPatch>, JsonSerializer<ModPatch>
{
    @Override
    public ModPatch deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
    throws JsonParseException
    {
        String patchType = je.getAsJsonObject().get("type").getAsString().toLowerCase();
        switch (patchType)
        {
            case "translation":
                return jdc.deserialize(je, TranslationPatch.class);
            default:
                return null;
        }
    }

    @Override
    public JsonElement serialize(ModPatch patch, Type type, JsonSerializationContext jsc)
    {
        switch (patch.getType())
        {
            case TRANSLATION:
            {
                return jsc.serialize(patch, TranslationPatch.class);
            }
            default:
                return null;
        }
    }
}
