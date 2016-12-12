/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.DRAFT_FACET;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;


/**
 * @author david
 *
 */
public class DraftListener implements EventListener {

    /**
     * Constructor.
     */
    public DraftListener() {
        super();
    }

    /**
     * Restore state of checkined document when its draft is deleted.
     * Removes Draft when checkined document is put in trash.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

            // Case of Draft removed
            if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
                DocumentModel srcDoc = docCtx.getSourceDocument();
                if (srcDoc.hasFacet(DRAFT_FACET)) {
                    CoreSession session = docCtx.getCoreSession();
                    String checkinedId = DocumentCheckinHelper.getInstance().getCheckinedId(srcDoc);

                    if (StringUtils.isNotEmpty(checkinedId)) {
                        // Draft has a checkined document
                        DocumentCheckinHelper.getInstance().restoreCheckinedDoc(session, checkinedId);
                    }
                }
            }
        }
    }

}
