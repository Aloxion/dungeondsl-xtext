package org.xtext.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import static org.junit.jupiter.api.Assertions.*
import org.eclipse.xtext.generator.IGenerator2
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.generator.InMemoryFileSystemAccess
import org.xtext.dungeonDSL.Dungeon
import org.xtext.tests.DungeonDSLInjectorProvider


@ExtendWith(InjectionExtension)
@InjectWith(DungeonDSLInjectorProvider)
class DungeonDSLGeneratorTest {

  @Inject
  ParseHelper<Dungeon> parseHelper

  @Inject
  IGenerator2 generator

  @Test
  def void testPythonCodeGeneration() {
    val model = parseHelper.parse('''
      Dungeon MyTest {
        theme = "Adventure"
        lvl = 42
        Floor F1 {
          Room A {
            size = SMALL
            type = COMBAT
            connections = []
            Trap spike {
              trigger = stepOn
              disarmable = true
              triggerChance = 10
            }
          }
        }
      }
    ''')

    val fsa = new InMemoryFileSystemAccess
    generator.doGenerate(model.eResource, fsa, null)

    val content = fsa.allFiles.get("MyTest.py")?.toString

    // Ensure file was generated
    assertNotNull(content, "Expected Python file not generated")

    // Check expected content
    assertTrue(content.contains("Generated Dungeon: MyTest"), "Missing dungeon header")
    assertTrue(content.contains("class Sizes"), "Expected 'class Sizes' definition")
    assertTrue(content.contains("spike"), "Expected trap name 'spike' to appear in output")
  }
}
