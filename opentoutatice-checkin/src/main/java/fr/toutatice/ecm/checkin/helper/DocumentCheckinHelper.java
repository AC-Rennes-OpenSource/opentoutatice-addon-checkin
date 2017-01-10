/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_DOC_ID;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_IN_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_PARENT_ID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;


/**
 * @author david
 *
 */
public class DocumentCheckinHelper {
    
    /** Logger. */
    private static final Log log = LogFactory.getLog(DocumentCheckinHelper.class);
    
    /** Singleton instance. */
    private static DocumentCheckinHelper instance;
    
    /** UserWorkspace service. */
    private static UserWorkspaceService uwService;

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
    
    public static UserWorkspaceService getUserWorkspaceService(){
        if(uwService == null){
            uwService = Framework.getService(UserWorkspaceService.class);
        }
        return uwService;
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
        String checkinedId = DocumentHelper.getCheckinedIdOfDraftDoc(draft);
        DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(session, checkinedId);
        return checkinedDoc != null && checkinedDoc.hasFacet(CHECKINED_IN_FACET);
    }
    
    /**
     * Get Draft document.
     * 
     * @param checkinedDoc
     * @return Draft document
     */
    public DocumentModel getDraftDoc(CoreSession session, DocumentModel checkinedDoc){
        String draftId = DocumentHelper.getDraftIdFromCheckinedDoc(checkinedDoc);
        return WebIdResolver.getLiveDocumentByWebId(session, draftId);
    }
    
    /**
     * Gets draft folder reference (where documents are checkined).
     * Creates Drafts folder it doesn't exist.
     * 
     * @param documentManager
     * @param currentDoc
     * @return reference of draft folder.
     */
    public PathRef getDraftsFolderRef(CoreSession documentManager, DocumentModel currentDoc) {

        DocumentModel userWorkspace = getUserWorkspace(documentManager, currentDoc);

        PathRef draftFolderRef = new PathRef(userWorkspace.getPathAsString().concat("/")
                .concat(CheckinConstants.DRAFTS));
        if (!documentManager.exists(draftFolderRef)) {
            createDraftsFolder(documentManager, userWorkspace);
        } else {
            DocumentModel draftFolder = documentManager.getDocument(draftFolderRef);
            if(!draftFolder.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)){
                draftFolder.addFacet(FacetNames.HIDDEN_IN_NAVIGATION);
                documentManager.saveDocument(draftFolder);
            }
        }
        return draftFolderRef;
    }
    
    /**
     * Gets draft folder reference (where documents are checkined).
     * Creates Drafts folder it doesn't exist.
     * 
     * @param documentManager
     * @param navigationContext
     * @return reference of draft folder.
     */
    public PathRef getDraftsFolderRef(CoreSession documentManager, NavigationContext navigationContext) {
        return getDraftsFolderRef(documentManager, navigationContext.getCurrentDocument());
    }
    
    /**
     * Gets draft path (where documents are checkined).
     * Creates Drafts folder if it doesn't exist.
     * 
     * @param session
     * @param document
     * @return
     */
    public String getDraftsFolderPath(CoreSession session, DocumentModel document){
        PathRef draftFolder = getDraftsFolderRef(session, document);
        return (String) draftFolder.reference();
    }

    /**
     * Creates Drafts folder.
     * 
     * @param documentManager
     * @param userWorkspace
     */
    protected void createDraftsFolder(CoreSession documentManager, DocumentModel userWorkspace) {
        DocumentModel d = documentManager.createDocumentModel(
                userWorkspace.getPathAsString(), CheckinConstants.DRAFTS, "Folder");
        d.setPropertyValue("dc:title", CheckinConstants.DRAFTS_TITLE);
        DocumentModel draftFolder = documentManager.createDocument(d);
        draftFolder.addFacet(FacetNames.HIDDEN_IN_NAVIGATION);
        documentManager.saveDocument(draftFolder);
    }
    
    /**
     * @param session
     * @param currentDocument
     * @return User Workspace
     */
    protected DocumentModel getUserWorkspace(CoreSession session, DocumentModel currentDocument){
        return getUserWorkspaceService().getCurrentUserPersonalWorkspace(session, currentDocument);
    }
    
    /**
     * Restore state of checkined document.
     * 
     * @param session
     * @param checkinedId
     */
    public void restoreCheckinedDoc(CoreSession session, String checkinedId) {

        if (StringUtils.isNotEmpty(checkinedId)) {
            DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(session, checkinedId);

            if (checkinedDoc != null) {
                removeDraftInfosOn(session, checkinedDoc);
            } else {
                if (StringUtils.isNotEmpty(checkinedId)) {
                    log.error("No Draft document with webid: ".concat(checkinedId));
                }
            }
        }
    }

    /**
     * Restore state of checkined document.
     * 
     * @param session
     * @param checkinedDoc
     */
    public void restoreCheckinedDoc(CoreSession session, DocumentModel checkinedDoc) {
        removeDraftInfosOn(session, checkinedDoc);
    }
    
    /**
     * Remove Draft infos on checkined document.
     * 
     * @param session
     * @param checkinedDoc
     */
    public void removeDraftInfosOn(CoreSession session, DocumentModel checkinedDoc) {
        checkinedDoc.removeFacet(CHECKINED_IN_FACET);
        session.removeLock(checkinedDoc.getRef());
        ToutaticeDocumentHelper.saveDocumentSilently(session, checkinedDoc, false);
    }

}
