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

package com.redhat.rhn.common.security.acl;

/**
 * Interface for classes that have ACL handler methods.
 * Classes implementing this interface are registered with
 * {@link Acl#registerHandler(String)}. Any method of the subclass with
 * the prefix "acl" and the following signature 
 * (static or non-static) are registered as
 * ACL handler methods:
 * <pre>
 *     public boolean aclXXXX(Object context, String parameters[])
 * </pre>
 *
 * @version $Rev$
 * @see Acl
 */
public interface AclHandler {
    // strictly a type interface, sort of like Serializable
}
