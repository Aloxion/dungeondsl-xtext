from enum import Enum
from typing import List, Optional


class Sizes(Enum):
    LARGE = "LARGE"
    MEDIUM = "MEDIUM"
    SMALL = "SMALL"


class RoomTypes(Enum):
    COMBAT = "COMBAT"
    TREASURE = "TREASURE"
    BOSS = "BOSS"
    PUZZLE = "PUZZLE"
    SHOP = "SHOP"


class EventTrigger(Enum):
    STEP_ON = "stepOn"
    OPEN_DOOR = "openDoor"


class Behaviour(Enum):
    AGGRESSIVE = "AGGRESSIVE"
    NEUTRAL = "NEUTRAL"


class NPCType(Enum):
    MERCHANT = "MERCHANT"
    ENEMY = "ENEMY"
    NORMAL = "NORMAL"


class Dungeon:
    def __init__(self, name: str, theme: str, lvl: int):
        self.name = name
        self.theme = theme
        self.lvl = lvl
        self.floors: List[Dungeon.Floor] = []

    def add_floor(self, floor):
        self.floors.append(floor)

    class Floor:
        def __init__(self, name: str):
            self.name = name
            self.rooms: List[Dungeon.Room] = []

        def add_room(self, room):
            self.rooms.append(room)

    class Room:
        def __init__(self, name: str, size: Sizes, room_type: RoomTypes, floor_id: str, connections: List[str]):
            self.name = name
            self.size = size
            self.room_type = room_type
            self.floor_id = floor_id
            self.connections = connections
            self.traps: List[Dungeon.Trap] = []

        def add_trap(self, trap):
            self.traps.append(trap)

    class Trap:
        def __init__(self, name: str, trigger: EventTrigger, disarmable: bool, trigger_chance: int):
            self.name = name
            self.trigger = trigger
            self.disarmable = disarmable
            self.trigger_chance = trigger_chance

    class NPC:
        def __init__(self, name: str, behaviour: Behaviour, npc_type: NPCType, health: int):
            self.name = name
            self.behaviour = behaviour
            self.npc_type = npc_type
            self.health = health
            
dung = Dungeon("Cave of Wonders", "Cave", 1)

dung.floors.append(
    floor = Dungeon.Floor("First Floor")
)

 	
 	    #     {
        #   "dungeon": {
        #     "name": "«escape(dungeon.name)»",
        #     "theme": "«escape(dungeon.theme)»",
        #     "level": «dungeon.lvl»,
        #     "floors": [
        #       «FOR floor : dungeon.floors SEPARATOR ','»
        #         «generateFloorJson(floor)»
        #       «ENDFOR»
        #     ]
        #   }
        # }