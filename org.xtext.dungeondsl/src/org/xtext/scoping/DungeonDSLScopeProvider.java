package org.xtext.scoping; // Keep this package as it's for the scope provider itself

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
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
import org.xtext.dungeonDSL.DungeonDSLPackage;

import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceDescription;
import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.EcoreUtil2;

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
        if (reference == DungeonDSLPackage.Literals.ROOM__CONNECTIONS) {
            Model currentModel = EcoreUtil2.getContainerOfType(context, Model.class);
            if (currentModel == null) {
                return super.getScope(context, reference);
            }

            List<IEObjectDescription> visibleObjects = new ArrayList<>();

            // 1. Add all Rooms defined within the current file
            // First, check if there's a main Dungeon and add its Rooms
            if (currentModel.getDungeon() != null) {
                for (Floor floor : currentModel.getDungeon().getFloors()) {
                    for (IEObjectDescription roomDesc : Scopes.scopedElementsFor(floor.getRooms())) {
                        visibleObjects.add(roomDesc);
                    }
                }
            }

            // Then, add rooms from top-level ModularElements
            for (ModularElement element : currentModel.getElements()) {
                if (element instanceof Floor) {
                    Floor f = (Floor) element;
                    for (IEObjectDescription roomDesc : Scopes.scopedElementsFor(f.getRooms())) {
                        visibleObjects.add(roomDesc);
                    }
                } else if (element instanceof Room) {
                    Iterables.addAll(visibleObjects, Scopes.scopedElementsFor(Collections.singletonList((Room) element)));
                } 
                if (element instanceof Trap) {
                    Iterables.addAll(visibleObjects, Scopes.scopedElementsFor(Collections.singletonList((Trap) element)));
                }
                if (element instanceof NPC) {
                    Iterables.addAll(visibleObjects, Scopes.scopedElementsFor(Collections.singletonList((NPC) element)));
                }
            }

            // 2. Process import statements - now handling specific imports
            for (ImportStatement importStatement : currentModel.getImports()) {
                String importURIString = importStatement.getImportURI();
                
                // Skip if no URI is provided
                if (importURIString == null || importURIString.isEmpty()) {
                    continue;
                }

                URI resolvedImportURI = resolveImportURI(context, importURIString);
                if (resolvedImportURI == null) continue;
                
                IResourceDescription importedDesc = resourceDescriptions.getResourceDescription(resolvedImportURI);
                if (importedDesc == null) continue;

                // Handle different import types
                if (importStatement.getSpecificImports() != null) {
                    // Specific imports - only import referenced elements
                    for (ModularElement specificElement : importStatement.getSpecificImports().getImportedElements()) {
                        String elementName = specificElement.getName();
                        
                        // Find matching elements in the imported resource
                        for (IEObjectDescription desc : importedDesc.getExportedObjects()) {
                            if (desc.getName().getLastSegment().equals(elementName)) {
                                visibleObjects.add(desc);
                            }
                        }
                    }
                } else {
                    // Import all elements from the resource (old behavior)
                    Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                            DungeonDSLPackage.Literals.ROOM));
                    Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                            DungeonDSLPackage.Literals.TRAP));
                    Iterables.addAll(visibleObjects, importedDesc.getExportedObjectsByType(
                            DungeonDSLPackage.Literals.NPC));
                }
            }

            return new SimpleScope(visibleObjects);
        }

        // Fallback to default scoping for all other references
        return super.getScope(context, reference);
    }

    // Helper method to resolve import URIs
    private URI resolveImportURI(EObject context, String importURIString) {
        // Get the IResourceDescription for the current resource
        IResourceDescription currentResourceDesc = null;
        if (context.eResource() != null) {
            currentResourceDesc = resourceDescriptions.getResourceDescription(context.eResource().getURI());
        }

        if (currentResourceDesc != null) {
            try {
                return currentResourceDesc.getURI().resolve(URI.createURI(importURIString));
            } catch (IllegalArgumentException e) {
                // Error handling
            }
        }
        
        try {
            return URI.createURI(importURIString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}