package cofh.thermaldynamics.duct.tiles;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.TDDucts;
import cofh.thermaldynamics.duct.energy.DuctUnitEnergy;
import cofh.thermaldynamics.duct.nutypeducts.DuctToken;
import cofh.thermaldynamics.duct.nutypeducts.TileGridStructureBase;

public abstract class TileEnergyDuct extends TileGridStructureBase implements IEnergyReceiver, IEnergyProvider {

	public TileEnergyDuct(Duct duct) {

		addDuctUnits(DuctToken.ENERGY, new DuctUnitEnergy(this, duct));
	}

	@Override
	protected DuctToken getPrimaryDuctToken() {

		return DuctToken.ENERGY;
	}

	public static class Basic extends TileEnergyDuct {

		public Basic() {

			super(TDDucts.energyBasic);
		}
	}

	public static class Reinforced extends TileEnergyDuct {

		public Reinforced() {

			super(TDDucts.energyReinforced);
		}
	}

	public static class Hardened extends TileEnergyDuct {

		public Hardened() {

			super(TDDucts.energyHardened);
		}
	}

	public static class Signalum extends TileEnergyDuct {

		public Signalum() {

			super(TDDucts.energySignalum);
		}
	}

	public static class Resonant extends TileEnergyDuct {

		public Resonant() {

			super(TDDucts.energyResonant);
		}
	}
}
