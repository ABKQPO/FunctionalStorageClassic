package com.hfstudio.functionalstorage.client.model;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Face;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

public class FaceRotatedModel extends JSONModel {

    private final int[] rotations;
    private int faceIndex;

    public FaceRotatedModel(JSONModel parent) {
        super(parent);
        rotations = new int[elements.stream()
            .mapToInt(
                element -> element.faces()
                    .size())
            .sum()];
        int index = 0;
        for (ModelElement element : elements) {
            for (Face face : element.faces()) {
                rotations[index++] = Math.floorMod(face.rotation() / 90, 4);
            }
        }
    }

    @Override
    public synchronized BakedModel bake(BakeData data) {
        faceIndex = 0;
        return super.bake(data);
    }

    @Override
    protected void bakeSprite(ModelQuadViewMutable quad, String name) {
        int turns = rotations[faceIndex++];
        for (int turn = 0; turn < turns; turn++) {
            float firstU = quad.getTexU(0);
            float firstV = quad.getTexV(0);
            for (int vertex = 0; vertex < 3; vertex++) {
                quad.setTexU(vertex, quad.getTexU(vertex + 1));
                quad.setTexV(vertex, quad.getTexV(vertex + 1));
            }
            quad.setTexU(3, firstU);
            quad.setTexV(3, firstV);
        }
        super.bakeSprite(quad, name);
    }
}
