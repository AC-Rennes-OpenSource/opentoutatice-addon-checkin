/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;


/**
 * @author david
 *
 */
public class AsyncFolderishListener implements PostCommitFilteringEventListener {

    /** Logger. */
    public static Log log = LogFactory.getLog(AsyncFolderishListener.class);

    /** Checlined documents query. */
    protected static String ORPHAN_DRAFTS_QUERY_OF = "select * from Document where ecm:mixinType = \"OttcDraft\" "
            + "and ottcDft:checkinedDocId = \"\" and ottcDft:checkoutParentId = \"%s\" and ecm:isProxy = 0 and ecm:isVersion = 0";

    /** Trash service. */
    private static TrashService trashService;

    /** Getter for trash service. */
    protected TrashService getTrashService() {
        if (trashService == null) {
            trashService = Framework.getService(TrashService.class);
        }
        return trashService;
    }
    
    /**
     * Accepts only deleted transition event.
     */
    @Override
    public boolean acceptEvent(Event event) {
        if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
            if (event.getContext() instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            String transition = (String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
            return LifeCycleConstants.DELETE_TRANSITION.equals(transition);
            }   
        }
        return false;
    }

    /**
     * Drafts orphan removed when Folderish referenced by them (ottcDft:checkoutParentId)
     * is trashed.
     */
    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        for (Event event : events) {

                DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
                DocumentModel sourceDocument = docCtx.getSourceDocument();
                CoreSession session = docCtx.getCoreSession();

                if (sourceDocument != null && sourceDocument.isFolder()) {
                    String parentId = (String) sourceDocument.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
                    
                    DocumentModelList orphanDrafts = session.query(String.format(ORPHAN_DRAFTS_QUERY_OF, parentId));
                    if(orphanDrafts.size() > 0){
                        List<DocumentRef> orphanDraftsRefs = new ArrayList<DocumentRef>(orphanDrafts.size());
                        for(DocumentModel orphan : orphanDrafts){
                            orphanDraftsRefs.add(orphan.getRef());
                        }
                        session.removeDocuments(orphanDraftsRefs.toArray(new DocumentRef[orphanDraftsRefs.size()]));
                        session.save();
                    }
                    
                    
                    
                    
                    
                    
                    
                    
                    

//                    boolean removed = DocumentEventTypes.DOCUMENT_REMOVED.equals(event.getName());
//                    boolean trashed = isTrashed(docCtx, event);
//
//                    if (removed || trashed) {
//
//                        // Get checkined documents
//                        DocumentModelList checkinedDocs = session.query(String.format(ORPHAN_DRAFTS_QUERY, sourceDocument.getId()));
//
//                        List<DocumentModel> draftsToTrashed = new ArrayList<DocumentModel>(0);
//                        List<DocumentRef> draftsToRemove = new ArrayList<DocumentRef>(0);
//
//                        // Get Drafts and
//                        if (checkinedDocs != null) {
//                            for (DocumentModel checkinedDoc : checkinedDocs) {
//                                DocumentModel draft = checkinHelper.getDraftDoc(session, checkinedDoc);
//
//                                if (draft != null) {
//                                    if (trashed) {
//                                        draftsToTrashed.add(draft);
//                                    } else if (removed) {
//                                        draftsToRemove.add(draft.getRef());
//                                    }
//                                } else {
//                                    log.warn("Checkined document: " + checkinedDoc.getPathAsString() + "is an incoherent state "
//                                            + "(corresponding Draft is null)");
//                                }
//                            }
//
//                            session.removeDocuments(draftsToRemove.toArray(new DocumentRef[draftsToRemove.size()]));
//                        }
//                    }
                }
            }
    }

    /**
     * Checks if event maps with the put in trash action.
     * 
     * @param docCtx
     * @param event
     * @return true if event is trashed event
     */
    protected boolean isTrashed(DocumentEventContext docCtx, Event event) {
        if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
            Map<String, Serializable> properties = docCtx.getProperties();
            if (properties != null) {
                String transition = (String) properties.get(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
                return LifeCycleConstants.DELETE_TRANSITION.equals(transition);
            }
        }
        return false;
    }

}
