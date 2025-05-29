package org.xtext.validation;

import org.eclipse.xtext.validation.Check;
import org.xtext.dungeonDSL.Room;

import org.xtext.dungeonDSL.DungeonDSLPackage;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.xtext.dungeonDSL.ImportStatement;
import org.xtext.dungeonDSL.ModularElement;
import org.xtext.dungeonDSL.SpecificImports;

public class DungeonDSLValidator extends AbstractDungeonDSLValidator {

	public static final String CONNECTIONS_EMPTY = "connectionsCannotBeEmpty";

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
		// Only check specific imports (with curly braces syntax)
		if (importStatement.getSpecificImports() == null) {
			return;
		}

		SpecificImports specificImports = importStatement.getSpecificImports();
		String importURIString = specificImports.getImportURI();
		if (importURIString == null || importURIString.isEmpty()) {
			error("Import URI cannot be empty",
				  specificImports, // <-- Change here: Pass specificImports as context
				  DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
			return;
		}

		// Check if the referenced elements actually exist in the imported file
		URI resolvedImportURI = resolveImportURI(importStatement, importURIString);
		if (resolvedImportURI == null) {
			error("Could not resolve import URI: " + importURIString,
				  specificImports, // <-- Change here: Pass specificImports as context
				  DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
			return;
		}

		// Get a description of the imported resource
		IResourceServiceProvider.Registry registry = IResourceServiceProvider.Registry.INSTANCE;
		IResourceServiceProvider resourceServiceProvider = registry.getResourceServiceProvider(resolvedImportURI);

		if (resourceServiceProvider == null) {
			error("No resource service provider found for URI: " + resolvedImportURI,
				  specificImports, // <-- Change here: Pass specificImports as context
				  DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
			return;
		}

		IResourceDescriptions resourceDescriptions = resourceServiceProvider
			.get(IResourceDescriptions.class);

		IResourceDescription importedDesc = resourceDescriptions.getResourceDescription(resolvedImportURI);
		if (importedDesc == null) {
			error("Could not find imported resource: " + importURIString,
				  specificImports, // <-- Change here: Pass specificImports as context
				  DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORT_URI);
			return;
		}

		// Check each imported element
		for (ModularElement element : specificImports.getImportedElements()) {
			String elementName = element.getName();
			boolean elementFound = false;

			for (IEObjectDescription desc : importedDesc.getExportedObjects()) {
				if (desc.getName().getLastSegment().equals(elementName)) {
					elementFound = true;
					break;
				}
			}

			if (!elementFound) {
				error("Element '" + elementName + "' not found in imported resource",
					  specificImports, // <-- Change here: Pass specificImports as context
					  DungeonDSLPackage.Literals.SPECIFIC_IMPORTS__IMPORTED_ELEMENTS,
					  specificImports.getImportedElements().indexOf(element));
			}
		}
	}

	// Helper method to resolve import URIs
	private URI resolveImportURI(EObject context, String importURIString) {
		// Get the containing resource URI
		if (context.eResource() == null || context.eResource().getURI() == null) {
			return null;
		}

		URI baseURI = context.eResource().getURI();
		try {
			// Remove the file name part to get the directory
			URI baseDir = baseURI.trimSegments(1);
			// Resolve the import URI against the directory
			return URI.createURI(importURIString).resolve(baseDir);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}