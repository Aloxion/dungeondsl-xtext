
package org.xtext.validation;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.validation.Check;
import org.xtext.dungeonDSL.BOOLEAN;
import org.xtext.dungeonDSL.Dungeon;
import org.xtext.dungeonDSL.Floor;
import org.xtext.dungeonDSL.Room;
import org.xtext.dungeonDSL.Sizes;
import org.xtext.dungeonDSL.RoomTypes;
import org.xtext.dungeonDSL.Trap;
import org.xtext.dungeonDSL.DungeonDSLPackage.Literals;

public class DungeonDSLValidator extends AbstractDungeonDSLValidator {

	/** 1) Unique Room names across the entire dungeon → now an ERROR */
	@Check
	public void checkUniqueRoomNames(Dungeon dungeon) {
		Set<String> seen = new HashSet<>();
		for (Floor floor : dungeon.getFloors()) {
			for (Room room : floor.getRooms()) {
				if (!seen.add(room.getName())) {
					error("Room name '" + room.getName() + "' is duplicated", room, Literals.ROOM__NAME,
							"duplicate-room-name");
				}
			}
		}
	}

	/** 2) Valid Room connections: existence, no self‐link, symmetry */
	@Check
	public void checkRoomConnections(Room room) {
		Dungeon dungeon = (Dungeon) EcoreUtil.getRootContainer(room);

		for (String targetName : room.getConnections()) {
			Room target = findRoomByName(dungeon, targetName);
			if (target == null) {
				error("Unknown connection: no room named '" + targetName + "'", room, Literals.ROOM__CONNECTIONS,
						"invalid-connection");
			} else if (target == room) {
				error("A room cannot connect to itself", room, Literals.ROOM__CONNECTIONS, "self-connection");
			} else if (!target.getConnections().contains(room.getName())) {
				error("Connections must be symmetric: '" + room.getName() + "' ↔ '" + target.getName() + "'", room,
						Literals.ROOM__CONNECTIONS, "asymmetric-connection");
			}
		}
	}

	private Room findRoomByName(Dungeon dungeon, String name) {
		for (Floor f : dungeon.getFloors()) {
			for (Room r : f.getRooms()) {
				if (r.getName().equals(name)) {
					return r;
				}
			}
		}
		return null;
	}

	/** 3) Trap probability and usage in PUZZLE rooms */
	@Check
	public void checkTrapProbability(Trap trap) {
		int chance = trap.getTriggerChance();
		if (chance < 0 || chance > 100) {
			error("triggerChance must be 0–100 (found " + chance + ")", trap, Literals.TRAP__TRIGGER_CHANCE,
					"invalid-probability");
		}
	}

	 @Check
	  public void checkTrapUsageInPuzzle(Trap trap) {
	    Room room = (Room) trap.eContainer();
	    if (trap.getDisarmable() == BOOLEAN.FALSE && room.getType() == RoomTypes.PUZZLE) {
	      error("Non‐disarmable traps cannot be placed in PUZZLE rooms", trap, Literals.TRAP__DISARMABLE,
	        "trap-in-puzzle");
	    }
	  }

	/** 4) Floor must contain a COMBAT room → still a WARNING by default */
	@Check
	public void checkFloorHasCombat(Floor floor) {
		boolean hasCombat = floor.getRooms().stream().anyMatch(r -> r.getType() == RoomTypes.COMBAT);
		if (!hasCombat) {
			// you can switch this to `error(...)` if you prefer red underlines here too
			warning("Each floor should contain at least one COMBAT room", floor, Literals.FLOOR__ROOMS,
					"missing-combat-room");
		}
	}

	/** 5) SHOP rooms must not be LARGE */
	@Check
	public void checkShopSize(Floor floor) {
		for (Room r : floor.getRooms()) {
			if (r.getType() == RoomTypes.SHOP && r.getSize() == Sizes.LARGE) {
				error("SHOP rooms must be SMALL or MEDIUM (found LARGE)", r, Literals.ROOM__SIZE, "invalid-shop-size");
			}
		}
	}
}
