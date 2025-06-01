package org.xtext.tests

import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.util.stream.Collectors

class DungeonDSLTraceabilityTest {
	
	/**
	* Dynamically locates the 'src-gen' folder in the runtime Eclipse application.
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
	* Checks that at least one generated Python file includes a valid traceability block for a room.
	* A valid trace block contains:
	*  - '# BEGIN ROOM:'      
	*  - a 'Dungeon.Room(' instantiation       
	*  - '# END ROOM:'
	* Also validates that the number of BEGIN and END markers match.
	*/
	@Test
	def void testTraceCommentInGeneratedPython() {
		println("Starting traceability check for rooms in generated files...")
		
		val srcGenDir = findSrcGenFolder()
		assertTrue(Files.exists(srcGenDir), "src-gen directory does not exist")
	
	    // Get all .py files
	    val pyFiles = Files.list(srcGenDir)
	      .filter[it.toString.endsWith(".py")]
	      .collect(Collectors.toList)
	
	    assertFalse(pyFiles.empty, "No .py files found in src-gen folder")
	
	    var traceFound = false
	
	    for (file : pyFiles) {
	      val content = Files.readString(file)
	      
	      val hasBegin = content.contains("# BEGIN ROOM:")
	      val hasEnd = content.contains("# END ROOM:")
	      val hasRoomInit = content.contains("Dungeon.Room(")
	
	      // Check if the file has at least one trace block for a room
	      if (hasBegin && hasEnd && hasRoomInit) {
	      	val beginCount = content.split("# BEGIN ROOM:").length - 1
      		val endCount = content.split("# END ROOM:").length - 1
	        assertEquals(beginCount, endCount, "Mismatched number of BEGIN and END ROOM markers in: " + file.fileName)
	        traceFound = true
	        println("Traceability block correctly found and validated in: " + file.fileName + "\n")
	        return
	      }
	    }
	
		assertTrue(traceFound, "No generated Python file contained the expected trace markers for room1")
	}
	  

	/*
	* Ensures all generated Python files contain traceability comments for rooms.
	*/
	@Test
	def void testTraceabilityCommentsInAllFiles() {
		println("Starting global traceability check for all Python files...")
		
		val srcGenDir = DungeonDSLStableGenerationTest.findSrcGenFolder()
		val pyFiles = Files.list(srcGenDir).filter[it.toString.endsWith(".py")].collect(Collectors.toList)
	
		assertFalse(pyFiles.empty, "No generated Python files found.")
	
		var found = false
		println("Files with traceability markers:")
		for (file : pyFiles) {
			val content = Files.readString(file)
			if (
				content.contains("# BEGIN ROOM:") &&
				content.contains("# END ROOM:") &&
				content.contains("Dungeon.Room(")
			) {
				println("- " + file.fileName)
				found = true
			} else {
				println("No traceability markers found in: " + file.fileName + "\n")
			}
		}
		println("")
		assertTrue(found, "No traceability markers found in any Python file.")
	}
	
	
	
  /* @Test
  def void testTraceCommentInGeneratedPython() {
    val path = Paths.get(
      "C:/Users/mbagg/Documents/Software Model Based/runtime-EclipseApplication/Dungeon/src-gen/TraceTest.py"
    )

    assertTrue(Files.exists(path), "TraceTest.py was not found. Make sure the file was generated.")

    val content = Files.readString(path)

    assertTrue(content.contains("# BEGIN ROOM: room1"), "Trace marker for room 'room1' missing.")
    assertTrue(content.contains("room1_F = Dungeon.Room("), "Room instantiation for 'room1' missing.")
    assertTrue(content.contains("# END ROOM: room1"), "Closing trace marker missing.")
  }
  */
  
  
}
