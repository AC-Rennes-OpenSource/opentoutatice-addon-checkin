/**
 * 
 */
package fr.toutatice.ecm.checkin.portal.infos.provider;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_IN_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.DRAFT_FACET;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.DocumentHelper;
import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;

/**
 * @author Loïc Billon
 *
 */
public class CheckinInfosProvider implements DocumentInformationsProvider {

    /** Logger. */
    private static final Log log = LogFactory.getLog(CheckinInfosProvider.class);

    /**
     * Fetch publications infos :
     * si document d'origine : envoyer en plus le path du brouillon
     * si document brouillon : envoyer en plus le path de contextualisation (= dossier parent du document d'origine).
     */
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws NuxeoException {
        // For Trace logs
        long begin = System.currentTimeMillis();

        Map<String, Object> infos = new HashMap<String, Object>();

        if (currentDocument.hasFacet(CHECKINED_IN_FACET)) {
            Lock lock = currentDocument.getLockInfo();
            if (lock != null) {
                String owner = lock.getOwner();

                if (coreSession.getPrincipal().getName().equals(owner)) {
                    // Check draft existence
                    String draftId = DocumentHelper.getDraftIdFromCheckinedDoc(currentDocument);
                    DocumentModel draft = WebIdResolver.getLiveDocumentByWebId(coreSession, draftId);

                    String draftPath = StringUtils.EMPTY;
                    if (draft != null) {
                        draftPath = draft.getPathAsString();
                    } else {
                        log.error("Document with webId: ".concat(draftId).concat(" does not exist"));
                    }

                    try {
                        infos.put("draftPath", URLEncoder.encode(draftPath, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new NuxeoException(e);
                    }
                }
            } else {
                log.warn("CheckedIn document " + currentDocument.getPathAsString() + " has no more lock");
            }
        }

        if (currentDocument.hasFacet(DRAFT_FACET)) {
            DocumentCheckinHelper checkinHelper = DocumentCheckinHelper.getInstance();

            boolean hasCheckinedDoc = checkinHelper.hasCheckinedDoc(coreSession, currentDocument);
            String checkinedParentId = checkinHelper.getCheckinedParentId(currentDocument);
            String draftContainerPath = StringUtils.EMPTY;
            // Check parent existence
            DocumentModel checkinedParent = WebIdResolver.getLiveDocumentByWebId(coreSession, checkinedParentId);
            if (checkinedParent != null) {
                draftContainerPath = DocumentHelper.getPathFromId(coreSession, checkinedParentId);
            } else {
                log.error("Draft '".concat(currentDocument.getName()).concat("' is orphan"));
            }

            try {
                infos.put("draftContextualizationPath", URLEncoder.encode(draftContainerPath, "UTF-8"));
                infos.put("hasCheckinedDoc", URLEncoder.encode(String.valueOf(hasCheckinedDoc), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error(StringUtils.EMPTY, e);
            }

        }

        if (log.isTraceEnabled()) {
            long end = System.currentTimeMillis();
            log.trace(": " + String.valueOf(end - begin) + " ms");
        }

        return infos;
    }


}
