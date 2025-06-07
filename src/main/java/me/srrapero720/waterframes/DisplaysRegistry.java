package me.srrapero720.waterframes;

import me.srrapero720.waterframes.client.rendering.DisplayRenderer;
import me.srrapero720.waterframes.common.block.*;
import me.srrapero720.waterframes.common.block.entity.*;
import me.srrapero720.waterframes.common.commands.WaterFramesCommand;
import me.srrapero720.waterframes.common.item.RemoteControl;
import me.srrapero720.waterframes.common.item.data.CodecManager;
import me.srrapero720.waterframes.common.item.data.RemoteData;
import me.srrapero720.waterframes.common.network.packets.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

import static me.srrapero720.waterframes.common.network.DisplayNetwork.*;
import static me.srrapero720.waterframes.WaterFrames.*;

public class DisplaysRegistry {
    /* DATA */
    public static final DataComponentType<RemoteData> REMOTE_DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, resloc("remote"), new DataComponentType.Builder<RemoteData>()
            .persistent(CodecManager.REMOTE_CODEC)
            .networkSynchronized(CodecManager.REMOTE_STREAM_CODEC)
            .build());

    /* BLOCKS */
    public static final DisplayBlock
            FRAME = registerBlock("frame", FrameBlock::new, BlockBehaviour.Properties.of()),
            PROJECTOR = registerBlock("projector", ProjectorBlock::new, BlockBehaviour.Properties.of()),
            TV = registerBlock("tv", TvBlock::new, BlockBehaviour.Properties.of()),
            BIG_TV = registerBlock("big_tv", BigTvBlock::new, BlockBehaviour.Properties.of()),
            TV_BOX = registerBlock("tv_box", TVBoxBlock::new, BlockBehaviour.Properties.of());
//            GOLDEN_PROJECTOR = BLOCKS.register("golden_projector", ProjectorBlock::new);

    /* ITEMS */
    public static final Item
            REMOTE_ITEM = registerItem("remote", RemoteControl::new, remoteProp()),
            FRAME_ITEM = registerBlockItem("frame", FRAME, prop()),
            PROJECTOR_ITEM = registerBlockItem("projector", PROJECTOR, prop()),
            TV_ITEM = registerBlockItem("tv", TV, prop()),
            BIG_TV_ITEM = registerBlockItem("big_tv", BIG_TV, prop()),
            TV_BOX_ITEM = registerBlockItem("tv_box", TV_BOX, prop());
//            GOLDEN_PROJECTOR_ITEM = ITEMS.register("golden_projector", () -> new BlockItem(GOLDEN_PROJECTOR.get(), prop().tab(null)));

    /* TILES */
    public static final BlockEntityType<DisplayTile>
            TILE_FRAME = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resloc("frame"), tile(FrameTile::new, () -> FRAME)),
            TILE_PROJECTOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resloc("projector"), tile(ProjectorTile::new, () -> PROJECTOR)),
            TILE_TV = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resloc("tv"), tile(TvTile::new, () -> TV)),
            TILE_BIG_TV = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resloc("big_tv"), tile(BigTvTile::new, () -> BIG_TV)),
            TILE_TV_BOX = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, resloc("tv_box"), tile(TVBoxTile::new, () -> TV_BOX));

    /* TABS */
    public static final CreativeModeTab WATERTAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, resloc("tab"), FabricItemGroup.builder()
            .icon(() -> new ItemStack(FRAME_ITEM))
            .title(Component.translatable("itemGroup.waterframes"))
            .displayItems((itemDisplayParameters, output) -> {
                output.accept(REMOTE_ITEM);
                output.accept(FRAME_ITEM);
                output.accept(PROJECTOR_ITEM);
                output.accept(TV_ITEM);
                output.accept(BIG_TV_ITEM);
                output.accept(TV_BOX_ITEM);
            })
            .build());

    /* PERMISSIONS */
    public static final String
            PERM_DISPLAYS_EDIT = "waterframes.displays.save",
            PERM_DISPLAYS_INTERACT = "waterframes.displays.interact",
            PERM_DISPLAYS_INTERACT_FRAME = "waterframes.displays.interact.frame",
            PERM_DISPLAYS_INTERACT_PROJECTOR = "waterframes.displays.interact.projector",
            PERM_DISPLAYS_INTERACT_TV = "waterframes.displays.interact.tv",
            PERM_REMOTE_INTERACT = "waterframes.remote.interact",
            PERM_REMOTE_BIND = "waterframes.remote.bind",
            PERM_WHITELIST_BYPASS = "waterframes.whitelist.bypass";

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = makeKey(Registries.BLOCK, name);
        T block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static <T extends Item> T registerItem(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = makeKey(Registries.ITEM, name);
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static BlockItem registerBlockItem(String name, Block block, Item.Properties properties) {
        ResourceKey<Item> key = makeKey(Registries.ITEM, name);
        BlockItem blockItem = new BlockItem(block, properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, blockItem);
    }

    public static boolean getPermBoolean(Player player, String node) {
        return DisplaysConfig.isOwner(player) || player.hasPermissions(2);
    }

    private static BlockEntityType<DisplayTile> tile(FabricBlockEntityTypeBuilder.Factory<DisplayTile> creator, Supplier<DisplayBlock> block) {
        return FabricBlockEntityTypeBuilder.<DisplayTile>create(creator, block.get()).build();
    }

    private static <T> ResourceKey<T> makeKey(ResourceKey<? extends Registry<T>> reg, String name) {
        return ResourceKey.create(reg, resloc(name));
    }

    private static Item.Properties remoteProp() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant();
    }

    private static Item.Properties prop() {
        return new Item.Properties().stacksTo(16).rarity(Rarity.RARE);
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WaterFramesCommand.register(dispatcher));
        NET.registerType(DataSyncPacket.class, DataSyncPacket::new);
        NET.registerType(ActivePacket.class, ActivePacket::new);
        NET.registerType(LoopPacket.class, LoopPacket::new);
        NET.registerType(MutePacket.class, MutePacket::new);
        NET.registerType(PausePacket.class, PausePacket::new);
        NET.registerType(TimePacket.class, TimePacket::new);
        NET.registerType(VolumePacket.class, VolumePacket::new);
        NET.registerType(VolumeRangePacket.class, VolumeRangePacket::new);
    }

    @Environment(EnvType.CLIENT)
    public static void registerTexture(ResourceLocation location, AbstractTexture texture) {
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    @Environment(EnvType.CLIENT)
    public static void unregisterTexture(ResourceLocation location) {
        Minecraft.getInstance().getTextureManager().release(location);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> WaterFramesCommand.registerClient(dispatcher));
        BlockEntityRenderers.register(TILE_FRAME, DisplayRenderer::new);
        BlockEntityRenderers.register(TILE_PROJECTOR, DisplayRenderer::new);
        BlockEntityRenderers.register(TILE_TV, DisplayRenderer::new);
        BlockEntityRenderers.register(TILE_BIG_TV, DisplayRenderer::new);
        BlockEntityRenderers.register(TILE_TV_BOX, DisplayRenderer::new);
    }

    public static ResourceLocation resloc(String name) {
        return new ResourceLocation(ID, name);
    }

    public static class UnsupportedModException extends UnsupportedOperationException {
        private static final String MSG_REASON = "§fMod §6'%s' §fis not compatible with §e'%s' §fbecause §c%s §fplease remove it";

        public UnsupportedModException(String modid, String reason) {
            super(String.format(MSG_REASON, modid, NAME, reason));
        }
    }
}