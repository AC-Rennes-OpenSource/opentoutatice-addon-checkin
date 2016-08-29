
package fr.toutatice.ecm.checkin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioning;
import org.nuxeo.ecm.webapp.versioning.VersionedActions;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.service.url.WebIdResolver;

/**
 * @author Loïc Billon
 *
 */
@Name("checkinActions")
@Scope(CONVERSATION)
public class CheckinActions implements Serializable {

	/**
	 * 
	 */
	private static final String DRAFT_PREFIX = "draft_";

	/**
	 * 
	 */
	private static final String TTC_WEBID = "ttc:webid";

	/**
	 * 
	 */
	private static final long serialVersionUID = -5092427864129643896L;

	/**
	 * 
	 */
	private static final String DRAFTS = "drafts";

	@In(create = true)
	protected transient NavigationContext navigationContext;

	@In(create = true, required = false)
	protected transient CoreSession documentManager;

	@In(create = true, required = false)
	protected transient DocumentActions documentActions;

	@In(create = true, required = false)
	protected transient VersionedActions versionedActions;
    @In(create = true)
    protected transient DocumentVersioning documentVersioning;	

	@In(create = true)
	private LockActions lockActions;


	/**
	 * Réserve le document : déplacement dans l'espace perso, versionning, création du duplicata et verrouillage
	 */
	public String checkin() {
		

		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		
		
		if(!currentDocument.hasFacet("Draft")) {
			currentDocument.checkIn(VersioningOption.MINOR, "Backup before local modifications");
			
			String webId = currentDocument.getPropertyValue(TTC_WEBID).toString();
			String draftId = DRAFT_PREFIX + webId;
			currentDocument.addFacet("Draft");
			currentDocument.setPropertyValue(TTC_WEBID, draftId);
			
			currentDocument = documentManager.saveDocument(currentDocument);
			

			// Déplacement dans espace perso

			DocumentModel draftDoc = documentManager.move(currentDocument.getRef(),
					getDraftFolder(), null);

			
			// Duplication du doc courant
			DocumentModel originalDoc = documentManager.copy(
					currentDocument.getRef(), currentDocument.getParentRef(), null);
			// Pose du verrou
			originalDoc.setPropertyValue(TTC_WEBID, webId);
			originalDoc.addFacet("CheckedIn");
			originalDoc.removeFacet("Draft");
			documentManager.saveDocument(originalDoc);
			lockActions.lockDocument(originalDoc);

			navigationContext.navigateToDocument(draftDoc, "after-edit");
		}

				
		
		return "toutatice_edit";
	}

	/**
	 * Terminer : retour dans le folder d'origine du doc modifié, effactement du duplicata
	 */
	public String release() {

		documentActions.updateCurrentDocument();

		DocumentModel draftDoc = navigationContext.getCurrentDocument();
		String draftId = draftDoc.getPropertyValue(TTC_WEBID).toString();
		String webId = draftId.substring(DRAFT_PREFIX.length());

		// Remise du brouillon dans dossier courant
		DocumentModel originalDoc = WebIdResolver.getLiveDocumentByWebId(
				documentManager, webId);
		draftDoc = documentManager.move(draftDoc.getRef(),
				originalDoc.getParentRef(), null);

		// Suppression du document temporaire
		documentManager.removeDocument(originalDoc.getRef());

		// Restitution du webid
		draftDoc.setPropertyValue(TTC_WEBID, webId);
		draftDoc.removeFacet("Draft");
		documentManager.saveDocument(draftDoc);

		// return navigationContext.navigateToDocument(draftDoc, "after-edit");
		return "done";

	}

	/**
	 *  Retour arrière : retour dans le folder d'origine du doc restauré, effactement du duplicata
	 */
	public String cancelCheckin() {
	
		
		DocumentModel draftDoc = navigationContext.getCurrentDocument();
		String draftId = draftDoc.getPropertyValue(TTC_WEBID).toString();
		String webId = draftId.substring(DRAFT_PREFIX.length());

		// Remise du brouillon dans dossier courant
		DocumentModel originalDoc = WebIdResolver.getLiveDocumentByWebId(
				documentManager, webId);
		draftDoc = documentManager.move(draftDoc.getRef(),
				originalDoc.getParentRef(), null);

		// Suppression du document temporaire
		documentManager.removeDocument(originalDoc.getRef());

		// Restauration de la version
		VersionModel versionModel = documentManager.getVersionsForDocument(draftDoc.getRef()).get(0);
		versionedActions.restoreToVersion(versionModel);
		
		// Restitution du webid
		draftDoc.setPropertyValue(TTC_WEBID, webId);
		draftDoc.removeFacet("Draft");
		documentManager.saveDocument(draftDoc);

		// return navigationContext.navigateToDocument(draftDoc, "after-edit");
		return "done";
	}

	/**
	 * Calcul du dossier brouillon de l'espace perso
	 */
	private PathRef getDraftFolder() {
		// Obtention du dossier personnel
		UserWorkspaceService uwService = Framework
				.getService(UserWorkspaceService.class);

		DocumentModel userWorkspace = uwService
				.getCurrentUserPersonalWorkspace(documentManager,
						navigationContext.getCurrentDocument());

		// Initialisation Mes brouillons
		PathRef draftFolder = new PathRef(userWorkspace.getPathAsString() + "/"
				+ DRAFTS);
		if (!documentManager.exists(draftFolder)) {
			DocumentModel d = documentManager.createDocumentModel(
					userWorkspace.getPathAsString(), DRAFTS, "Folder");
			d.setPropertyValue("dc:title", "Mes brouillons");

			documentManager.createDocument(d);
		}
		return draftFolder;
	}

	/**
	 * Filtres, est un document brouillon ? 
	 */
	public boolean isDraftDocument() {
		DocumentModel currentDocument = navigationContext.getCurrentDocument();
		return currentDocument.getPropertyValue(TTC_WEBID).toString()
				.startsWith(DRAFT_PREFIX);
	}
};
