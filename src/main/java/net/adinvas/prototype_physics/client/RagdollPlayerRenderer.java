package net.adinvas.prototype_physics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.adinvas.prototype_physics.PrototypePhysics;
import net.adinvas.prototype_physics.RagdollPart;
import net.adinvas.prototype_physics.RagdollTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RagdollPlayerRenderer {
    private static final Minecraft mc = Minecraft.getInstance();

    static final Vector3f[] headoff = new Vector3f[]{
            new Vector3f(0.0f, 0.0f, 0.0f),
            new Vector3f(0.0f, -6.0f/16, 0.0f)
    };
    static final Vector3f[] torsoff = new Vector3f[]{
            new Vector3f(0.0f, 0.0f, 0.0f),
            new Vector3f(0.0f, -6.0f/16, 0.0f)
    };
    static final Vector3f[] larmoff = new Vector3f[]{
            new Vector3f(3.8F, 4.0f, 0.0f),
            new Vector3f(1F/16, -7.0f/16, 0.0f)
    };
    static final Vector3f[] rarmoff = new Vector3f[]{
            new Vector3f(-3.8F, 4.0f, 0.0f),
            new Vector3f(-1F/16, -7.0f/16, 0.0f)
    };
    static final Vector3f[] llegoff = new Vector3f[]{
            new Vector3f(1.9f, 5.5f, 0.0f),
            new Vector3f(0.0f, 0f/16, 0.0f)
    };
    static final Vector3f[] rlegoff = new Vector3f[]{
            new Vector3f(-1.9f, 5.5f, 0.0f),
            new Vector3f(0.00f, 0f/16, 0.0f),
    };

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        RagdollManager.ClientRagdoll rag = RagdollManager.get(player.getId());
        if (player.isDeadOrDying()){
            RagdollManager.remove(player.getId());
            return;
        }
        if (rag == null || !rag.isActive()) return;
        event.setCanceled(true); 

        PlayerRenderer renderer = event.getRenderer();
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        float partial = event.getPartialTick();
        RagdollTransform torso = rag.getPartInterpolated(RagdollPart.TORSO, partial);
        RagdollTransform head = rag.getPartInterpolated(RagdollPart.HEAD, partial);
        RagdollTransform larm = rag.getPartInterpolated(RagdollPart.LEFT_ARM, partial);
        RagdollTransform rarm = rag.getPartInterpolated(RagdollPart.RIGHT_ARM, partial);
        RagdollTransform lleg = rag.getPartInterpolated(RagdollPart.LEFT_LEG, partial);
        RagdollTransform rleg = rag.getPartInterpolated(RagdollPart.RIGHT_LEG, partial);
        if (torso == null) return;

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skin));
        int light = event.getPackedLight();

        // Base Layer + Second Layer of the skin
        
        // Torso / Jacket
        renderRagdollPart(poseStack, vertexConsumer, model.body, torso, torso, torsoff, light);
        if (player.isModelPartShown(PlayerModelPart.JACKET)) {
            renderRagdollPart(poseStack, vertexConsumer, model.jacket, torso, torso, torsoff, light);
        }

        // Head / Hat
        renderRagdollPart(poseStack, vertexConsumer, model.head, head, torso, headoff, light);
        if (player.isModelPartShown(PlayerModelPart.HAT)) {
            renderRagdollPart(poseStack, vertexConsumer, model.hat, head, torso, headoff, light);
        }

        // Left Leg / Left Pants
        renderRagdollPart(poseStack, vertexConsumer, model.leftLeg, lleg, torso, llegoff, light);
        if (player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)) {
            renderRagdollPart(poseStack, vertexConsumer, model.leftPants, lleg, torso, llegoff, light);
        }

        // Right Leg / Right Pants
        renderRagdollPart(poseStack, vertexConsumer, model.rightLeg, rleg, torso, rlegoff, light);
        if (player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)) {
            renderRagdollPart(poseStack, vertexConsumer, model.rightPants, rleg, torso, rlegoff, light);
        }

        // Left Arm / Left Sleeve
        renderRagdollPart(poseStack, vertexConsumer, model.leftArm, larm, torso, larmoff, light);
        if (player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)) {
            renderRagdollPart(poseStack, vertexConsumer, model.leftSleeve, larm, torso, larmoff, light);
        }

        // Right Arm / Right Sleeve
        renderRagdollPart(poseStack, vertexConsumer, model.rightArm, rarm, torso, rarmoff, light);
        if (player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)) {
            renderRagdollPart(poseStack, vertexConsumer, model.rightSleeve, rarm, torso, rarmoff, light);
        }
    }

    public static void renderRagdollPart(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            ModelPart part,
            RagdollTransform transform,
            RagdollTransform torso,
            Vector3f[] pivot,
            int light
    ) {
        if (transform == null) return;
        part.setPos(pivot[0].x, pivot[0].y, pivot[0].z);
        poseStack.pushPose();

        Quaternionf torsoRot = new Quaternionf(
                torso.rotation.x,
                torso.rotation.y,
                torso.rotation.z,
                torso.rotation.w
        );

        Vector3f rotatedPivot = new Vector3f(pivot[1]);
        torsoRot.transform(rotatedPivot);

        Quaternionf q = new Quaternionf(
                transform.rotation.x,
                transform.rotation.y,
                transform.rotation.z,
                transform.rotation.w
        );

        poseStack.translate(-rotatedPivot.x, -rotatedPivot.y, -rotatedPivot.z);
        q.rotateZ((float) Math.PI);
        poseStack.mulPose(q);

        part.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}
