/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_DOC_ID;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_IN_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_PARENT_ID;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;


/**
 * @author david
 *
 */
public class DocumentCheckinHelper {
    
    /** Singleton instance. */
    private static DocumentCheckinHelper instance;

    /**
     * Singleton.
     */
    private DocumentCheckinHelper() {
        super();
    }
    
    /**
     * @return singleton instance.
     */
    public static synchronized DocumentCheckinHelper getInstance(){
        if(instance == null){
            instance = new DocumentCheckinHelper();
        }
        return instance;
    }
    
    /**
     * @param document
     * @return true if document is a draft.
     */
    public boolean isDraft(DocumentModel document){
        return document.hasFacet(CheckinConstants.DRAFT_FACET);
    }
    
    /**
     * @param document
     * @return true if document has a draft.
     */
    public boolean hasDraft(DocumentModel document){
        return document.hasFacet(CheckinConstants.CHECKINED_IN_FACET);
    }
    
    /**
     * @param draft
     * @return checkout parent webId.
     */
    public String getCheckinedParentId(DocumentModel draft){
        if(draft != null){
            return (String) draft.getPropertyValue(CheckinConstants.CHECKINED_PARENT_ID);
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Sets checkouted parent webId of draft.
     * 
     * @param draft
     * @param checkinedParent
     */
    public void setCheckinedParentId(DocumentModel draft, DocumentModel checkinedParent){
        if(draft != null && checkinedParent != null){
            String checkinedParentId = DocumentHelper.getId(checkinedParent);
            draft.setPropertyValue(CHECKINED_PARENT_ID, checkinedParentId);
        }
    }
    
    /**
     * @param draft
     * @return chekinedId property of draft.
     */
    public String getCheckinedId(DocumentModel draft){
        if(draft != null){
            String checkinedId = (String) draft.getPropertyValue(CheckinConstants.CHECKINED_DOC_ID);
            return StringUtils.isNotBlank(checkinedId) ? checkinedId : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Store checkined document webId in draft.
     * 
     * @param draft
     * @param checkinedDoc
     */
    public void setCheckinedDocId(DocumentModel draft, DocumentModel checkinedDoc){
        // FIXME: test OttcDraft schema and adds it if not there?
        if(draft != null && checkinedDoc != null){
            String checkinedDocId = DocumentHelper.getId(checkinedDoc);
            draft.setPropertyValue(CHECKINED_DOC_ID, checkinedDocId);
        }
    }
    
    
    /**
     * @param session
     * @param draft
     * @return true if draft has a checkined document.
     */
    public boolean hasCheckinedDoc(CoreSession session, DocumentModel draft){
        String checkinedId = DocumentHelper.getIdFromDraftId(draft);
        DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(session, checkinedId);
        // FIXME: case of doc created and checkined after draft orphelin
        // draft: webid = draft_y and new checkined: webid = y ...
        // Check on webid generation?
        return checkinedDoc != null && checkinedDoc.hasFacet(CHECKINED_IN_FACET);
    }
    
    /**
     * Gets draft folder reference (where documents are checkined).
     * Creates if it doesn't exist.
     * 
     * @param documentManager
     * @param navigationContext
     * @return reference of draft folder.
     */
    public PathRef getDraftFolderRef(CoreSession documentManager, NavigationContext navigationContext) {
        // Obtention du dossier personnel
        UserWorkspaceService uwService = Framework
                .getService(UserWorkspaceService.class);

        DocumentModel userWorkspace = uwService
                .getCurrentUserPersonalWorkspace(documentManager,
                        navigationContext.getCurrentDocument());

        // Initialisation Mes brouillons
        PathRef draftFolder = new PathRef(userWorkspace.getPathAsString() + "/"
                + CheckinConstants.DRAFTS);
        if (!documentManager.exists(draftFolder)) {
            DocumentModel d = documentManager.createDocumentModel(
                    userWorkspace.getPathAsString(), CheckinConstants.DRAFTS, "Folder");
            d.setPropertyValue("dc:title", CheckinConstants.DRAFTS_TITLE);

            documentManager.createDocument(d);
        }
        return draftFolder;
    }

}
