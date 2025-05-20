
package org.xtext.validation;

// Standard Java collections for tracking room names and BFS traversal
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

// EMF and Xtext utilities for model traversal and validation
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.validation.Check;
// Generated model classes from the DSL grammar
import org.xtext.dungeonDSL.BOOLEAN;
import org.xtext.dungeonDSL.Dungeon;
import org.xtext.dungeonDSL.Floor;
import org.xtext.dungeonDSL.Room;
import org.xtext.dungeonDSL.Sizes;
import org.xtext.dungeonDSL.RoomTypes;
import org.xtext.dungeonDSL.Trap;
import org.xtext.dungeonDSL.DungeonDSLPackage.Literals;

/**
 * Custom validator for DungeonDSL that enforces game design rules and ensures
 * dungeon consistency and playability.
 */
public class DungeonDSLValidator extends AbstractDungeonDSLValidator {

	/**
	 * 1) Unique Room Names Ensures every room has a unique name across the entire
	 * dungeon, which prevents ambiguity in room references and connections.
	 */
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

	/**
	 * 2) Room Connections Validation Validates three properties of room
	 * connections: - References must point to existing rooms - A room cannot
	 * connect to itself - Connections must be bidirectional (symmetric) These
	 * ensure the dungeon layout is navigable and coherent.
	 */
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

	/**
	 * Helper method to find a room by name across all floors in the dungeon.
	 * 
	 * @param dungeon The dungeon to search in
	 * @param name    Room name to find
	 * @return The room object if found, null otherwise
	 */
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

	/**
	 * 3a) Trap Probability Range Check Ensures trap trigger chances are valid
	 * percentages (0-100).
	 */
	@Check
	public void checkTrapProbability(Trap trap) {
		int chance = trap.getTriggerChance();
		if (chance < 0 || chance > 100) {
			error("triggerChance must be 0–100 (found " + chance + ")", trap, Literals.TRAP__TRIGGER_CHANCE,
					"invalid-probability");
		}
	}

	/**
	 * 3b) Puzzle Room Trap Rules Ensures that traps in puzzle rooms must be
	 * disarmable, which prevents players from getting stuck in puzzle rooms.
	 */
	@Check
	public void checkTrapUsageInPuzzle(Trap trap) {
		Room room = (Room) trap.eContainer();
		if (trap.getDisarmable() == BOOLEAN.FALSE && room.getType() == RoomTypes.PUZZLE) {
			error("Non‐disarmable traps cannot be placed in PUZZLE rooms", trap, Literals.TRAP__DISARMABLE,
					"trap-in-puzzle");
		}
	}

	/**
	 * 4) Combat Room Requirement Encourages at least one combat room per floor for
	 * balanced gameplay with a mix of exploration and combat encounters.
	 */
	@Check
	public void checkFloorHasCombat(Floor floor) {
		boolean hasCombat = floor.getRooms().stream().anyMatch(r -> r.getType() == RoomTypes.COMBAT);
		if (!hasCombat) {
			// Using warning instead of error makes this a design guideline rather than
			// strict rule
			warning("Each floor should contain at least one COMBAT room", floor, Literals.FLOOR__ROOMS,
					"missing-combat-room");
		}
	}

	/**
	 * 5) Shop Size Restriction Enforces that shop rooms must be appropriately sized
	 * (SMALL or MEDIUM). Large shops would be unrealistic in this dungeon setting.
	 */
	@Check
	public void checkShopSize(Floor floor) {
		for (Room r : floor.getRooms()) {
			if (r.getType() == RoomTypes.SHOP && r.getSize() == Sizes.LARGE) {
				error("SHOP rooms must be SMALL or MEDIUM (found LARGE)", r, Literals.ROOM__SIZE, "invalid-shop-size");
			}
		}
	}

	/**
	 * 6) Boss Room Requirements Enforces design rules for boss rooms: - Boss rooms
	 * must be LARGE in size (throws error if not) - Boss rooms should have limited
	 * entrances (warning if more than one connection) This ensures boss rooms are
	 * appropriately challenging and control player flow.
	 */
	@Check
	public void checkBossRoomRequirements(Room room) {
		if (room.getType() == RoomTypes.BOSS) {
			// Boss rooms need sufficient space for epic encounters
			if (room.getSize() != Sizes.LARGE) {
				error("Boss rooms must be LARGE", room, Literals.ROOM__SIZE, "boss-room-size");
			}
			// Boss rooms should be strategic dead-ends with limited escape routes
			if (room.getConnections().size() > 1) {
				warning("Boss rooms should have at most one entrance", room, Literals.ROOM__CONNECTIONS,
						"boss-room-connections");
			}
		}
	}

	/**
	 * 7) Balanced Distribution Checks if treasure rooms are reasonably distributed
	 * within a floor. Warns if more than 1/3 of rooms are treasure rooms, which
	 * would create gameplay imbalance by providing too many rewards without
	 * sufficient challenges.
	 */
	@Check
	public void checkRoomDistribution(Floor floor) {
		int total = floor.getRooms().size();
		if (total <= 1)
			return; // Skip validation for floors with 0-1 rooms

		// Calculate percentage of treasure rooms
		long treasureCount = floor.getRooms().stream().filter(r -> r.getType() == RoomTypes.TREASURE).count();

		// Warn if treasure rooms exceed 1/3 of total rooms
		if (treasureCount > total / 3) {
			warning("Too many treasure rooms on this floor (max recommended: " + (total / 3) + ")", floor,
					Literals.FLOOR__ROOMS, "too-many-treasure-rooms");
		}
	}

	/**
	 * 8) Path Connectivity - Check if all rooms are connected Uses a breadth-first
	 * search algorithm to ensure all rooms in a floor are reachable from at least
	 * one other room. This prevents isolated rooms that players cannot access.
	 */
	@Check
	public void checkAllRoomsConnected(Floor floor) {
		if (floor.getRooms().isEmpty() || floor.getRooms().size() == 1)
			return; // No connectivity to check with 0-1 rooms

		Set<String> visited = new HashSet<>(); // Tracks rooms we've seen
		Queue<Room> queue = new LinkedList<>(); // BFS queue for traversal

		// Start BFS from the first room
		Room startRoom = floor.getRooms().get(0);
		queue.add(startRoom);
		visited.add(startRoom.getName());

		Dungeon dungeon = (Dungeon) EcoreUtil.getRootContainer(floor);

		// BFS traversal to mark all connected rooms as visited
		while (!queue.isEmpty()) {
			Room current = queue.poll();

			// Check all connections from the current room
			for (String connName : current.getConnections()) {
				Room connRoom = findRoomByName(dungeon, connName);
				if (connRoom != null && !visited.contains(connRoom.getName())) {
					queue.add(connRoom);
					visited.add(connRoom.getName());
				}
			}
		}

		// After traversal, any unvisited room is isolated
		for (Room room : floor.getRooms()) {
			if (!visited.contains(room.getName())) {
				error("Room '" + room.getName() + "' is isolated and not connected to other rooms", room, null,
						"isolated-room");
			}
		}
	}
}
