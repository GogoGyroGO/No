package dev.gyro.spin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpinClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(SpinMod.BALL, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(SpinMod.GYRO, context -> new VillagerEntityRenderer(context) {
            @Override public Identifier getTexture(net.minecraft.entity.passive.VillagerEntity entity) { return SpinMod.id("textures/entity/gyro.png"); }
        });
        EntityRendererRegistry.register(SpinMod.CHAIR, ChairRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(SpinMod.OPEN, (client, handler, buf, sender) -> {
            int rank = buf.readVarInt(), levels = buf.readVarInt();
            client.execute(() -> client.setScreen(new TreeScreen(rank, levels)));
        });
    }
    private static final class TreeScreen extends Screen {
        private final int rank, levels;
        TreeScreen(int rank, int levels) { super(Text.translatable("screen.gyrospin.title")); this.rank = rank; this.levels = levels; }
        @Override protected void init() {
            int left = width / 2 - 145;
            int top = Math.max(45, height / 2 - 90);
            for (int i = 1; i <= 5; i++) {
                final int node = i;
                Text label = Text.translatable("skill.gyrospin." + i).copy().append(i <= rank ? " ✓" : " (" + SpinSkills.cost(i) + " XP)");
                ButtonWidget button = ButtonWidget.builder(label, b -> {
                    b.active = false;
                    ClientPlayNetworking.send(SpinMod.LEARN, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create());
                }).dimensions(left, top + (i-1)*30, 290, 20).tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("skill.gyrospin."+node+".desc"))).build();
                button.active = i == rank+1 && SpinSkills.canLearn(rank, levels, client.player != null && client.player.isCreative());
                addDrawableChild(button);
            }
            addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(width/2-50,top+168,100,20).build());
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            int top = Math.max(45, height/2-90);
            context.drawCenteredTextWithShadow(textRenderer, title, width/2, top-32, 0xF5D76E);
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable(rank == 0 ? "message.gyrospin.find_gyro" : "screen.gyrospin.levels", levels), width/2, top-17, 0xFFFFFF);
            for (int i=0;i<4;i++) context.fill(width/2-1, top+i*30+20, width/2+1, top+i*30+30, i+1 < rank ? 0xFF78D98B : 0xFF9E8541);
            super.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean shouldPause() { return false; }
    }
    /** Block-based model needs no external texture assets. */
    private static final class ChairRenderer extends EntityRenderer<WheelchairEntity> {
        ChairRenderer(EntityRendererFactory.Context context) { super(context); shadowRadius = .5f; }
        @Override public Identifier getTexture(WheelchairEntity entity) { return net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE; }
        private void part(MatrixStack matrices, VertexConsumerProvider vertices, int light, double x,double y,double z,float sx,float sy,float sz, net.minecraft.block.BlockState state) {
            matrices.push(); matrices.translate(x,y,z); matrices.scale(sx,sy,sz);
            net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(state, matrices, vertices, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
        @Override public void render(WheelchairEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
            matrices.push(); matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            var frame=Blocks.IRON_BLOCK.getDefaultState(); var seat=Blocks.GREEN_WOOL.getDefaultState(); var wheel=Blocks.BLACK_CONCRETE.getDefaultState();
            part(matrices,vertices,light,-.34,.48,-.3,.68f,.12f,.65f,seat);
            part(matrices,vertices,light,-.34,.6,.26,.68f,.62f,.10f,seat);
            for (int side : new int[]{-1,1}) {
                part(matrices,vertices,light,side*.42-.06,.08,-.3,.12f,.56f,.56f,wheel);
                part(matrices,vertices,light,side*.32-.035,.15,-.25,.07f,.65f,.07f,frame);
                part(matrices,vertices,light,side*.32-.035,.77,-.28,.07f,.06f,.62f,frame);
                part(matrices,vertices,light,side*.30-.06,.02,-.52,.12f,.2f,.2f,wheel);
            }
            matrices.pop(); super.render(entity,yaw,tickDelta,matrices,vertices,light);
        }
    }
}
