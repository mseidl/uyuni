/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.frontend.action.user;

import com.redhat.rhn.common.localization.LocalizationService;
import com.redhat.rhn.common.security.PermissionException;
import com.redhat.rhn.common.security.user.StateChangeException;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.struts.RequestContext;
import com.redhat.rhn.frontend.struts.RhnAction;
import com.redhat.rhn.manager.acl.AclManager;
import com.redhat.rhn.manager.user.UserManager;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * DisableUserAction
 * @version $Rev$
 */
public class DisableUserAction extends RhnAction {
    
    /** {@inheritDoc} */
    public ActionForward execute(ActionMapping mapping,
            ActionForm formIn,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        RequestContext requestContext = new RequestContext(request);

        if (!AclManager.hasAcl("user_role(org_admin)",
                request, null)) {
            //Throw an exception with a nice error message so the user
            //knows what went wrong.
            LocalizationService ls = LocalizationService.getInstance();
            PermissionException pex = new PermissionException("Missing Acl");
            pex.setLocalizedTitle(ls.getMessage("permission.jsp.title.disableuser"));
            pex.setLocalizedSummary(ls.getMessage("permission.jsp.summary.disableuser"));
            throw pex;
        }
        Long uid = requestContext.getRequiredParam("uid");
        User loggedInUser = requestContext.getLoggedInUser();
        User user = UserManager.lookupUser(loggedInUser, uid);
        
        try {
            UserManager.disableUser(loggedInUser, user);
            ActionMessages msg = new ActionMessages();
            msg.add(ActionMessages.GLOBAL_MESSAGE, 
                    new ActionMessage("user.disable", user.getLogin()));
            getStrutsDelegate().saveMessages(request, msg);
            if (user.getId().equals(loggedInUser.getId())) {
                return mapping.findForward("logout");
            }
            return mapping.findForward("success");
        }
        catch (StateChangeException e) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                    new ActionMessage(e.getMessage()));
            Map params = new HashMap();
            params.put("uid", uid);
            addErrors(request, errors);
            return getStrutsDelegate().forwardParams(mapping.findForward("failure"), 
                    params);
        }
    }

}
