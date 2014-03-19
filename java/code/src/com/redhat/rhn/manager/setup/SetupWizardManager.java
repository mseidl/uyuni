/**
 * Copyright (c) 2014 SUSE
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

package com.redhat.rhn.manager.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.redhat.rhn.common.conf.Config;
import com.redhat.rhn.common.validator.ValidatorError;
import com.redhat.rhn.domain.channel.ChannelFamilyFactory;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.manager.BaseManager;
import com.redhat.rhn.manager.satellite.ConfigureSatelliteCommand;
import com.suse.manager.model.ncc.ListSubscriptions;
import com.suse.manager.model.ncc.Subscription;
import com.suse.manager.model.ncc.SubscriptionList;

public class SetupWizardManager extends BaseManager {

    // Logger for this class
    private static Logger logger = Logger.getLogger(SetupWizardManager.class);

    // Config keys
    public final static String KEY_MIRRCREDS_USER = "server.susemanager.mirrcred_user";
    public final static String KEY_MIRRCREDS_PASS = "server.susemanager.mirrcred_pass";
    public final static String KEY_MIRRCREDS_EMAIL = "server.susemanager.mirrcred_email";

    // NCC URL for listing subscriptions
    private final static String NCC_URL = "https://secure-www.novell.com/center/regsvc/?command=listsubscriptions";

    // Session attribute keys
    private final static String SUBSCRIPTIONS_KEY = "SETUP_WIZARD_SUBSCRIPTIONS";

    // Maximum number of redirects that will be followed
    private final static int MAX_REDIRECTS = 10;

    /**
     * Find all valid mirror credentials and return them.
     * @return List of all available mirror credentials
     */
    public static List<MirrorCredentials> findMirrorCredentials() {
        List<MirrorCredentials> credsList = new ArrayList<MirrorCredentials>();

        // Get the main pair of credentials
        String user = Config.get().getString(KEY_MIRRCREDS_USER);
        String password = Config.get().getString(KEY_MIRRCREDS_PASS);
        String email = Config.get().getString(KEY_MIRRCREDS_EMAIL);

        // Add credentials as long as they have user and password
        MirrorCredentials creds;
        int id = 0;
        while (user != null && password != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Found credentials (" + id + "): " + user);
            }

            // Create credentials object
            creds = new MirrorCredentials(email, user, password);
            creds.setId(new Long(id));
            credsList.add(creds);

            // Search additional credentials with continuous enumeration
            id++;
            user = Config.get().getString(KEY_MIRRCREDS_USER + "." + id);
            password = Config.get().getString(KEY_MIRRCREDS_PASS + "." + id);
            email = Config.get().getString(KEY_MIRRCREDS_EMAIL + "." + id);
        }
        return credsList;
    }

    /**
     * Find mirror credentials for a given ID.
     * @return pair of credentials for given ID.
     */
    public static MirrorCredentials findMirrorCredentials(long id) {
        // Generate suffix depending on the ID
        String suffix = "";
        if (id > 0) {
            suffix = "." + id;
        }

        // Get the credentials from config
        String user = Config.get().getString(KEY_MIRRCREDS_USER + suffix);
        String password = Config.get().getString(KEY_MIRRCREDS_PASS + suffix);
        String email = Config.get().getString(KEY_MIRRCREDS_EMAIL + suffix);
        MirrorCredentials creds = null;
        if (user != null && password != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found credentials for ID: " + id);
            }
            // Create credentials object
            creds = new MirrorCredentials(email, user, password);
            creds.setId(id);
        }
        return creds;
    }

    /**
     * Store a given pair of credentials in the filesystem after editing or using the next
     * available free index for new credentials.
     * @param creds mirror credentials to store
     * @param user the current user
     * @return list of validation errors or null in case of success
     */
    public static ValidatorError[] storeMirrorCredentials(MirrorCredentials creds,
            User userIn, HttpServletRequest request) {
        if (creds.getUser() == null || creds.getPassword() == null) {
            return null;
        }

        // Find the first free ID if necessary
        Long id = creds.getId();
        if (creds.getId() == null) {
            List<MirrorCredentials> credentials = SetupWizardManager.findMirrorCredentials();
            id = new Long(credentials.size());
        }

        // Check if there is changes by looking at previous object
        MirrorCredentials oldCreds = SetupWizardManager.findMirrorCredentials(id);
        if (!creds.equals(oldCreds)) {
            // Generate suffix depending on the ID
            String suffix = "";
            if (id > 0) {
                suffix = "." + id;
            }
            ConfigureSatelliteCommand configCommand = new ConfigureSatelliteCommand(userIn);
            configCommand.updateString(KEY_MIRRCREDS_USER + suffix, creds.getUser());
            configCommand.updateString(KEY_MIRRCREDS_PASS + suffix, creds.getPassword());
            if (creds.getEmail() != null) {
                configCommand.updateString(KEY_MIRRCREDS_EMAIL + suffix, creds.getEmail());
            }
            // Remove old credentials data from cache
            if (oldCreds != null) {
                removeSubsFromSession(oldCreds, request);
            }
            return configCommand.storeConfiguration();
        }
        else {
            // Nothing to do
            return null;
        }
    }

    /**
     * Delete a pair of credentials given by their ID. Includes some sophisticated logic
     * to shift IDs in case you delete a pair of credentials from the middle.
     * @param id the id of credentials being deleted
     * @param userIn the user currently logged in
     * @return list of validation errors or null in case of success
     */
    public static ValidatorError[] deleteMirrorCredentials(Long id, User userIn,
            HttpServletRequest request) {
        ValidatorError[] errors = null;

        // Store credentials to empty cache later
        MirrorCredentials credentials = SetupWizardManager.findMirrorCredentials(id);

        // Find all credentials and see what needs to be done
        List<MirrorCredentials> creds = SetupWizardManager.findMirrorCredentials();

        // Special case: delete the last pair of credentials
        if (creds.size() == id + 1) {
            MirrorCredentials delCreds = new MirrorCredentials("", "", "");
            delCreds.setId(id);
            errors = SetupWizardManager.storeMirrorCredentials(delCreds, userIn, request);
        }
        else if (creds.size() > id + 1) {
            // We need to shift indices
            ConfigureSatelliteCommand configCommand = new ConfigureSatelliteCommand(userIn);
            for (MirrorCredentials c : creds) {
                int index = creds.indexOf(c);
                if (index > id) {
                    String targetSuffix = "";
                    if (index > 1) {
                        targetSuffix = "." + (index - 1);
                    }
                    configCommand.updateString(KEY_MIRRCREDS_USER + targetSuffix, c.getUser());
                    configCommand.updateString(KEY_MIRRCREDS_PASS + targetSuffix, c.getPassword());
                    if (c.getEmail() != null) {
                        configCommand.updateString(KEY_MIRRCREDS_EMAIL + targetSuffix, c.getEmail());
                    }
                    // Empty the last pair of credentials
                    if (index == creds.size() - 1) {
                        targetSuffix = "." + index;
                        configCommand.updateString(KEY_MIRRCREDS_USER + targetSuffix, "");
                        configCommand.updateString(KEY_MIRRCREDS_PASS + targetSuffix, "");
                        configCommand.updateString(KEY_MIRRCREDS_EMAIL + targetSuffix, "");
                    }
                }
            }
            errors = configCommand.storeConfiguration();

            // Clean deleted credentials data from cache
            removeSubsFromSession(credentials, request);
        }
        return errors;
    }

    /**
     * Make primary credentials for a given credentials ID.
     * Cache is not affected by reordering, because username is used as the key.
     * @return list of validation errors or null in case of success
     */
    public static ValidatorError[] makePrimaryCredentials(Long id, User userIn,
            HttpServletRequest request) {
        ValidatorError[] errors = null;
        List<MirrorCredentials> allCreds = SetupWizardManager.findMirrorCredentials();
        if (allCreds.size() > 1) {
            // Find the future primary creds before reordering
            MirrorCredentials primaryCreds = SetupWizardManager.findMirrorCredentials(id);
            ConfigureSatelliteCommand configCommand = new ConfigureSatelliteCommand(userIn);

            // Shift all indices starting from 1
            int i = 1;
            for (MirrorCredentials c : allCreds) {
                if (allCreds.indexOf(c) != id) {
                    String targetSuffix = "." + i;
                    configCommand.updateString(KEY_MIRRCREDS_USER + targetSuffix, c.getUser());
                    configCommand.updateString(KEY_MIRRCREDS_PASS + targetSuffix, c.getPassword());
                    if (c.getEmail() != null) {
                        configCommand.updateString(KEY_MIRRCREDS_EMAIL + targetSuffix, c.getEmail());
                    }
                    i++;
                }
            }

            // Set the primary credentials and store
            primaryCreds.setId(0L);
            configCommand.updateString(KEY_MIRRCREDS_USER, primaryCreds.getUser());
            configCommand.updateString(KEY_MIRRCREDS_PASS, primaryCreds.getPassword());
            if (primaryCreds.getEmail() != null) {
                configCommand.updateString(KEY_MIRRCREDS_EMAIL, primaryCreds.getEmail());
            }
            errors = configCommand.storeConfiguration();
        }
        return errors;
    }

    /**
     * Connect to NCC and return subscriptions for a given pair of credentials.
     * @param creds the mirror credentials to use
     * @return list of subscriptions available via the given credentials
     */
    public static List<Subscription> downloadSubscriptions(MirrorCredentials creds) {
        // Setup XML to send it with the request
        ListSubscriptions listsubs = new ListSubscriptions();
        listsubs.setUser(creds.getUser());
        listsubs.setPassword(creds.getPassword());
        PostMethod post = new PostMethod(NCC_URL);
        List<Subscription> subscriptions = null;
        try {
            // Serialize into XML
            Serializer serializer = new Persister();
            StringWriter xmlString = new StringWriter();
            serializer.write(listsubs, xmlString);
            RequestEntity entity = new StringRequestEntity(
                    xmlString.toString(), "text/xml", "UTF-8");

            // Manually follow redirects as long as we get 302
            HttpClient httpclient = new HttpClient();
            int result = 0;
            int redirects = 0;
            do {
                if (result == 302) {
                    // Prepare the redirect
                    Header locationHeader = post.getResponseHeader("Location");
                    String location = locationHeader.getValue();
                    logger.info("Got 302, following redirect to: " + location);
                    post = new PostMethod(location);
                    redirects++;
                }

                // Execute the request
                post.setRequestEntity(entity);
                result = httpclient.executeMethod(post);
                if (logger.isDebugEnabled()) {
                    logger.debug("Response status code: " + result);
                }
            } while (result == 302 && redirects < MAX_REDIRECTS);

            // Parse the response body in case of success
            if (result == 200) {
                InputStream stream = post.getResponseBodyAsStream();
                SubscriptionList subsList = serializer.read(SubscriptionList.class, stream);
                subscriptions = subsList.getSubscriptions();
                logger.info("Found " + subscriptions.size() + " subscriptions");
            }
        } catch (HttpException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            logger.debug("Releasing connection");
            post.releaseConnection();
        }
        return subscriptions;
    }

    /**
     * Make DTOs from a given list of {@link Subscription} objects read from NCC. While doing
     * that, filter out only active subscriptions and get human readable names from DB.
     * @param subscriptions
     * @return list of subscription DTOs
     */
    private static List<SubscriptionDto> makeDtos(List<Subscription> subscriptions) {
        if (subscriptions == null) {
            return null;
        }
        // Go through all of the given subscriptions
        List<SubscriptionDto> dtos = new ArrayList<SubscriptionDto>();
        for (Subscription sub : subscriptions) {
            if (sub.getSubstatus().equals("EXPIRED")) {
                continue;
            }

            // Determine subscription name from given product class
            String subName = null;
            String productClass = sub.getProductClass();

            // Check if there is a comma separated list of product classes
            if (productClass.indexOf(',') == -1) {
                subName = ChannelFamilyFactory.getNameByLabel(sub.getProductClass());
                if (subName == null || subName.isEmpty()) {
                    logger.warn("Empty name for: " + sub.getProductClass());
                    continue;
                }
            }
            else {
                if (logger.isDebugEnabled()) {
                    logger.debug("List of product classes: " + sub.getProductClass());
                }
                List<String> productClasses = Arrays.asList(productClass.split(","));
                for (String s : productClasses) {
                    String name = ChannelFamilyFactory.getNameByLabel(s);
                    if (name == null || name.isEmpty()) {
                        logger.warn("Empty name for: " + sub.getProductClass());
                        continue;
                    }

                    // This is an or relationship so for now: append with an 'or'
                    if (subName == null) {
                        subName = name;
                    }
                    else {
                        subName = subName + " or " + name;
                    }
                }
            }

            // We have a valid subscription, add it as DTO
            SubscriptionDto dto = new SubscriptionDto();
            dto.setName(subName);
            dto.setStartDate(sub.getStartDate());
            dto.setEndDate(sub.getEndDate());
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Return cached list of subscriptions or "null" for signaling "verification failed".
     * @param creds credentials
     * @param request request
     * @param forceRefresh set true to refresh the cached subscriptions
     * @return list of subscriptions or null signaling "verification failed"
     */
    @SuppressWarnings("unchecked")
    public static List<SubscriptionDto> getSubscriptions(MirrorCredentials creds,
            HttpServletRequest request, boolean forceRefresh) {
        // Implicitly download subscriptions if requested
        if (forceRefresh || verificationStatusUnknown(creds, request)) {
            List<Subscription> subscriptions = SetupWizardManager.downloadSubscriptions(creds);
            storeSubsInSession(makeDtos(subscriptions), creds, request);
        }

        // Return from cache
        List<SubscriptionDto> ret = null;
        HttpSession session = request.getSession();
        Map<String, List<SubscriptionDto>> subsMap =
                (Map<String, List<SubscriptionDto>>) session.getAttribute(SUBSCRIPTIONS_KEY);
        if (subsMap != null) {
            ret = subsMap.get(creds.getUser());
        }
        return ret;
    }

    /**
     * Check if the verification status of any given credentials is unknown.
     * @param creds credentials
     * @param request request
     * @return true if verification status is unknown for the given creds, otherwise false.
     */
    @SuppressWarnings("unchecked")
    private static boolean verificationStatusUnknown(MirrorCredentials creds,
            HttpServletRequest request) {
        boolean ret = true;
        HttpSession session = request.getSession();
        Map<String, List<Subscription>> subsMap =
                (Map<String, List<Subscription>>) session.getAttribute(SUBSCRIPTIONS_KEY);
        if (subsMap != null && subsMap.containsKey(creds.getUser())) {
            ret = false;
        }
        return ret;
    }

    /**
     * Put a list of subscriptions in the session cache, while "null" is stored whenever the
     * verification status is "failed" for a given pair of credentials.
     * @param subscriptions subscriptions
     * @param request request
     */
    @SuppressWarnings("unchecked")
    private static void storeSubsInSession(List<SubscriptionDto> subscriptions,
            MirrorCredentials creds, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, List<SubscriptionDto>> subsMap =
                (Map<String, List<SubscriptionDto>>) session.getAttribute(SUBSCRIPTIONS_KEY);

        // Create the map for caching if it doesn't exist
        if (subsMap == null) {
            subsMap = new HashMap<String, List<SubscriptionDto>>();
            session.setAttribute(SUBSCRIPTIONS_KEY, subsMap);
        }

        // Store or update the subscriptions
        logger.debug("Storing subscriptions for " + creds.getUser());
        subsMap.put(creds.getUser(), subscriptions);
    }

    /**
     * Delete cached subscriptions for a given pair of credentials.
     * @param creds credentials
     */
    @SuppressWarnings("unchecked")
    private static void removeSubsFromSession(MirrorCredentials creds,
            HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, List<SubscriptionDto>> subsMap =
                (Map<String, List<SubscriptionDto>>) session.getAttribute(SUBSCRIPTIONS_KEY);
        subsMap.remove(creds.getUser());
        if (logger.isDebugEnabled()) {
            logger.debug("removed " + creds.getUser());
        }
    }
}
