package cwlib.structs.mesh;

import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import cwlib.enums.BoneFlag;
import cwlib.io.Serializable;
import cwlib.io.serializer.Serializer;
import cwlib.structs.animation.AnimBone;

/**
 * A joint of an armature for skinned meshes.
 */
public class Bone extends AnimBone {
    public static final int MAX_BONE_NAME_LENGTH = 32;
    public static final int BASE_ALLOCATION_SIZE = AnimBone.BASE_ALLOCATION_SIZE + 0x120;

    private String name;
    public int flags = BoneFlag.NONE;
    public Matrix4f skinPoseMatrix, invSkinPoseMatrix;
    public Vector4f obbMin, obbMax;
    public MeshShapeVertex[] shapeVerts;
    public MeshShapeInfo[] shapeInfos;
    public float shapeMinZ, shapeMaxZ;
    public Vector4f boundBoxMin, boundBoxMax, boundSphere;

    /**
     * Creates an empty bone
     */
    public Bone() {};

    /**
     * Creates an empty named bone
     * @param name Name of the bone
     */
    public Bone(String name) {
        if (name != null && name.length() > MAX_BONE_NAME_LENGTH)
            throw new IllegalArgumentException("Bone name length cannot be more than 32 characters!");
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    @Override public Bone serialize(Serializer serializer, Serializable structure) {
        Bone bone = (structure == null) ? new Bone() : (Bone) structure;

        bone.name = serializer.str(bone.name, MAX_BONE_NAME_LENGTH);
        bone.flags = serializer.i32(bone.flags);

        super.serialize(serializer, structure);
        
        bone.skinPoseMatrix = serializer.m44(bone.skinPoseMatrix);
        bone.invSkinPoseMatrix = serializer.m44(bone.invSkinPoseMatrix);

        bone.obbMin = serializer.v4(bone.obbMin);
        bone.obbMax = serializer.v4(bone.obbMax);

        bone.shapeVerts = serializer.array(bone.shapeVerts, MeshShapeVertex.class);
        bone.shapeInfos = serializer.array(bone.shapeInfos, MeshShapeInfo.class);

        bone.shapeMinZ = serializer.f32(bone.shapeMinZ);
        bone.shapeMaxZ = serializer.f32(bone.shapeMaxZ);

        bone.boundBoxMin = serializer.v4(bone.boundBoxMin);
        bone.boundBoxMax = serializer.v4(bone.boundBoxMax);
        bone.boundSphere = serializer.v4(bone.boundSphere);

        return bone;
    }

    public String getName() { return this.name; }
    public void setName(String name) {
        if (name != null && name.length() > MAX_BONE_NAME_LENGTH)
            throw new IllegalArgumentException("Bone name cannot be longer than 32 characters!");
        this.name = name;
    }

    /**
     * Attempts to get a bone's name from an animation hash.
     * @param skeleton Skeleton to search for animation hash
     * @param animHash Hash to search for
     * @return Bone's name
     */
    public static String getNameFromHash(Bone[] skeleton, int animHash) {
        if (skeleton == null)
            throw new NullPointerException("Can't get bones from null skeleton!");
        for (Bone bone : skeleton)
            if (bone.animHash == animHash)
                return bone.name;
        return null;
    }

    /**
     * Gets the index of this bone in a skeleton.
     * @param skeleton The skeleton that this bone is from
     * @return The index of this bone if it exists
     */
    public int getIndex(Bone[] skeleton) {
        if (skeleton == null)
            throw new NullPointerException("Can't get bones from null skeleton!");
        for (int i = 0; i < skeleton.length; ++i)
            if (skeleton[i].name.equals(this.name))
                return i;
        return -1;
    }

    /**
     * Gets the children of this bone.
     * @param skeleton The skeleton that this bone is from 
     * @return The children of this bone
     */
    public Bone[] getChildren(Bone[] skeleton) {
        if (skeleton == null)
            throw new NullPointerException("Can't get bones from null skeleton!");
        ArrayList<Bone> bones = new ArrayList<>(skeleton.length);
        int index = this.getIndex(skeleton);
        if (index == -1)
            throw new IllegalArgumentException("This bone doesn't exist in the skeleton provided!");
        for (Bone bone : skeleton) {
            if (bone == this) continue;
            if (bone.parent == index)
                bones.add(bone);
        }
        return bones.toArray(Bone[]::new);
    }

    @Override public int getAllocatedSize() { 
        int size = BASE_ALLOCATION_SIZE;
        if (this.shapeInfos != null)    
            size += this.shapeInfos.length * MeshShapeInfo.BASE_ALLOCATION_SIZE;
        if (this.shapeVerts != null)
            size += this.shapeVerts.length * MeshShapeVertex.BASE_ALLOCATION_SIZE;
        return size; 
    }
}
