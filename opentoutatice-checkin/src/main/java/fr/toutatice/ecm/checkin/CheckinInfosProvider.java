/**
 * 
 */
package fr.toutatice.ecm.checkin;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;

/**
 * @author Loïc Billon
 *
 */
public class CheckinInfosProvider implements DocumentInformationsProvider {


	/**
	 * 
	 */
	private static final String DRAFT_PREFIX = "draft_";
	
	/**
	 * 
	 */
	private static final String TTC_WEBID = "ttc:webid";
	
	/**
	 * Fetch publications infos :
	 * si document d'origine : envoyer en plus le path du brouillon
	 * si document brouillon : envoyer en plus le path de contextualisation (= dossier parent du document d'origine).
	 */
	@Override
	public Map<String, Object> fetchInfos(CoreSession coreSession,
			DocumentModel currentDocument) throws ClientException {
		
		Map<String, Object> infos = new HashMap<String, Object>();
		if(currentDocument.hasFacet("CheckedIn")) {
			String owner = currentDocument.getLockInfo().getOwner();
			if(coreSession.getPrincipal().getName().equals(owner)) {
				
				String draftId = DRAFT_PREFIX + currentDocument.getPropertyValue(TTC_WEBID);
				
				DocumentModel draft = WebIdResolver.getLiveDocumentByWebId(coreSession, draftId);
				
				try {
					infos.put("draftPath", URLEncoder.encode(draft.getPathAsString().toString(), "UTF-8") );
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(currentDocument.hasFacet("Draft")) {

			String draftId = currentDocument.getPropertyValue(TTC_WEBID).toString();
			String webId = draftId.substring(DRAFT_PREFIX.length());
			
			DocumentModel original = WebIdResolver.getLiveDocumentByWebId(coreSession, webId);
			DocumentModel parent = coreSession.getDocument(original.getParentRef());
			
			try {
				infos.put("draftContentPath", URLEncoder.encode(parent.getPathAsString().toString(), "UTF-8") );
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		return infos;
	}

	
	
}
