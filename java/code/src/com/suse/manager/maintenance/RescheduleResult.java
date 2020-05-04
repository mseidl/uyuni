/**
 * Copyright (c) 2020 SUSE LLC
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
package com.suse.manager.maintenance;

import com.redhat.rhn.domain.action.Action;
import com.redhat.rhn.domain.server.Server;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RescheduleResult {

    private String strategy;
    private Map<Action, List<Server>> actionsServers;
    private boolean success;

    /**
     * Constructor
     * @param rescheduleType result for type
     * @param actionsServersIn actions with the affected servers
     */
    public RescheduleResult(String rescheduleType, Map<Action, List<Server>> actionsServersIn) {
        strategy = rescheduleType;
        actionsServers = actionsServersIn;
        success = false;
    }

    /**
     * @return the strategy used for this result
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * @return a result message
     */
    public String getMessage() {
        return this.toString();
    }

    /**
     * @return get the status
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param strategyIn set strategy type
     */
    public void setStrategy(String strategyIn) {
        strategy = strategyIn;
    }

    /**
     * @param statusIn the status
     */
    public void setSuccess(boolean statusIn) {
        success = statusIn;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return actionsServers.keySet().stream()
            .map(action -> {
                String serverNames = actionsServers.get(action).stream()
                        .map(s -> s.getName()).collect(Collectors.joining(", "));
                if (success) {
                    return String.format(
                            "Action '%s' sucessfully handled using '%s' reschedule strategy " +
                            "for the following server: '%s'",
                            action.getName(),
                            strategy,
                            serverNames);
                }
                else {
                    return String.format(
                            "Unable to handle Action '%s' using '%s' reschedule strategy " +
                            "for the following server: '%s'",
                            action.getName(),
                            strategy,
                            serverNames);
                }
            }).collect(Collectors.joining("\n"));
    }
}
