
package fr.toutatice.ecm.checkin.bean;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_IN_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.DRAFT_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.WEBID_DISABLED_FACET;
import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActionsBean;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.DocumentHelper;
import fr.toutatice.ecm.platform.core.constants.PortalConstants;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;
import fr.toutatice.ecm.platform.web.document.ToutaticeDocumentActionsBean;
import fr.toutatice.ecm.platform.web.fragments.PageBean;

/**
 * @author Loïc Billon
 *
 */
@Name("checkinActions")
@Scope(CONVERSATION)
public class CheckinActions implements Serializable {

	private static final long serialVersionUID = -5092427864129643896L;

	@In(create = true)
	protected transient NavigationContext navigationContext;

	@In(create = true, required = false)
	protected transient CoreSession documentManager;

	@In(create = true)
	PathSegmentService pathSegmentService;
	
	@In(create = true)
	protected transient DocumentActionsBean documentActions;
	
	@In(create = true)
    protected PageBean pageBean;
	
	/** Checkin helper. */
	private static DocumentCheckinHelper checkinHelper = DocumentCheckinHelper.getInstance();

    /**
     * Create and checkin a document
     * (in fact, just create document in user's workspace).
     * 
     * @return portal view outcome.
     */
    public String createNCheckin(){
        // UC: Method called from toutatice_create PortalView in Portal 'public' Workspace context.
        
        DocumentModel draftDocBean = navigationContext.getChangeableDocument();
        DocumentModel checkoutParentDoc = navigationContext.getCurrentDocument();
        
        // Prepare bean to save data in Draft Folder
        String docName = pathSegmentService.generatePathSegment(draftDocBean);
        String draftsFolderPath = checkinHelper.getDraftsFolderPath(documentManager, checkoutParentDoc);
        
        draftDocBean.setPathInfo(draftsFolderPath, docName);
        draftDocBean.addFacet(DRAFT_FACET);
        draftDocBean.addFacet(WEBID_DISABLED_FACET);
        
        // Draft is created with no prefixed webId
        // (as the logical document exists only in one state)
        DocumentModel draftDoc = documentManager.createDocument(draftDocBean);
        
        // Store draft infos
        checkinHelper.setCheckinedParentId(draftDoc, checkoutParentDoc);
        
        // Save and invalidations of caches
        documentManager.saveDocument(draftDoc);
        documentManager.save();
        
        // For Portal notification
        pageBean.setNotificationKey(CheckinConstants.SUCCESS_MESSAGE_DRAFT_CREATED);
        ((ToutaticeDocumentActionsBean) documentActions).setLive(true);
        
        // To refresh draft document (cf osivia_done.xhtml)
        navigationContext.setCurrentDocument(draftDoc);
        
        return PortalConstants.FINAL_PORTAL_VIEW;
    }

    /**
     * Checkin existing document.
     * 
     * @return portal view id.
     */
    public String checkin() {
        // UC: Method called from toutatice_edit PortalView in Portal 'public' Workspace context.

        DocumentModel checkinableDocBean = navigationContext.getCurrentDocument();
        DocumentModel checkinableDoc = documentManager.getDocument(checkinableDocBean.getRef());

        // Copy to keep lifecycle, versions (?), ...
        String draftsFolderId = checkinHelper.getDraftsFolderId(documentManager, checkinableDocBean);
        DocumentModel draftDoc = documentManager.copy(checkinableDoc.getRef(), new IdRef(draftsFolderId), null); // note that webId has changed (createdByCopy event)
        
        // To be able to set draft schema property
        // FIXME: find other way than save?
        draftDoc.addFacet(DRAFT_FACET);
        draftDoc.addFacet(WEBID_DISABLED_FACET);
        
        // Explicit checkin so:
        DocumentModel checkinedParent = documentManager.getParentDocument(checkinableDoc.getRef());
        checkinHelper.setCheckinedParentId(draftDoc, checkinedParent);
        checkinHelper.setCheckinedDocId(draftDoc, checkinableDoc);
        // To avoid versions
        ToutaticeDocumentHelper.saveDocumentSilently(documentManager, draftDoc, false);

        // To fill draft document with checkinableDocBean data
        checkinableDocBean.setPathInfo(DocumentHelper.getParentPath(draftDoc), draftDoc.getName());
        // To be able to write all properties, not the current modified ones only
        //checkinableDocBean = DocumentHelper.setDirty(checkinableDocBean); Not needed??

        draftDoc = documentManager.saveDocument(checkinableDocBean); 
        // Doc is checkined now
        DocumentModel checkinedDoc = checkinableDoc;

        // Lock to avoid modification possibility of checkined document by others users
        checkinedDoc.addFacet(CHECKINED_IN_FACET);
        checkinedDoc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        
        Lock lockInfo = documentManager.getLockInfo(checkinedDoc.getRef());
        if(lockInfo != null){
            documentManager.removeLock(checkinedDoc.getRef());
        }
        documentManager.setLock(checkinedDoc.getRef());
        
        // Silently To avoid modification of webId
        ToutaticeDocumentHelper.saveDocumentSilently(documentManager, checkinedDoc, false);
        // Facet must be saved before setting prop of facet's schema
        // FIXME: use only webId
        checkinedDoc.setPropertyValue(CheckinConstants.DRAFT_PATH, draftDoc.getPathAsString());
        checkinedDoc.setPropertyValue(CheckinConstants.DRAFT_ID, DocumentHelper.getId(draftDoc));
        ToutaticeDocumentHelper.saveDocumentSilently(documentManager, checkinedDoc, false);
        
        // To refresh go to draft document (cf osivia_done.xhtml)
        navigationContext.setCurrentDocument(draftDoc);
        
        // Caches invalidation 
        documentManager.save();

        return PortalConstants.FINAL_PORTAL_VIEW;
    }

	/**
	 * Checkout draft document (corresponding checkined document exists or not).
	 * 
	 * @return "done" Portal View id.
	 */
	public String checkout() {
	    DocumentModel draftBean = navigationContext.getCurrentDocument();
	    DocumentModel checkoutedDoc = null;
	    
	    if(!checkinHelper.hasCheckinedDoc(documentManager, draftBean)){
	        // Save and move:
	        // checkoutParentPath is stored before remove of OttcDraft facet 
	        // as it contains ottcDraft schema
	        String checkoutParentId = checkinHelper.getCheckinedParentId(draftBean);
	        draftBean.removeFacet(DRAFT_FACET);
	        draftBean.removeFacet(WEBID_DISABLED_FACET);
	        draftBean.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
	        DocumentModel draftDoc = documentManager.saveDocument(draftBean);
	        
	        // WebId is used as name to ensure name unicity (and so move possible)
	        // (Draft has been created with no prefixed webId
	        // (as the logical document exists only in one state)
	        String checkoutParentPath = DocumentHelper.getPathFromId(documentManager, checkoutParentId);
	        checkoutedDoc = documentManager.move(draftDoc.getRef(), new PathRef(checkoutParentPath), DocumentHelper.getCheckinedIdOfDraftDoc(draftDoc));
	        
	    } else {
	        // Remove Draft (path to restore) before save to avoid webid modification
            // formDraftBean.setPathInfo(DocumentHelper.getPath(documentManager, getDraftFolderRef()), formDraftBean.getName());
            documentManager.removeDocument(draftBean.getRef());
	        
	        String checkinedDocId = DocumentHelper.getCheckinedIdOfDraftDoc(draftBean);
    		DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(
    				documentManager, checkinedDocId);
    		
    		draftBean.setPathInfo(DocumentHelper.getParentPath(checkinedDoc), checkinedDoc.getName());
    		draftBean.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
    		
    		// Editorial save
    		draftBean.removeFacet(CheckinConstants.DRAFT_FACET);
    		// To be able to write all properties, not the current modified ones only
    		draftBean = DocumentHelper.setDirty(draftBean);
    		checkoutedDoc = documentManager.saveDocument(draftBean);
    		
    		// Deletion of Checkin state is done by DeleteDraftListener
	    }
	    // To refresh checkouted ('public') document (cf osivia_done.xhtml)
        navigationContext.setCurrentDocument(checkoutedDoc);
	    
	    // Caches invalidation 
	    documentManager.save();

		return PortalConstants.FINAL_PORTAL_VIEW;

	}
	
	/**
	 * @return true if current document is a draft.
	 */
	public boolean isDraft(){
	    DocumentModel currentDocument = navigationContext.getCurrentDocument();
	    return currentDocument.hasFacet(DRAFT_FACET);
	}

}
