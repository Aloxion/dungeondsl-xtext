package org.xtext.validation;

import org.eclipse.xtext.validation.Check;
import org.xtext.dungeonDSL.Room;

import org.xtext.dungeonDSL.DungeonDSLPackage;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.xtext.dungeonDSL.ImportStatement;
import org.xtext.dungeonDSL.Model;
import org.xtext.dungeonDSL.ModularElement;
import org.xtext.dungeonDSL.SpecificImports;

import com.google.inject.Inject;

public class DungeonDSLValidator extends AbstractDungeonDSLValidator {

	public static final String CONNECTIONS_EMPTY = "connectionsCannotBeEmpty";
	
    @Inject
    private IResourceDescriptions resourceDescriptions;
    
	@Check
	public void checkRoomConnectionsNotEmpty(Room room) {
		// Since we now want to allow empty connections,
		// we're commenting out the validation code that requires connections

		// If you want to keep this validation as a warning instead of an error:
		// if (isPartOfMainResourceModel && room.getConnections().isEmpty()) {
		//     warning("Room has no connections. Consider connecting it to another room.",
		//           DungeonDSLPackage.Literals.ROOM__CONNECTIONS);
		// }
	}

	@Check
	public void checkImportSpecificElements(ImportStatement importStatement) {
		System.out.println("------- NEW VALIDATOR RUN -------" + '\n');
		System.out.println("VALIDATOR DEBUG: Running checkImportSpecificElements for import statement: " + importStatement.getSpecificImports());
        try { // Added try-catch for debugging EValidator error
            // Only check specific imports (with curly braces syntax)
            if (importStatement.getSpecificImports() == null) {
                // This is a regular import (importURI=STRING), not a specific import.
                return;
            }

            SpecificImports specificImports = importStatement.getSpecificImports();
            String importURIString = specificImports.getImportURI();
            if (importURIString == null || importURIString.isEmpty()) {
                error("Import URI cannot be empty for specific imports",
                        specificImports,
                        DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
                return;
            }

            // Check if the referenced elements actually exist in the imported file
            URI resolvedImportURI = resolveImportURI(importStatement, importURIString);
            if (resolvedImportURI == null) {
                error("Could not resolve import URI: '" + importURIString + "'",
                        specificImports,
                        DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
                return;
            }

            IResourceDescription importedDesc = resourceDescriptions.getResourceDescription(resolvedImportURI);
            System.out.println("VALIDATOR DEBUG: Resolved import URI: " + resolvedImportURI);
       
            if (importedDesc == null) {
                error("Could not find imported resource: '" + importURIString + "'",
                        specificImports,
                        DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
                return;
            }

            // Check each imported element
            for (ModularElement element : specificImports.getImportedElements()) {
                String elementName = element.getName(); // Get the name from the specific import reference
                if (elementName == null) { // Handle unresolved proxies or malformed elements
                    error("Imported element reference is unresolved or invalid.",
                          DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORTED_ELEMENTS,
                          specificImports.getImportedElements().indexOf(element));
                    continue;
                }

                boolean elementFound = false;
                for (IEObjectDescription desc : importedDesc.getExportedObjects()) {
                    if (desc.getName().getLastSegment().equals(elementName)) {
                        if (DungeonDSLPackage.Literals.ROOM.isSuperTypeOf(desc.getEClass()) ||
                            DungeonDSLPackage.Literals.TRAP.isSuperTypeOf(desc.getEClass()) ||
                            DungeonDSLPackage.Literals.NPC.isSuperTypeOf(desc.getEClass()) ||
                            DungeonDSLPackage.Literals.FLOOR.isSuperTypeOf(desc.getEClass())) {
                            elementFound = true;
                            break;
                        }
                    }
                }

                if (!elementFound) {
                    error("Element '" + elementName + "' not found or not a valid modular element in imported resource",
                          DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORTED_ELEMENTS,
                          specificImports.getImportedElements().indexOf(element));
                }
            }
        } catch (Exception e) {
            // Catch any unexpected exceptions during validation and report them to console
            System.err.println("ERROR in checkImportSpecificElements validator: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            // Optionally, add a generic error to the model itself
            error("An internal error occurred during specific import validation. See console for details.",
                  DungeonDSLPackage.Literals.MODEL__IMPORTS, // Point to the imports feature of the Model
                  importStatement.eContainer() instanceof Model ? ((Model)importStatement.eContainer()).getImports().indexOf(importStatement) : 0);
        }
	}

	private URI resolveImportURI(EObject context, String importURIString) {
	    URI resolvedImportURI = null;
	    IResourceDescription currentResourceDesc = null;
	    if (context.eResource() != null) {
	        currentResourceDesc = resourceDescriptions.getResourceDescription(context.eResource().getURI());
	    }

	    System.out.println("VALIDATOR DEBUG: Current resource description: " + (currentResourceDesc != null ? currentResourceDesc.getURI() : "null"));

	    if (currentResourceDesc != null) {
	        try {
	            // Use the URI from the IResourceDescription as the base for resolution
	            URI baseURI = currentResourceDesc.getURI().trimSegments(1); // Get directory of current resource
	            System.out.println("VALIDATOR DEBUG: Base URI for resolution: " + baseURI); // This is crucial
	            System.out.println("VALIDATOR DEBUG: Import URI string: " + importURIString); // Also crucial
	            String fixedImportURIString = '/' + importURIString; // Ensure it starts with a slash
	            String finalURI = baseURI + fixedImportURIString; // Concatenate base URI and import URI string
	            System.out.println("VALIDATOR DEBUG: Final URI to resolve: " + finalURI);
	            
	            resolvedImportURI = URI.createURI(finalURI).resolve(baseURI);
	            // System.out.println("VALIDATOR DEBUG: Resolved '" + importURIString + "' against '" + baseURI + "' to '" + resolvedImportURI + "'");
	        } catch (IllegalArgumentException e) {
	            System.err.println("VALIDATOR ERROR: Error resolving import URI '" + importURIString + "' against base '" + currentResourceDesc.getURI() + "': " + e.getMessage());
	        }
	    } else {
	        // Fallback if currentResourceDesc is null (e.g., resource not in workspace, transient state)
	        try {
	            resolvedImportURI = URI.createURI(importURIString);
//	            System.out.println("VALIDATOR DEBUG: Directly created URI: '" + resolvedImportURI + "' from '" + importURIString + "'");
	        } catch (IllegalArgumentException e) {
	            System.err.println("VALIDATOR ERROR: Error creating direct URI from import string: " + importURIString + ". Error: " + e.getMessage());
	        }
	    }
	    return resolvedImportURI;
	}

    /**
     * Debugging check: Prints all resources known to the IResourceDescriptions service
     * and the elements they export. This helps to understand what Xtext has indexed.
     */
//    @Check
//    public void debugPrintProjectTree(Model model) { // Runs on the root Model of the current file
//        System.out.println("\n--- VALIDATOR DEBUG: Project Resource Tree ---");
//        System.out.println("Current file being validated: " + model.eResource().getURI());
//
//        if (resourceDescriptions == null) {
//            System.out.println("IResourceDescriptions service not injected or available.");
//            return;
//        }
//
//        for (IResourceDescription resourceDesc : resourceDescriptions.getAllResourceDescriptions()) {
//            System.out.println("Resource URI: " + resourceDesc.getURI());
//            System.out.println("  Exported Elements:");
//            if (resourceDesc.getExportedObjects().iterator().hasNext()) {
//                for (IEObjectDescription objDesc : resourceDesc.getExportedObjects()) {
//                    System.out.println("    - Name: " + objDesc.getName() + ", EClass: " + objDesc.getEClass().getName() + ", URI: " + objDesc.getEObjectURI());
//                }
//            } else {
//                System.out.println("    (No exported elements)");
//            }
//        }
//        System.out.println("--- END Project Resource Tree ---\n");
//    }
}