package cofh.thermaldynamics.block;

import codechicken.lib.block.property.PropertyInteger;
import codechicken.lib.model.DummyBakedModel;
import codechicken.lib.model.ModelRegistryHelper;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import cofh.api.block.IBlockConfigGui;
import cofh.core.init.CoreProps;
import cofh.core.network.PacketHandler;
import cofh.core.render.IBlockAppearance;
import cofh.core.render.IModelRegister;
import cofh.core.render.hitbox.ICustomHitBox;
import cofh.core.render.hitbox.RenderHitbox;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.ItemBlockDuct;
import cofh.thermaldynamics.duct.TDDucts;
import cofh.thermaldynamics.duct.attachments.cover.Cover;
import cofh.thermaldynamics.duct.energy.EnergyGrid;
import cofh.thermaldynamics.duct.entity.EntityTransport;
import cofh.thermaldynamics.duct.entity.TransportHandler;
import cofh.thermaldynamics.duct.fluid.PacketFluid;
import cofh.thermaldynamics.duct.nutypeducts.DuctUnit;
import cofh.thermaldynamics.duct.nutypeducts.TileGrid;
import cofh.thermaldynamics.duct.tiles.*;
import cofh.thermaldynamics.proxy.ProxyClient;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BlockDuct extends BlockTDBase implements IBlockAppearance, IBlockConfigGui, IModelRegister {

	public static final PropertyInteger VARIANT = new PropertyInteger("meta", 15);
	public static final ThreadLocal<BlockPos> IGNORE_RAY_TRACE = new ThreadLocal<>();
	public int offset;

	public BlockDuct(int offset) {

		super(Material.GLASS);

		setUnlocalizedName("duct");

		setHardness(1.0F);
		setResistance(10.0F);
		setDefaultState(getBlockState().getBaseState().withProperty(VARIANT, 0));

		this.offset = offset * 16;
	}

	@Override
	protected BlockStateContainer createBlockState() {

		return new BlockStateContainer(this, VARIANT);
	}

	@Override
	@SideOnly (Side.CLIENT)
	public void getSubBlocks(@Nonnull Item item, CreativeTabs tab, List<ItemStack> list) {

		for (int i = 0; i < 16; i++) {
			if (TDDucts.isValid(i + offset)) {
				list.add(TDDucts.getDuct(i + offset).itemStack.copy());
			}
		}
	}

	/* TYPE METHODS */
	@Override
	public IBlockState getStateFromMeta(int meta) {

		return getDefaultState().withProperty(VARIANT, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(VARIANT);
	}

	@Override
	public int damageDropped(IBlockState state) {

		return state.getValue(VARIANT);
	}

	/* ITileEntityProvider */
	@Override
	public TileEntity createNewTileEntity(@Nonnull World world, int metadata) {

		Duct duct = TDDucts.getType(metadata + offset);

		return duct.factory.createTileEntity(duct, world);
	}

	/* BLOCK METHODS */
	@Override
	public void addCollisionBoxToList(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity) {

		if (entity instanceof EntityTransport) {
			return;
		}
		float min = getSize(state);
		float max = 1 - min;

		AxisAlignedBB bb = new AxisAlignedBB(min, min, min, max, max, max);
		addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
		TileGrid theTile = (TileGrid) world.getTileEntity(pos);

		if (theTile != null) {
			for (byte i = 0; i < 6; i++) {
				Attachment attachment = theTile.getAttachment(i);
				if (attachment != null) {
					attachment.addCollisionBoxesToList(entityBox, collidingBoxes, entity);
				}
				Cover cover = theTile.getCover(i);
				if (cover != null) {
					cover.addCollisionBoxesToList(entityBox, collidingBoxes, entity);
				}
			}
			if (theTile.getVisualConnectionType(0).renderDuct) {
				bb = new AxisAlignedBB(min, 0.0F, min, max, max, max);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
			if (theTile.getVisualConnectionType(1).renderDuct) {
				bb = new AxisAlignedBB(min, min, min, max, 1.0F, max);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
			if (theTile.getVisualConnectionType(2).renderDuct) {
				bb = new AxisAlignedBB(min, min, 0.0F, max, max, max);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
			if (theTile.getVisualConnectionType(3).renderDuct) {
				bb = new AxisAlignedBB(min, min, min, max, max, 1.0F);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
			if (theTile.getVisualConnectionType(4).renderDuct) {
				bb = new AxisAlignedBB(0.0F, min, min, max, max, max);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
			if (theTile.getVisualConnectionType(5).renderDuct) {
				bb = new AxisAlignedBB(min, min, min, 1.0F, max, max);
				addCollisionBoxToList(pos, entityBox, collidingBoxes, bb);
			}
		}
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack) {

		super.onBlockPlacedBy(world, pos, state, living, stack);

		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof TileGrid) {
			((TileGrid) tile).onPlacedBy(living, stack);
		}
	}

	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {

		if (side == null) {
			return false;
		}
		int s;
		if (side == EnumFacing.DOWN) {
			s = 2;
		} else if (side == EnumFacing.UP) {
			s = 5;
		} else if (side == EnumFacing.NORTH) {
			s = 3;
		} else {
			s = 4;
		}
		TileGrid theTile = (TileGrid) world.getTileEntity(pos);
		return theTile != null && theTile.getAttachment(s ^ 1) != null && theTile.getAttachment(s ^ 1).shouldRSConnect();
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {

		return TDDucts.isValid(getMetaFromState(state) + offset);
	}

	@Override
	public boolean isFullCube(IBlockState state) {

		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {

		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {

		return false;
	}

	@Override
	public boolean isSideSolid(IBlockState base_state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {

		TileGrid theTile = (TileGrid) world.getTileEntity(pos);
		return (theTile != null && (theTile.getCover(side.ordinal()) != null || theTile.getAttachment(side.ordinal()) != null && theTile.getAttachment(side.ordinal()).makesSideSolid())) || super.isSideSolid(base_state, world, pos, side);
	}

	//	@Override
	//	public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
	//
	//		return 0;
	//	}

	@Override
	public int getWeakPower(IBlockState blockState, IBlockAccess world, BlockPos pos, EnumFacing side) {

		TileGrid theTile = (TileGrid) world.getTileEntity(pos);
		if (theTile != null && theTile.getAttachment(side.ordinal() ^ 1) != null) {
			return theTile.getAttachment(side.ordinal() ^ 1).getRSOutput();
		}
		return 0;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {

		float min = getSize(state);
		float max = 1 - min;

		return new AxisAlignedBB(min, min, min, max, max, max);
	}

	//	@Override
	//	public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, ItemStack stack) {
	//
	//		return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, stack);
	//	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {

		if (target.subHit >= 14 && target.subHit < 20) {
			TileGrid tileEntity = (TileGrid) world.getTileEntity(pos);
			Attachment attachment = tileEntity.getAttachment(target.subHit - 14);
			ItemStack pickBlock = attachment.getPickBlock();
			if (pickBlock != null) {
				return pickBlock;
			}
		}
		if (target.subHit >= 20 && target.subHit < 26) {
			TileGrid tileEntity = (TileGrid) world.getTileEntity(pos);
			ItemStack pickBlock = tileEntity.getCover(target.subHit - 20).getPickBlock();
			if (pickBlock != null) {
				return pickBlock;
			}
		}
		return super.getPickBlock(state, target, world, pos, player);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState blockState, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {

		BlockPos ignore_pos = IGNORE_RAY_TRACE.get();
		if (ignore_pos != null && ignore_pos.equals(pos)) {
			return null;
		}

		TileGrid tile = (TileGrid) world.getTileEntity(pos);
		if (tile != null) {
			List<IndexedCuboid6> cuboids = new LinkedList<>();
			tile.addTraceableCuboids(cuboids);
			return RayTracer.rayTraceCuboidsClosest(start, end, cuboids, pos);
		}
		return null;
	}

	/* RENDERING METHODS */
	@Override
	@SideOnly (Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {

		super.randomDisplayTick(state, world, pos, rand);

		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof TileGrid) {
			((TileGrid) tileEntity).randomDisplayTick();
		}
	}

	@Override
	@SideOnly (Side.CLIENT)
	public boolean canRenderInLayer(IBlockState state, @Nonnull BlockRenderLayer layer) {

		return true;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {

		return BlockRenderLayer.CUTOUT;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public EnumBlockRenderType getRenderType(IBlockState state) {

		return ProxyClient.renderType;
	}

	/* EVENT HANDLERS */
	@SideOnly (Side.CLIENT)
	@SubscribeEvent (priority = EventPriority.HIGH)
	public void onBlockHighlight(DrawBlockHighlightEvent event) {

		RayTraceResult target = event.getTarget();
		EntityPlayer player = event.getPlayer();
		if (target.typeOfHit == RayTraceResult.Type.BLOCK && player.worldObj.getBlockState(event.getTarget().getBlockPos()).getBlock().getUnlocalizedName().equals(getUnlocalizedName())) {
			RayTracer.retraceBlock(player.worldObj, player, target.getBlockPos());

			ICustomHitBox theTile = ((ICustomHitBox) player.worldObj.getTileEntity(target.getBlockPos()));
			if (theTile.shouldRenderCustomHitBox(target.subHit, player)) {
				event.setCanceled(true);
				RenderHitbox.drawSelectionBox(player, target, event.getPartialTicks(), theTile.getCustomHitBox(target.subHit, player));
			}
		}
	}

	/* IBlockAppearance */
	@Override
	public IBlockState getVisualState(IBlockAccess world, BlockPos pos, EnumFacing side) {

		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof TileGrid) {
			Cover cover = ((TileGrid) tileEntity).getCover(side.ordinal());
			if (cover != null) {
				return cover.state;
			}
		}
		return world.getBlockState(pos);
	}

	@Override
	public boolean supportsVisualConnections() {

		return true;
	}

	/* IBlockConfigGui */
	@Override
	public boolean openConfigGui(IBlockAccess world, BlockPos pos, EnumFacing side, EntityPlayer player) {

		TileGrid tile = (TileGrid) world.getTileEntity(pos);

		if (tile instanceof IBlockConfigGui) {
			return ((IBlockConfigGui) tile).openConfigGui(world, pos, side, player);
		} else if (tile != null) {
			int subHit = side.ordinal();
			if (world instanceof World) {
				RayTraceResult rayTrace = RayTracer.retraceBlock((World) world, player, pos);
				if (rayTrace == null) {
					return false;
				}

				if (subHit > 13 && subHit < 20) {
					subHit = rayTrace.subHit - 14;
				}
			}
			if (subHit > 13 && subHit < 20) {
				Attachment attachment = tile.getAttachment(subHit - 14);
				if (attachment instanceof IBlockConfigGui) {
					return ((IBlockConfigGui) attachment).openConfigGui(world, pos, side, player);
				}
			}
			for (DuctUnit ductUnit : tile.getDuctUnits()) {
				if (ductUnit instanceof IBlockConfigGui) {
					return ((IBlockConfigGui) ductUnit).openConfigGui(world, pos, side, player);
				}
			}
		}
		return false;
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		ModelLoader.setCustomStateMapper(this, new StateMap.Builder().ignore(VARIANT).build());
		ModelResourceLocation normalLocation = new ModelResourceLocation(getRegistryName(), "normal");
		ModelRegistryHelper.register(normalLocation, new DummyBakedModel());
		ModelRegistryHelper.registerItemRenderer(Item.getItemFromBlock(this), RenderDuct.instance);
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		GameRegistry.register(this.setRegistryName("ThermalDynamics_" + offset));
		GameRegistry.register(new ItemBlockDuct(this).setRegistryName("ThermalDynamics_" + offset));
		ThermalDynamics.proxy.addIModelRegister(this);

		for (int i = 0; i < 16; i++) {
			if (TDDucts.isValid(offset + i)) {
				TDDucts.getType(offset + i).itemStack = new ItemStack(this, 1, i);
			}
		}
		return true;
	}

	@Override
	public boolean initialize() {

		MinecraftForge.EVENT_BUS.register(this);

		if (offset != 0) {
			return true;
		}
		EnergyGrid.initialize();

		PacketHandler.instance.registerPacket(PacketFluid.class);

		GameRegistry.registerTileEntity(TileItemDuct.Basic.class, "thermaldynamics.itemduct.transparent");
		GameRegistry.registerTileEntity(TileItemDuct.Opaque.class, "thermaldynamics.itemduct.opaque");
		GameRegistry.registerTileEntity(TileItemDuct.Fast.class, "thermaldynamics.itemduct.fast.transparent");
		GameRegistry.registerTileEntity(TileItemDuct.FastOpaque.class, "thermaldynamics.itemduct.fast.opaque");
		GameRegistry.registerTileEntity(TileItemDuct.Flux.Transparent.class, "thermaldynamics.itemduct.flux.transparent");
		GameRegistry.registerTileEntity(TileItemDuct.Flux.Opaque.class, "thermaldynamics.itemduct.flux.opaque");
		GameRegistry.registerTileEntity(TileItemDuct.Warp.Transparent.class, "thermaldynamics.itemduct.warp.transparent");
		GameRegistry.registerTileEntity(TileItemDuct.Warp.Opaque.class, "thermaldynamics.itemduct.warp.opaque");
		GameRegistry.registerTileEntity(TileDuctOmni.Transparent.class, "thermaldynamics.itemduct.ender.transparent");
		GameRegistry.registerTileEntity(TileDuctOmni.Opaque.class, "thermaldynamics.itemduct.ender.opaque");

		GameRegistry.registerTileEntity(TileEnergyDuct.Basic.class, "thermaldynamics.fluxduct.basic");
		GameRegistry.registerTileEntity(TileEnergyDuct.Hardened.class, "thermaldynamics.fluxduct.hardened");
		GameRegistry.registerTileEntity(TileEnergyDuct.Reinforced.class, "thermaldynamics.fluxduct.reinforced");
		GameRegistry.registerTileEntity(TileEnergyDuct.Signalum.class, "thermaldynamics.fluxduct.signalum");
		GameRegistry.registerTileEntity(TileEnergyDuct.Resonant.class, "thermaldynamics.fluxduct.resonant");
		GameRegistry.registerTileEntity(TileEnergySuperDuct.class, "thermaldynamics.fluxduct.super");

		GameRegistry.registerTileEntity(TileFluidDuct.Fragile.Transparent.class, "thermaldynamics.fluidduct.fragile.transparent");
		GameRegistry.registerTileEntity(TileFluidDuct.Fragile.Opaque.class, "thermaldynamics.fluidduct.fragile.opaque");
		GameRegistry.registerTileEntity(TileFluidDuct.Hardened.Transparent.class, "thermaldynamics.fluidduct.hardened.transparent");
		GameRegistry.registerTileEntity(TileFluidDuct.Hardened.Opaque.class, "thermaldynamics.fluidduct.hardened.opaque");
		GameRegistry.registerTileEntity(TileFluidDuct.Flux.Transparent.class, "thermaldynamics.fluidduct.flux.transparent");
		GameRegistry.registerTileEntity(TileFluidDuct.Flux.Opaque.class, "thermaldynamics.fluidduct.flux.opaque");
		GameRegistry.registerTileEntity(TileFluidDuct.Super.Transparent.class, "thermaldynamics.fluidduct.super.transparent");
		GameRegistry.registerTileEntity(TileFluidDuct.Super.Opaque.class, "thermaldynamics.fluidduct.super.opaque");

		GameRegistry.registerTileEntity(TileStructuralDuct.class, "thermaldynamics.structure");

		GameRegistry.registerTileEntity(TileLuxDuct.class, "thermaldynamics.luxduct");

		GameRegistry.registerTileEntity(TileTransportDuct.class, "thermaldynamics.viaduct");
		GameRegistry.registerTileEntity(TileTransportDuct.LongRange.class, "thermaldynamics.viaduct.longrange");
		GameRegistry.registerTileEntity(TileTransportDuct.Linking.class, "thermaldynamics.viaduct.linking");

		EntityRegistry.registerModEntity(EntityTransport.class, "Transport", 0, ThermalDynamics.instance, CoreProps.ENTITY_TRACKING_DISTANCE, 1, true);
		MinecraftForge.EVENT_BUS.register(TransportHandler.INSTANCE);
		FMLCommonHandler.instance().bus().register(TransportHandler.INSTANCE);

		addRecipes();

		return true;
	}

	@Override
	public boolean postInit() {

		return true;
	}

	/* HELPERS */
	private void addRecipes() {

		// TODO
	}

	public float getSize(IBlockState state) {

		return TDDucts.getDuct(offset + getMetaFromState(state)).isLargeTube() ? 0.05F : 0.3F;
	}

	/* CONNECTIONS */
	public enum ConnectionType {

		// @formatter:off
		NONE(false),
		STRUCTURE_CLEAN,
		DUCT,
		CLEAN_DUCT,
		STRUCTURE_CONNECTION,
		TILE_CONNECTION;
		// @formatter:on

		private final boolean renderDuct;

		ConnectionType() {

			this(true);
		}

		ConnectionType(boolean renderDuct) {

			this.renderDuct = renderDuct;
		}

		public static ConnectionType getPriority(@Nonnull ConnectionType a, @Nonnull ConnectionType b) {

			if (a.ordinal() < b.ordinal()) {
				return b;
			}
			return a;
		}

		public boolean renderDuct() {

			return this.renderDuct;
		}
	}

	/* TYPE */
	public enum Type implements IStringSerializable {

		// @formatter:off
		ENERGY(0, "energy"),
		FLUID(1, "fluid"),
		ITEM(2, "item"),
		TRANSPORT(3, "transport");
		// @formatter:on

		private static final Type[] METADATA_LOOKUP = new Type[values().length];

		private final int metadata;
		private final String name;

		Type(int metadata, String name) {

			this.metadata = metadata;
			this.name = name;
		}

		public int getMetadata() {

			return this.metadata;
		}

		@Nonnull
		@Override
		public String getName() {

			return this.name;
		}

		public static Type byMetadata(int metadata) {

			if (metadata < 0 || metadata >= METADATA_LOOKUP.length) {
				metadata = 0;
			}
			return METADATA_LOOKUP[metadata];
		}

		static {
			for (Type type : values()) {
				METADATA_LOOKUP[type.getMetadata()] = type;
			}
		}
	}

	/* REFERENCES */
	public static ItemStack ductEnergy;
	public static ItemStack ductFluid;
	public static ItemStack ductItem;
	public static ItemStack ductTransport;

	public static ItemBlockDuct itemBlock;

}
