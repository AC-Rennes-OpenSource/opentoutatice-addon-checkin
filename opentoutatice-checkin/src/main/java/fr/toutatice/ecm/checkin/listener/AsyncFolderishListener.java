/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.helper.DocumentHelper;
import fr.toutatice.ecm.checkin.helper.DraftsQueryHelper;
import fr.toutatice.ecm.checkin.helper.TransitionHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author david
 *
 */
public class AsyncFolderishListener implements PostCommitFilteringEventListener {

    /** Logger. */
    public static Log log = LogFactory.getLog(AsyncFolderishListener.class);

    /** Trash service. */
    private static TrashService trashService;

    /** Getter for trash service. */
    protected TrashService getTrashService() {
        if (trashService == null) {
            trashService = Framework.getService(TrashService.class);
        }
        return trashService;
    }

    /** Delete transition indicator. */
    private boolean isDeletion = false;
    /** Undelete transition indicator. */
    private boolean isUndeletion = false;
    /** Removed indocator. */
    private boolean isRemoved = false;

    /**
     * Accepts only deleted transition event.
     */
    @Override
    public boolean acceptEvent(Event event) {

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

            if (DocumentEventTypes.DOCUMENT_REMOVED.equals(event.getName())) {
                this.isRemoved = true;
                return this.isRemoved;
            }

            this.isDeletion = TransitionHelper.isTransition(docCtx, event, LifeCycleConstants.DELETE_TRANSITION);
            this.isUndeletion = TransitionHelper.isTransition(docCtx, event, LifeCycleConstants.UNDELETE_TRANSITION);

            return this.isDeletion || this.isUndeletion;
        }
        return false;
    }

    /**
     * Drafts orphan removed when Folderish referenced by them (ottcDft:checkoutParentId)
     * is trashed.
     */
    @Override
    public void handleEvent(EventBundle events) throws NuxeoException {
        for (Event event : events) {

            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            DocumentModel srcDoc = docCtx.getSourceDocument();
            CoreSession session = docCtx.getCoreSession();

            if (srcDoc != null && srcDoc.isFolder()) {
                if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
                    deleteOrRestoreOrphansDrafts(session, srcDoc);
                } else if (this.isRemoved) {
                    removeDrafts(docCtx, session, srcDoc);
                }
            }
        }
    }

    /**
     * Delete or restore Drafts of folder.
     * 
     * @param session
     * @param folder
     */
    protected void deleteOrRestoreOrphansDrafts(CoreSession session, DocumentModel folder) {
        String parentId = DocumentHelper.getId(folder);
        DocumentModelList orphanDrafts = session.query(String.format(DraftsQueryHelper.ORPHAN_DRAFTS_QUERY_OF, parentId));
        if (orphanDrafts.size() > 0) {
            for (DocumentModel orphan : orphanDrafts) {
                session.followTransition(orphan, TransitionHelper.getTransitionName(this.isDeletion));
            }
            session.save();
        }
    }

    /**
     * Remove Drafts of Folder.
     * 
     * @param session
     * @param folder
     */
    protected void removeDrafts(DocumentEventContext docCtx, CoreSession session, DocumentModel folder) {
        // Property get from FolderishInfosListener
        String parentIdList = (String) docCtx.getProperty(DraftsQueryHelper.PARENT_CHECKOUT_IDS_LIST);
        if (StringUtils.isNotBlank(parentIdList)) {
            DocumentModelList drafts = session.query(String.format(DraftsQueryHelper.DRAFTS_QUERY_OF, parentIdList));
            if (drafts.size() > 0) {
                for (DocumentModel draft : drafts) {
                    ToutaticeDocumentHelper.removeDocumentSilently(session, draft, false);
                }
                session.save();
            }
        }

    }

}
