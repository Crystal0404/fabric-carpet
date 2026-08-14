package carpet.mixins;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import carpet.patches.FakeClientConnection;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin
{
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "onDisconnect",
            at = @At("TAIL")
    )
    @SuppressWarnings("resource")
    private void createShadow(DisconnectionDetails details, CallbackInfo ci)
    {
        if (this.player.fabric_carpet$shouldShadow())
        {
            MinecraftServer server = this.player.level().getServer();
            ServerLevel worldIn = this.player.level();//.getWorld(player.dimension);
            GameProfile gameprofile = this.player.getGameProfile();
            EntityPlayerMPFake playerShadow = new EntityPlayerMPFake(server, worldIn, gameprofile, this.player.clientInformation(), true);
            playerShadow.setChatSession(this.player.getChatSession());
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerShadow, new CommonListenerCookie(gameprofile, 0, this.player.clientInformation(), true));
            EntityPlayerMPFake.loadPlayerData(playerShadow);

            playerShadow.setHealth(this.player.getHealth());
            playerShadow.connection.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
            playerShadow.gameMode.changeGameModeForPlayer(this.player.gameMode.getGameModeForPlayer());
            ((ServerPlayerInterface) playerShadow).getActionPack().copyFrom(((ServerPlayerInterface) this.player).getActionPack());
            // this might create problems if a player logs back in...
            playerShadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            playerShadow.getEntityData().set(Avatar.DATA_PLAYER_MODE_CUSTOMISATION, this.player.getEntityData().get(Avatar.DATA_PLAYER_MODE_CUSTOMISATION));


            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (this.player.yHeadRot * 256 / 360)), playerShadow.level().dimension());
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, playerShadow));
            //player.world.getChunkManager().updatePosition(playerShadow);
            playerShadow.getAbilities().flying = this.player.getAbilities().flying;
        }
    }
}
