package com.sohlman.liferay.dl.cdn;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.theme.PortletDisplay;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.documentlibrary.service.permission.DLFileEntryPermission;
import com.liferay.portlet.documentlibrary.util.DLImpl;
import com.liferay.portlet.documentlibrary.util.ImageProcessorUtil;
import com.liferay.portlet.trash.util.TrashUtil;

import java.util.Date;

public class CdnDLImpl extends DLImpl {

	@Override
	public String getPreviewURL(
		FileEntry fileEntry, FileVersion fileVersion, ThemeDisplay themeDisplay,
		String queryString, boolean appendVersion, boolean absoluteURL) {

		StringBundler sb = new StringBundler(17);

		if (themeDisplay != null) {
			if (absoluteURL) {	
				// DL CDN Customization START
				PermissionChecker permissionChecker;
				
				String cdnHostHttp = PortalUtil.getCDNHostHttp(
					fileEntry.getCompanyId());

				String cdnHostHttps = PortalUtil.getCDNHostHttps(
					fileEntry.getCompanyId());
				
				try {
					if (Validator.isNotNull(cdnHostHttps) 
							&& themeDisplay.isSecure()) {
 
						permissionChecker = 
							PermissionCheckerFactoryUtil.create(
								themeDisplay.getDefaultUser());

						if (DLFileEntryPermission.contains(
								permissionChecker, fileEntry,
								ActionKeys.VIEW)) {

							sb.append(cdnHostHttps);
						}
						else {
							sb.append(themeDisplay.getPortalURL());
						}
					}
					else if (Validator.isNotNull(cdnHostHttp) && 
							!themeDisplay.isSecure()) {

						permissionChecker = 
							PermissionCheckerFactoryUtil.create(
									themeDisplay.getDefaultUser());
						
						if (DLFileEntryPermission.contains(
								permissionChecker, fileEntry, 
								ActionKeys.VIEW)) {

							sb.append(cdnHostHttp);
						}
						else {
							sb.append(themeDisplay.getPortalURL());
						}
					}
					else {
						sb.append(themeDisplay.getPortalURL());
					}
				} 
				catch (Exception e) {
					_log.warn(e);
					sb.append(themeDisplay.getPortalURL());
				}
				if (_log.isDebugEnabled()) {
					_log.debug("Preview URL : " + sb.toString());
				}
				// DL CDN Customization END
				// 
				// Original implementation
				// sb.append(themeDisplay.getPortalURL());
			}	
		}

		sb.append(PortalUtil.getPathContext());
		sb.append("/documents/");
		sb.append(fileEntry.getRepositoryId());
		sb.append(StringPool.SLASH);
		sb.append(fileEntry.getFolderId());
		sb.append(StringPool.SLASH);

		String title = fileEntry.getTitle();

		if (fileEntry.isInTrash()) {
			title = TrashUtil.getOriginalTitle(fileEntry.getTitle());
		}

		sb.append(HttpUtil.encodeURL(HtmlUtil.unescape(title)));

		sb.append(StringPool.SLASH);
		sb.append(HttpUtil.encodeURL(fileEntry.getUuid()));

		if (appendVersion) {
			sb.append("?version=");
			sb.append(fileVersion.getVersion());
		}

		if (ImageProcessorUtil.isImageSupported(fileVersion)) {
			if (appendVersion) {
				sb.append("&t=");
			}
			else {
				sb.append("?t=");
			}

			Date modifiedDate = fileVersion.getModifiedDate();

			sb.append(modifiedDate.getTime());
		}

		sb.append(queryString);

		if (themeDisplay != null) {
			PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

			if (portletDisplay != null) {
				String portletId = portletDisplay.getId();

				if (portletId.equals(PortletKeys.TRASH)) {
					sb.append("&status=");
					sb.append(WorkflowConstants.STATUS_IN_TRASH);
				}
			}
		}

		String previewURL = sb.toString();

		if ((themeDisplay != null) && themeDisplay.isAddSessionIdToURL()) {
			return PortalUtil.getURLWithSessionId(
				previewURL, themeDisplay.getSessionId());
		}

		return previewURL;
	}
	
	private static Log _log = LogFactoryUtil.getLog(CdnDLImpl.class);
}
