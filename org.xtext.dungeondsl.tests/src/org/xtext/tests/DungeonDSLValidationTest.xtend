package org.xtext.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.^extension.ExtendWith
import static org.xtext.validation.DungeonDSLValidator.TRIGGER_CHANCE_OUT_OF_RANGE
import org.junit.jupiter.api.Test
import org.eclipse.xtext.testing.util.ParseHelper
import org.xtext.dungeonDSL.Dungeon
import org.xtext.dungeonDSL.DungeonDSLPackage

@ExtendWith(InjectionExtension)
@InjectWith(DungeonDSLInjectorProvider)
class DungeonDSLValidationTest {

  @Inject
  extension ValidationTestHelper
  
  @Test
  def void testValidation() {
    // Example test using ValidationTestHelper
  }
  
  @Inject
  ParseHelper<Dungeon> parseHelper

  @Test
  def void testInvalidTrapTriggerChance() {
    val model = parseHelper.parse('''
      Dungeon ProbDungeon {
        theme = "Bad Luck"
        lvl = 13
        Floor Main {
          Room Room1 {
            size = MEDIUM
            type = TREASURE
            connections = []
            Trap Explode {
              trigger = stepOn
              disarmable = true
              triggerChance = 150
            }
          }
        }
      }
    ''')

    assertError(
      model.floors.head.rooms.head.traps.head,
      DungeonDSLPackage.Literals.TRAP,
      TRIGGER_CHANCE_OUT_OF_RANGE
    )
  }
  

}
