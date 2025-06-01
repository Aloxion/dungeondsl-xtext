package org.xtext.tests

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.util.stream.Collectors

class DungeonDSLStableGenerationTest {

	/**
	 * Attempt to locate the src-gen directory where generated Python files are placed.
	 * Dynamically search from the current directory upwards until it finds the project root "dungeondsl-xtext".
	*/
	def static Path findSrcGenFolder() {
		var path = Paths.get(System.getProperty("user.dir")).toAbsolutePath

		while (path !== null && !path.fileName.toString.equals("dungeondsl-xtext")) {
			path = path.parent
		}

		if (path === null) {
			throw new IllegalStateException("Could not find 'dungeondsl-xtext' in the directory path")
		}
		
		return path.resolveSibling("runtime-EclipseApplication").resolve("Dungeon").resolve("src-gen")
	}


	/**
	 * Verifies that the generated Python files are stable between versions.
	 * For every _v1.py file found in the generated output directory, the test looks for a matching _v2.py file.
	 * It then asserts that the contents of the two files are byte-for-byte identical.
	*/
	@Test
	def void testAllStableCodeGenerationPairs() {
		val srcGenDir = findSrcGenFolder()
		assertTrue(Files.exists(srcGenDir), "src-gen directory does not exist")

		// Find all _v1.py files
	    val v1Files = Files.list(srcGenDir)
	      .filter[it.toString.endsWith("_v1.py")]
	      .collect(Collectors.toList)
	      
		// Make sure we have something to test
		assertFalse(v1Files.empty, "No _v1.py files found in src-gen folder. Nothing to test.")
		println("Found " + v1Files.size + " _v1.py files. Starting comparisons...")
	
	    for (v1Path : v1Files) {
	      val baseName = v1Path.getFileName.toString.replace("_v1.py", "")
	      val v2Path = srcGenDir.resolve(baseName + "_v2.py")
	      
	      println("Comparing: " + baseName + "_v1.py with " + baseName + "_v2.py")
	      
	      // Check if the second version exists
	      assertTrue(Files.exists(v2Path), "Matching _v2 file not found for: " + baseName)
	
	      val content1 = Files.readAllBytes(v1Path)
	      val content2 = Files.readAllBytes(v2Path)
	
	      assertArrayEquals(
	        content1,
	        content2,
	        "Mismatch between generated outputs for " + baseName
	      )
	    }
    	println("All stable code generation pairs matched successfully.")
	}

}
