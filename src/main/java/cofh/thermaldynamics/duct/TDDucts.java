package cofh.thermaldynamics.duct;

import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.Duct.Type;
import cofh.thermaldynamics.duct.energy.TileEnergyDuct;
import cofh.thermaldynamics.duct.energy.TileEnergyDuctSuper;
import cofh.thermaldynamics.duct.entity.TileTransportDuct;
import cofh.thermaldynamics.duct.entity.TileTransportDuctCrossover;
import cofh.thermaldynamics.duct.entity.TileTransportDuctLongRange;
import cofh.thermaldynamics.duct.fluid.TileFluidDuct;
import cofh.thermaldynamics.duct.fluid.TileFluidDuctFlux;
import cofh.thermaldynamics.duct.fluid.TileFluidDuctFragile;
import cofh.thermaldynamics.duct.fluid.TileFluidDuctSuper;
import cofh.thermaldynamics.duct.item.TileItemDuct;
import cofh.thermaldynamics.duct.item.TileItemDuctEnder;
import cofh.thermaldynamics.duct.item.TileItemDuctFlux;
import cofh.thermaldynamics.duct.light.DuctLight;
import cofh.thermaldynamics.duct.light.TileLightDuct;

import java.util.ArrayList;
import java.util.List;

public class TDDucts {

	private static final IDuctFactory TRANSPORT_CROSSOVER = (duct, worldObj) -> new TileTransportDuctCrossover();
	private static final IDuctFactory TRANSPORT_LONGRANGE = (duct, worldObj) -> new TileTransportDuctLongRange();
	private static final IDuctFactory TRANSPORT = (duct, worldObj) -> new TileTransportDuct();
	private static final IDuctFactory FLUID_SUPER = (duct, worldObj) -> new TileFluidDuctSuper();
	private static final IDuctFactory GLOW = (duct, worldObj) -> new TileLightDuct();
	private static final IDuctFactory FLUID_FLUX = (duct, worldObj) -> new TileFluidDuctFlux();
	private static final IDuctFactory FLUID_FRAGILE = (duct, worldObj) -> new TileFluidDuctFragile();
	private static final IDuctFactory FLUID = (duct, worldObj) -> new TileFluidDuct();
	private static final IDuctFactory ENERGY_SUPER = (duct, worldObj) -> new TileEnergyDuctSuper();
	private static final IDuctFactory ENERGY = (duct, worldObj) -> new TileEnergyDuct();
	private static final IDuctFactory ITEM_FLUX = (duct, worldObj) -> new TileItemDuctFlux();
	private static final IDuctFactory ITEM_ENDER = (duct, worldObj) -> new TileItemDuctEnder();
	private static final IDuctFactory ITEM = (duct, worldObj) -> new TileItemDuct();

	private static final String REDSTONE_STILL = "thermalfoundation:blocks/fluid/redstone_still";
	private static final String GLOWSTONE_STILL = "thermalfoundation:blocks/fluid/glowstone_still";
	private static final String CRYOTHEUM_STILL = "thermalfoundation:blocks/fluid/cryotheum_still";
	private static final String GREEN_GLASS = "thermaldynamics:blocks/duct/base/green_glass";

	private TDDucts() {

	}

	public static ArrayList<Duct> ductList = new ArrayList<>();
	public static ArrayList<Duct> ductListSorted = null;

	static Duct addDuct(int id, boolean opaque, int pathWeight, int type, String name, Type ductType, IDuctFactory factory, String baseTexture, String connectionTexture, String fluidTexture, int fluidTransparency, String frameTexture, String frameFluidTexture, int frameFluidTransparency) {

		Duct newDuct = new Duct(id, opaque, pathWeight, type, name, ductType, factory, baseTexture, connectionTexture, fluidTexture, fluidTransparency, frameTexture, frameFluidTexture, frameFluidTransparency);

		return registerDuct(newDuct);
	}

	static DuctItem addDuctItem(int id, boolean opaque, int pathWeight, int type, String name, Type ductType, IDuctFactory factory, String baseTexture, String connectionTexture, String fluidTexture, int fluidTransparency, String frameTexture, String frameFluidTexture, int frameFluidTransparency) {

		DuctItem newDuct = new DuctItem(id, opaque, pathWeight, type, name, ductType, factory, baseTexture, connectionTexture, fluidTexture, fluidTransparency, frameTexture, frameFluidTexture, frameFluidTransparency);

		return registerDuct(newDuct);
	}

	static DuctTransport addDuctTransport(int id, boolean opaque, int pathWeight, int type, String name, Type ductType, IDuctFactory factory, String baseTexture, String connectionTexture, String fluidTexture, int fluidTransparency, String frameTexture, String frameFluidTexture, int frameFluidTransparency) {

		DuctTransport newDuct = new DuctTransport(id, opaque, pathWeight, type, name, ductType, factory, baseTexture, connectionTexture, fluidTexture, fluidTransparency, frameTexture, frameFluidTexture, frameFluidTransparency);

		return registerDuct(newDuct);
	}

	static <T extends Duct> T registerDuct(T newDuct) {

		int id = newDuct.id;
		while (id >= ductList.size()) {
			ductList.add(null);
		}
		Duct oldDuct = ductList.set(id, newDuct);
		if (oldDuct != null) {
			ThermalDynamics.LOG.info("Replacing " + oldDuct.unlocalizedName + " with " + newDuct.unlocalizedName);
		}
		return newDuct;
	}

	public static Duct getDuct(int id) {

		if (isValid(id)) {
			return ductList.get(id);
		} else {
			return structure;
		}
	}

	public static List<Duct> getSortedDucts() {

		if (ductListSorted == null) {
			ductListSorted = new ArrayList<>();
			for (Duct duct : ductList) {
				if (duct != null) {
					ductListSorted.add(duct);
				}
			}
			ductListSorted.sort((o1, o2) -> {

				int i = o1.ductType.compareTo(o2.ductType);
				if (i == 0) {
					i = o1.compareTo(o2);
				}
				return i;
			});
		}
		return ductListSorted;
	}

	public static boolean isValid(int id) {

		return id < ductList.size() && ductList.get(id) != null;
	}

	public static Duct getType(int id) {

		return ductList.get(id) != null ? ductList.get(id) : structure;
	}

	public static boolean addDucts() {

		addEnergyDucts();
		addFluidDucts();
		addItemDucts();
		addSupportDucts();
		addTransportDucts();

		return true;
	}

	static void addEnergyDucts() {

		energyBasic = addDuct(OFFSET_ENERGY, false, 1, 0, "energyBasic", Type.ENERGY, ENERGY, "lead", "lead", Duct.REDSTONE_BLOCK, 255, null, null, 0);

		energyHardened = addDuct(OFFSET_ENERGY + 1, false, 1, 1, "energyHardened", Type.ENERGY, ENERGY, "invar", "invar", Duct.REDSTONE_BLOCK, 255, null, null, 0);

		energyReinforced = addDuct(OFFSET_ENERGY + 2, false, 1, 2, "energyReinforced", Type.ENERGY, ENERGY, "electrum", "electrum", REDSTONE_STILL, 128, null, null, 0);

		energyReinforcedEmpty = addDuct(OFFSET_ENERGY + 3, false, 1, -1, "energyReinforcedEmpty", Type.CRAFTING, structural, "electrum", "electrum", null, 0, null, null, 0);

		energyResonant = addDuct(OFFSET_ENERGY + 4, false, 1, 3, "energyResonant", Type.ENERGY, ENERGY, "enderium", "enderium", REDSTONE_STILL, 128, null, null, 0);
		energyResonantEmpty = addDuct(OFFSET_ENERGY + 5, false, 1, -1, "energyResonantEmpty", Type.CRAFTING, structural, "enderium", "enderium", null, 0, null, null, 0);

		energySuperCond = addDuct(OFFSET_ENERGY + 6, false, 1, 4, "energySuperconductor", Type.ENERGY, ENERGY_SUPER, "electrum", "electrum", REDSTONE_STILL, 255, "electrum", CRYOTHEUM_STILL, 96);
		energySuperCondEmpty = addDuct(OFFSET_ENERGY + 7, false, 1, -1, "energySuperconductorEmpty", Type.CRAFTING, structural, "electrum", "electrum", REDSTONE_STILL, 192, "electrum", null, 0);

		energyReinforced.setRarity(1);
		energyReinforcedEmpty.setRarity(1);
		energyResonant.setRarity(2);
		energyResonantEmpty.setRarity(2);
		energySuperCond.setRarity(2);
		energySuperCondEmpty.setRarity(2);
	}

	static void addFluidDucts() {

		fluidBasic = addDuct(OFFSET_FLUID, false, 1, 0, "fluidBasic", Type.FLUID, FLUID_FRAGILE, "copper", "copper", null, 0, null, null, 0);
		fluidBasicOpaque = addDuct(OFFSET_FLUID + 1, true, 1, 0, "fluidBasic", Type.FLUID, FLUID_FRAGILE, "copper", "copper", null, 0, null, null, 0);

		fluidHardened = addDuct(OFFSET_FLUID + 2, false, 1, 1, "fluidHardened", Type.FLUID, FLUID, "invar", "invar", null, 0, null, null, 0);
		fluidHardenedOpaque = addDuct(OFFSET_FLUID + 3, true, 1, 1, "fluidHardened", Type.FLUID, FLUID, "invar", "invar", null, 0, null, null, 0);

		fluidFlux = addDuct(OFFSET_FLUID + 4, false, 1, 2, "fluidFlux", Type.FLUID, FLUID_FLUX, "flux_electrum", "flux_electrum", null, 0, null, null, 0);
		fluidFluxOpaque = addDuct(OFFSET_FLUID + 5, true, 1, 2, "fluidFlux", Type.FLUID, FLUID_FLUX, "flux_electrum", "flux_electrum", null, 0, null, null, 0);

		fluidSuper = addDuct(OFFSET_FLUID + 6, false, 1, 3, "fluidSuper", Type.FLUID, FLUID_SUPER, "invar", "invar", null, 0, "bronze_large", null, 0);

		fluidSuperOpaque = addDuct(OFFSET_FLUID + 7, true, 1, 3, "fluidSuper", Type.FLUID, FLUID_SUPER, "invar", "invar", null, 0, "bronze_large", null, 0);

		fluidHardened.setRarity(1);
		fluidHardenedOpaque.setRarity(1);

		fluidFlux.setRarity(1);
		fluidFluxOpaque.setRarity(1);

		fluidSuper.setRarity(2);
		fluidSuperOpaque.setRarity(2);
	}

	static void addItemDucts() {

		itemBasic = addDuctItem(OFFSET_ITEM, false, 1, 0, "itemBasic", Type.ITEM, ITEM, "tin", "tin", null, 0, null, null, 0);
		itemBasicOpaque = addDuctItem(OFFSET_ITEM + 1, true, 1, 0, "itemBasic", Type.ITEM, ITEM, "tin", "tin", null, 0, null, null, 0);

		itemFast = addDuctItem(OFFSET_ITEM + 2, false, 1, 1, "itemFast", Type.ITEM, ITEM, "tin", "tin", GLOWSTONE_STILL, 128, null, null, 0);
		itemFastOpaque = addDuctItem(OFFSET_ITEM + 3, true, 1, 1, "itemFast", Type.ITEM, ITEM, "tin_1", "tin", null, 0, null, null, 0);

		itemEnder = addDuctItem(OFFSET_ITEM + 4, false, 0, 2, "itemEnder", Type.ITEM, ITEM_ENDER, "enderium", "enderium", null, 48, null, null, 0);
		itemEnderOpaque = addDuctItem(OFFSET_ITEM + 5, true, 0, 2, "itemEnder", Type.ITEM, ITEM_ENDER, "enderium", "enderium", null, 48, null, null, 0);

		itemEnergy = addDuctItem(OFFSET_ITEM + 6, false, 1, 3, "itemFlux", Type.ITEM, ITEM_FLUX, "tin", "tin", REDSTONE_STILL, 48, null, null, 0);

		itemEnergyOpaque = addDuctItem(OFFSET_ITEM + 7, true, 1, 3, "itemFlux", Type.ITEM, ITEM_FLUX, "tin_2", "tin", null, 0, null, null, 0);

		itemFast.setRarity(1);
		itemFastOpaque.setRarity(1);

		itemEnder.setRarity(2);
		itemEnderOpaque.setRarity(2);

		itemEnergy.setRarity(1);
		itemEnergyOpaque.setRarity(1);
	}

	static void addTransportDucts() {

		transportBasic = addDuctTransport(OFFSET_TRANSPORT, false, 1, 4, "transport", Type.TRANSPORT, TRANSPORT, null, null, null, 255, "electrum", GREEN_GLASS, 96);

		transportLongRange = addDuctTransport(OFFSET_TRANSPORT + 1, false, 1, 4, "transportLongRange", Type.TRANSPORT, TRANSPORT_LONGRANGE, null, null, null, 255, "copper", GREEN_GLASS, 80);

		transportCrossover = addDuctTransport(OFFSET_TRANSPORT + 2, false, 1, 4, "transportAcceleration", Type.TRANSPORT, TRANSPORT_CROSSOVER, null, null, null, 255, "enderium", GREEN_GLASS, 128);

		transportFrame = addDuctTransport(OFFSET_TRANSPORT + 3, false, 1, 4, "transportCrafting", Type.CRAFTING, structural, null, null, null, 255, "electrum", null, 128);

		transportBasic.setRarity(1);
		transportLongRange.setRarity(1);
		transportCrossover.setRarity(2);
	}

	static void addSupportDucts() {

		structure = addDuct(OFFSET_STRUCTURE, true, 1, -1, "structure", Type.STRUCTURAL, structural, "support", null, null, 0, null, null, 0);

		lightDuct = registerDuct(new DuctLight(OFFSET_STRUCTURE + 1, 0, "structureGlow", Type.STRUCTURAL, GLOW, "lumium", "lumium", null, 0));
	}

	public static int OFFSET_ENERGY = 0;
	public static int OFFSET_FLUID = 16;
	public static int OFFSET_ITEM = 2 * 16;
	public static int OFFSET_STRUCTURE = 3 * 16;
	public static int OFFSET_TRANSPORT = 4 * 16;

	/* ENERGY */
	public static Duct energyBasic;

	public static Duct energyHardened;

	public static Duct energyReinforced;
	public static Duct energyReinforcedEmpty;

	public static Duct energyResonant;
	public static Duct energyResonantEmpty;

	public static Duct energySuperCond;
	public static Duct energySuperCondEmpty;

	/* FLUID */
	public static Duct fluidBasic;
	public static Duct fluidBasicOpaque;

	public static Duct fluidHardened;
	public static Duct fluidHardenedOpaque;

	public static Duct fluidFlux;
	public static Duct fluidFluxOpaque;

	public static Duct fluidSuper;
	public static Duct fluidSuperOpaque;

	/* ITEM */
	public static DuctItem itemBasic;
	public static DuctItem itemBasicOpaque;

	public static DuctItem itemFast;
	public static DuctItem itemFastOpaque;

	public static DuctItem itemEnder;
	public static DuctItem itemEnderOpaque;

	public static DuctItem itemEnergy;
	public static DuctItem itemEnergyOpaque;

	/* TRANSPORT */
	public static DuctTransport transportBasic;
	public static DuctTransport transportLongRange;
	public static DuctTransport transportCrossover;
	public static DuctTransport transportFrame;

	/* STRUCTURE */
	public static Duct structure;

	public static DuctLight lightDuct;

	private static final IDuctFactory structural = (duct, worldObj) -> new TileStructuralDuct();
	/* HELPERS - NOT REAL */
	public static Duct structureInvis = new Duct(-1, false, 1, -1, "structure", Type.STRUCTURAL, structural, "support", null, null, 0, null, null, 0);

	public static Duct placeholder = new Duct(-1, false, 1, -1, "structure", Type.STRUCTURAL, structural, "support", null, null, 0, null, null, 0);

}
