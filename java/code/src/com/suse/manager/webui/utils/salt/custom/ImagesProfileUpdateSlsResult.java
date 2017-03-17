/**
 * Copyright (c) 2016 SUSE LLC
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
package com.suse.manager.webui.utils.salt.custom;

import com.google.gson.annotations.SerializedName;
import com.suse.salt.netapi.results.Ret;
import com.suse.salt.netapi.results.StateApplyResult;

/**
 * Result structure from images.profileupdate
 */
public class ImagesProfileUpdateSlsResult {

    @SerializedName("module_|-mgr_image_profileupdate_|-dockerng.sls_build_|-run")
    private StateApplyResult<Ret<PkgProfileUpdateSlsResult>> dockerngSlsBuild;

    @SerializedName("module_|-mgr_image_inspect_|-dockerng.inspect_|-run")
    private StateApplyResult<Ret<ImageInspectSlsResult>> dockerngInspect;

    /**
     * @return getter
     */
    public StateApplyResult<Ret<PkgProfileUpdateSlsResult>> getDockerngSlsBuild() {
        return dockerngSlsBuild;
    }

    /**
     * @return getter
     */
    public StateApplyResult<Ret<ImageInspectSlsResult>> getDockerngInspect() {
        return dockerngInspect;
    }
}
