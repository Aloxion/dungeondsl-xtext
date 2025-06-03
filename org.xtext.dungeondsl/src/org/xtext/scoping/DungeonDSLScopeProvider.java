package org.xtext.scoping; // Keep this package as it's for the scope provider itself

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;

import org.xtext.dungeonDSL.Floor;
import org.xtext.dungeonDSL.ImportStatement;
import org.xtext.dungeonDSL.Room;
import org.xtext.dungeonDSL.Model;
import org.xtext.dungeonDSL.ModularElement;
import org.xtext.dungeonDSL.Trap;
import org.xtext.dungeonDSL.NPC;
import org.xtext.dungeonDSL.Dungeon;
import org.xtext.dungeonDSL.DungeonDSLPackage;

import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceDescription;
import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;

import com.google.common.collect.Iterables;
/**
 * This class contains custom scoping description.
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
public class DungeonDSLScopeProvider extends AbstractDungeonDSLScopeProvider {

    @Inject
    private IResourceDescriptions resourceDescriptions;

    @Override
    public IScope getScope(EObject context, EReference reference) {
        if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS || 
            reference.getEReferenceType() == DungeonDSLPackage.Literals.ROOM ||
            reference.getEReferenceType() == DungeonDSLPackage.Literals.TRAP ||
            reference.getEReferenceType() == DungeonDSLPackage.Literals.NPC) {
            
            Model currentModel = EcoreUtil2.getContainerOfType(context, Model.class);
            if (currentModel == null) {
                return super.getScope(context, reference);
            }

            List<IEObjectDescription> visibleObjects = new ArrayList<>();
            
            // Add local elements first
            for (ModularElement element : currentModel.getElements()) {
                // Add if the element is the type we're looking for
                if (isElementOfRightType(element, reference)) {
                    visibleObjects.add(EObjectDescription.create(QualifiedName.create(element.getName()), element));
                }
            }
            
            Dungeon currentDungeon = currentModel.getDungeon();
            
            if (currentDungeon != null) {
                for (Floor floor : currentDungeon.getFloors()) {
                    for (Room room : floor.getRooms()) {
                        if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS ||
                            reference.getEReferenceType() == DungeonDSLPackage.Literals.ROOM) {
                            visibleObjects.add(EObjectDescription.create(QualifiedName.create(room.getName()), room));
                        }
                    }
                }
            }
            
            // Process import statements
            for (ImportStatement importStatement : currentModel.getImports()) {
                String importURIString;
                
                if (importStatement.getSpecificImports() != null) {
                    importURIString = importStatement.getSpecificImports().getImportURI();
                } else {
                    importURIString = importStatement.getImportURI();
                }
                
                if (importURIString == null || importURIString.isEmpty()) {
                    continue;
                }
                
                // Properly resolve the URI against the current resource
                URI resolvedImportURI = resolveImportURI(context, importURIString);
                
                IResourceDescription importedDesc = resourceDescriptions.getResourceDescription(resolvedImportURI);
                if (importedDesc == null) continue;
                
                // Handle specific imports
                if (importStatement.getSpecificImports() != null) {
                    for (ModularElement specificElement : importStatement.getSpecificImports().getImportedElements()) {
                        String elementName = specificElement.getName();
                        
                        for (IEObjectDescription desc : importedDesc.getExportedObjects()) {
                            if (desc.getName().getLastSegment().equals(elementName)) {
                                // Check if this element matches the reference type we're looking for
                                EClass typeToMatch;
                                if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS) {
                                    typeToMatch = DungeonDSLPackage.Literals.ROOM;
                                } else {
                                    typeToMatch = reference.getEReferenceType();
                                }
                                
                                if (typeToMatch.isSuperTypeOf(desc.getEClass()) ||
                                    desc.getEClass().isSuperTypeOf(typeToMatch)) {
                                    visibleObjects.add(desc);
                                }
                            }
                        }
                    }
                } else {
                    // Regular imports - add all elements of the requested type
                    if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS || 
                        reference.getEReferenceType() == DungeonDSLPackage.Literals.ROOM) {
                        Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                                DungeonDSLPackage.Literals.ROOM));
                    } else if (reference.getEReferenceType() == DungeonDSLPackage.Literals.TRAP) {
                        Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                                DungeonDSLPackage.Literals.TRAP));
                    } else if (reference.getEReferenceType() == DungeonDSLPackage.Literals.NPC) {
                        Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                                DungeonDSLPackage.Literals.NPC));
                    }
                }
            }

            return new SimpleScope(visibleObjects);
        }

        // Fallback to default scoping for all other references
        return super.getScope(context, reference);
    }

    // Helper method to check if an element is of the right type for a reference
    private boolean isElementOfRightType(ModularElement element, EReference reference) {
        if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS || 
            reference.getEReferenceType() == DungeonDSLPackage.Literals.ROOM) {
            return element instanceof Room;
        } else if (reference.getEReferenceType() == DungeonDSLPackage.Literals.TRAP) {
            return element instanceof Trap;
        } else if (reference.getEReferenceType() == DungeonDSLPackage.Literals.NPC) {
            return element instanceof NPC;
        }
        return false;
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

}