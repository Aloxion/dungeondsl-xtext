
package org.xtext.validation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
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

	/** 6) Boss Room Requirements */
	@Check
	public void checkBossRoomRequirements(Room room) {
		if (room.getType() == RoomTypes.BOSS) {
			if (room.getSize() != Sizes.LARGE) {
				error("Boss rooms must be LARGE", room, Literals.ROOM__SIZE, "boss-room-size");
			}
			if (room.getConnections().size() > 1) {
				warning("Boss rooms should have at most one entrance", room, Literals.ROOM__CONNECTIONS,
						"boss-room-connections");
			}
		}
	}

	/** 7) Balanced Distribution */
	@Check
	public void checkRoomDistribution(Floor floor) {
		int total = floor.getRooms().size();
		if (total <= 1)
			return;

		long treasureCount = floor.getRooms().stream().filter(r -> r.getType() == RoomTypes.TREASURE).count();

		if (treasureCount > total / 3) {
			warning("Too many treasure rooms on this floor (max recommended: " + (total / 3) + ")", floor,
					Literals.FLOOR__ROOMS, "too-many-treasure-rooms");
		}
	}

	/** 8) Path Connectivity - Check if all rooms are connected */
	@Check
	public void checkAllRoomsConnected(Floor floor) {
		if (floor.getRooms().isEmpty() || floor.getRooms().size() == 1)
			return;

		Set<String> visited = new HashSet<>();
		Queue<Room> queue = new LinkedList<>();

		// Start from the first room
		Room startRoom = floor.getRooms().get(0);
		queue.add(startRoom);
		visited.add(startRoom.getName());

		Dungeon dungeon = (Dungeon) EcoreUtil.getRootContainer(floor);

		while (!queue.isEmpty()) {
			Room current = queue.poll();

			for (String connName : current.getConnections()) {
				Room connRoom = findRoomByName(dungeon, connName);
				if (connRoom != null && !visited.contains(connRoom.getName())) {
					queue.add(connRoom);
					visited.add(connRoom.getName());
				}
			}
		}

		// Check if all rooms in this floor are visited (connected)
		for (Room room : floor.getRooms()) {
			if (!visited.contains(room.getName())) {
				error("Room '" + room.getName() + "' is isolated and not connected to other rooms", room, null,
						"isolated-room");
			}
		}
	}
}
